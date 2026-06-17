package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.incident.ExecutorAssignmentNotifier;
import gov.mib.aims.backend.services.incident.StatusChangeCommentHolder;
import gov.mib.aims.backend.services.incident.status.IncidentStatusWorkflow;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link IncidentService}.
 */
@Service
@RequiredArgsConstructor
public class IncidentServiceImpl implements IncidentService {

    private final IncidentRepository incidentRepository;
    private final AlienRepository alienRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final EntityHistoryService entityHistoryService;
    private final IncidentStatusWorkflow incidentStatusWorkflow;
    private final IncidentCommentService incidentCommentService;
    private final ExecutorAssignmentNotifier executorAssignmentNotifier;
    private final IncidentMapper incidentMapper;
    private final AttachmentValidator attachmentValidator;
    private final Clock clock;

    @Override
    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        attachmentValidator.assertAllExist(request.getAttachmentFileIds());
        LocalDateTime now = LocalDateTime.now(clock);
        IncidentEntity entity = IncidentEntity.builder()
                .status(IncidentStatus.DRAFT)
                .eventType(incidentMapper.toModelEventType(request.getEventType()))
                .location(request.getLocation().trim())
                .detectedAt(request.getDetectedAt().toLocalDateTime())
                .description(request.getDescription().trim())
                .attachmentFileIds(request.getAttachmentFileIds())
                .executorUserIds(new ArrayList<>())
                .createdByUserId(currentUserService.getCurrentUserId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        entity = incidentRepository.save(entity);
        entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
        return incidentMapper.toResponse(entity);
    }

    @Override
    @Transactional
    public IncidentResponse changeStatus(Long id, ChangeIncidentStatusRequest request) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        IncidentStatus target = incidentMapper.toModelStatus(request.getStatus());
        String comment = request.getComment();
        try {
            StatusChangeCommentHolder.set(comment);
            entity = incidentStatusWorkflow.changeStatus(entity, target);
            if (comment != null && !comment.isBlank()) {
                incidentCommentService.createFromStatusChange(id, comment);
            }
            entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
            return incidentMapper.toResponse(entity);
        } finally {
            StatusChangeCommentHolder.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        return incidentMapper.toResponse(entity);
    }

    @Override
    @Transactional
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
        entity.setUpdatedAt(LocalDateTime.now(clock));
        entity = incidentRepository.save(entity);
        entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
        return incidentMapper.toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentListResponse list(int page, int size) {
        Page<IncidentEntity> result = incidentRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );
        return new IncidentListResponse()
                .items(result.getContent().stream().map(incidentMapper::toResponse).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    @Override
    @Transactional
    public IncidentResponse setResponsible(Long id, SetIncidentResponsibleRequest request) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        assertAssignmentAllowed(entity);
        Long userId = request.getUserId();
        if (userId != null) {
            assertAgentUser(userId);
        }
        entity.setResponsibleUserId(userId);
        entity.setUpdatedAt(LocalDateTime.now(clock));
        entity = incidentRepository.save(entity);
        entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
        return incidentMapper.toResponse(entity);
    }

    @Override
    @Transactional
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
        entity.setUpdatedAt(LocalDateTime.now(clock));
        entity = incidentRepository.save(entity);
        entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
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
