package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.generated.model.UpdateIncidentRequest;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.MonitoringAlertStatus;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.MonitoringAlertRepository;
import gov.mib.aims.backend.services.incident.ExecutorAssignmentNotifier;
import gov.mib.aims.backend.services.incident.IncidentHistoryService;
import gov.mib.aims.backend.services.incident.status.IncidentStatusWorkflow;
import gov.mib.aims.backend.services.mapping.IncidentInputMapper;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import gov.mib.aims.backend.services.transaction.RetryableSerializable;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link IncidentService}.
 */
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private static final int MAX_ATTACHMENT_COUNT = 10;

    private final IncidentRepository incidentRepository;
    private final MonitoringAlertRepository monitoringAlertRepository;
    private final AlienRepository alienRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final IncidentHistoryService incidentHistoryService;
    private final IncidentInputMapper incidentInputMapper;
    private final IncidentStatusWorkflow incidentStatusWorkflow;
    private final IncidentCommentService incidentCommentService;
    private final ExecutorAssignmentNotifier executorAssignmentNotifier;
    private final IncidentMapper incidentMapper;
    private final AttachmentValidator attachmentValidator;
    private final ChronoService chronoService;

    @Override
    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        var details = incidentInputMapper.from(request);
        attachmentValidator.assertAllExist(request.getAttachmentFileIds());
        MonitoringAlertEntity linkedAlert = resolveMonitoringAlertForCreate(request.getMonitoringAlertId());
        LocalDateTime now = chronoService.localDateTimeNow();
        IncidentEntity entity = IncidentEntity.builder()
                .status(IncidentStatus.DRAFT)
                .attachmentFileIds(new ArrayList<>(request.getAttachmentFileIds()))
                .executorUserIds(new ArrayList<>())
                .createdByUserId(currentUserService.getCurrentUserId())
                .monitoringAlertId(linkedAlert != null ? linkedAlert.getId() : null)
                .createdAt(now)
                .updatedAt(now)
                .build();
        details.applyTo(entity);
        entity = incidentRepository.save(entity);
        if (linkedAlert != null) {
            linkedAlert.setStatus(MonitoringAlertStatus.INCIDENT_CREATED);
            linkedAlert.setIncidentId(entity.getId());
            monitoringAlertRepository.save(linkedAlert);
        }

        incidentHistoryService.record(entity);

        return incidentMapper.toResponse(entity);
    }

    @Override
    @RetryableSerializable
    public IncidentResponse update(Long id, UpdateIncidentRequest request) {
        IncidentEntity entity = incidentRepository.findById(id).orElseThrow(Errors::incidentNotFound);
        if (entity.getStatus() != IncidentStatus.DRAFT && entity.getStatus() != IncidentStatus.CLARIFICATION_REQUIRED) {
            throw Errors.incidentConflict("Изменение доступно только в черновике или при уточнении инцидента");
        }

        var attachments = attachmentValidator.merge(entity.getAttachmentFileIds(),
                request.getAdditionalAttachmentFileIds(), MAX_ATTACHMENT_COUNT);
        var details = incidentInputMapper.from(request);
        boolean attachmentsAdded = !entity.getAttachmentFileIds().containsAll(attachments);
        if (details.matches(entity) && !attachmentsAdded) {
            return incidentMapper.toResponse(entity);
        }

        details.applyTo(entity);
        entity.setAttachmentFileIds(attachments);
        entity.setUpdatedAt(chronoService.localDateTimeNow());
        entity = incidentRepository.save(entity);
        incidentHistoryService.record(entity);

        return incidentMapper.toResponse(entity);
    }

    private MonitoringAlertEntity resolveMonitoringAlertForCreate(Long monitoringAlertId) {
        if (monitoringAlertId == null) {
            return null;
        }

        MonitoringAlertEntity alert = monitoringAlertRepository.findById(monitoringAlertId)
                .orElseThrow(Errors::monitoringAlertNotFound);
        if (alert.getStatus() != MonitoringAlertStatus.NEW || alert.getIncidentId() != null) {
            throw Errors.monitoringAlertNotLinkable();
        }

        return alert;
    }

    @Override
    @RetryableSerializable
    public IncidentResponse changeStatus(Long id, ChangeIncidentStatusRequest request) {
        IncidentEntity entity = incidentRepository.findById(id).orElseThrow(Errors::incidentNotFound);
        IncidentStatus previous = entity.getStatus();
        IncidentStatus target = incidentMapper.toModelStatus(request.getStatus());
        String comment = request.getComment();
        entity = incidentStatusWorkflow.changeStatus(entity, target, comment);
        entity = incidentRepository.save(entity);
        if (comment != null && !comment.isBlank()) {
            incidentCommentService.createFromStatusChange(id, comment);
        }
        incidentHistoryService.record(entity);
        incidentStatusWorkflow.afterSave(entity, previous, comment);

        return incidentMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);

        return incidentMapper.toResponse(entity);
    }

    @Override
    @RetryableSerializable
    public IncidentResponse linkAlien(Long id, LinkIncidentAlienRequest request) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        if (entity.getStatus() != IncidentStatus.READY_FOR_ANALYSIS) {
            throw Errors.invalidAlienLink();
        }

        Long alienId = request.getAlienId();
        if (!alienRepository.existsById(alienId)) {
            throw Errors.alienNotFound();
        }

        entity.setAlienId(alienId);
        entity.setUpdatedAt(chronoService.localDateTimeNow());
        entity = incidentRepository.save(entity);

        incidentHistoryService.record(entity);

        return incidentMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentListResponse list(int page, int size) {
        Page<IncidentEntity> result = incidentRepository.findAllByOrderByCreatedAtDesc(
                PageParameters.of(page, size).toPageable()
        );

        return incidentMapper.toListResponse(result);
    }

    @Override
    @RetryableSerializable
    public IncidentResponse setResponsible(Long id, SetIncidentResponsibleRequest request) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        assertAssignmentAllowed(entity);
        Long userId = request.getUserId();
        if (userId != null) {
            assertAgentUser(userId);
        }

        entity.setResponsibleUserId(userId);
        entity.setUpdatedAt(chronoService.localDateTimeNow());
        entity = incidentRepository.save(entity);

        incidentHistoryService.record(entity);

        return incidentMapper.toResponse(entity);
    }

    @Override
    @RetryableSerializable
    public IncidentResponse setExecutors(Long id, SetIncidentExecutorsRequest request) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        assertAssignmentAllowed(entity);
        List<Long> userIds = request.getUserIds() != null ? request.getUserIds() : List.of();

        for (Long userId : userIds) {
            assertAgentUser(userId);
        }

        List<Long> previous = entity.getExecutorUserIds() != null
                ? new ArrayList<>(entity.getExecutorUserIds())
                : new ArrayList<>();
        entity.setExecutorUserIds(new ArrayList<>(userIds));
        entity.setUpdatedAt(chronoService.localDateTimeNow());
        entity = incidentRepository.save(entity);

        incidentHistoryService.record(entity);
        List<Long> newExecutors = userIds.stream()
                .filter(userId -> !previous.contains(userId))
                .toList();
        executorAssignmentNotifier.notifyNewExecutors(entity.getId(), newExecutors);

        return incidentMapper.toResponse(entity);
    }

    private void assertAssignmentAllowed(IncidentEntity entity) {
        IncidentStatus status = entity.getStatus();
        if (status != IncidentStatus.READY_FOR_EXECUTION
                && status != IncidentStatus.PREPARATION_FOR_EXECUTION) {
            throw Errors.invalidAssignment();
        }
    }

    private void assertAgentUser(Long userId) {
        if (!appUserRepository.existsById(userId)) {
            throw Errors.userNotFound();
        }

        if (!appUserRepository.hasRole(userId, Role.AGENT.getCode())) {
            throw Errors.userNotAgent();
        }
    }
}
