package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.StoredFileRepository;
import gov.mib.aims.backend.services.incident.ExecutorAssignmentNotifier;
import gov.mib.aims.backend.services.incident.StatusChangeCommentHolder;
import gov.mib.aims.backend.services.incident.status.IncidentStatusWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
    private final StoredFileRepository storedFileRepository;
    private final AppUserRepository appUserRepository;
    private final CurrentUserService currentUserService;
    private final EntityHistoryService entityHistoryService;
    private final IncidentStatusWorkflow incidentStatusWorkflow;
    private final IncidentCommentService incidentCommentService;
    private final ExecutorAssignmentNotifier executorAssignmentNotifier;
    private final Clock clock;

    @Override
    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        assertAttachmentsExist(request.getAttachmentFileIds());
        LocalDateTime now = LocalDateTime.now(clock);
        IncidentEntity entity = IncidentEntity.builder()
                .status(IncidentStatus.DRAFT)
                .eventType(toModelEventType(request.getEventType()))
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
        return toResponse(entity);
    }

    @Override
    @Transactional
    public IncidentResponse changeStatus(Long id, ChangeIncidentStatusRequest request) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        IncidentStatus target = toModelStatus(request.getStatus());
        String comment = request.getComment();
        try {
            StatusChangeCommentHolder.set(comment);
            entity = incidentStatusWorkflow.changeStatus(entity, target);
            if (comment != null && !comment.isBlank()) {
                incidentCommentService.createFromStatusChange(id, comment);
            }
            entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
            return toResponse(entity);
        } finally {
            StatusChangeCommentHolder.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        return toResponse(entity);
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
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentListResponse list(int page, int size) {
        Page<IncidentEntity> result = incidentRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(page, size)
        );
        return new IncidentListResponse()
                .items(result.getContent().stream().map(this::toResponse).toList())
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
        return toResponse(entity);
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
        return toResponse(entity);
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

    private void assertAttachmentsExist(List<Long> attachmentFileIds) {
        for (Long fileId : attachmentFileIds) {
            if (!storedFileRepository.existsById(fileId)) {
                throw Errors.attachmentNotFound();
            }
        }
    }

    private IncidentEventType toModelEventType(IncidentEventTypeApi dto) {
        return switch (dto) {
            case UNIDENTIFIED_SIGHTING -> IncidentEventType.UNIDENTIFIED_SIGHTING;
            case CONTACT_SUSPECT -> IncidentEventType.CONTACT_SUSPECT;
            case ILLEGAL_UFO_LANDING -> IncidentEventType.ILLEGAL_UFO_LANDING;
            case MEMORY_ANOMALY -> IncidentEventType.MEMORY_ANOMALY;
            case ALIEN_ARTIFACT -> IncidentEventType.ALIEN_ARTIFACT;
            case ALIEN_CAPTURE -> IncidentEventType.ALIEN_CAPTURE;
        };
    }

    private IncidentEventTypeApi toApiEventType(IncidentEventType eventType) {
        return switch (eventType) {
            case UNIDENTIFIED_SIGHTING -> IncidentEventTypeApi.UNIDENTIFIED_SIGHTING;
            case CONTACT_SUSPECT -> IncidentEventTypeApi.CONTACT_SUSPECT;
            case ILLEGAL_UFO_LANDING -> IncidentEventTypeApi.ILLEGAL_UFO_LANDING;
            case MEMORY_ANOMALY -> IncidentEventTypeApi.MEMORY_ANOMALY;
            case ALIEN_ARTIFACT -> IncidentEventTypeApi.ALIEN_ARTIFACT;
            case ALIEN_CAPTURE -> IncidentEventTypeApi.ALIEN_CAPTURE;
        };
    }

    private IncidentStatus toModelStatus(IncidentStatusApi dto) {
        return switch (dto) {
            case DRAFT -> IncidentStatus.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatus.READY_FOR_ANALYSIS;
            case READY_FOR_EXECUTION -> IncidentStatus.READY_FOR_EXECUTION;
            case CLARIFICATION_REQUIRED -> IncidentStatus.CLARIFICATION_REQUIRED;
            case PREPARATION_FOR_EXECUTION -> IncidentStatus.PREPARATION_FOR_EXECUTION;
            case PREPARED_FOR_EXECUTION -> IncidentStatus.PREPARED_FOR_EXECUTION;
            case EXECUTING -> IncidentStatus.EXECUTING;
            case EXECUTION_COMPLETED -> IncidentStatus.EXECUTION_COMPLETED;
            case REANALYSIS_REQUIRED -> IncidentStatus.REANALYSIS_REQUIRED;
        };
    }

    private IncidentStatusApi toApiStatus(IncidentStatus status) {
        return switch (status) {
            case DRAFT -> IncidentStatusApi.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatusApi.READY_FOR_ANALYSIS;
            case READY_FOR_EXECUTION -> IncidentStatusApi.READY_FOR_EXECUTION;
            case CLARIFICATION_REQUIRED -> IncidentStatusApi.CLARIFICATION_REQUIRED;
            case PREPARATION_FOR_EXECUTION -> IncidentStatusApi.PREPARATION_FOR_EXECUTION;
            case PREPARED_FOR_EXECUTION -> IncidentStatusApi.PREPARED_FOR_EXECUTION;
            case EXECUTING -> IncidentStatusApi.EXECUTING;
            case EXECUTION_COMPLETED -> IncidentStatusApi.EXECUTION_COMPLETED;
            case REANALYSIS_REQUIRED -> IncidentStatusApi.REANALYSIS_REQUIRED;
        };
    }

    private IncidentResponse toResponse(IncidentEntity entity) {
        List<Long> executorIds = entity.getExecutorUserIds() != null
                ? new ArrayList<>(entity.getExecutorUserIds())
                : new ArrayList<>();
        return new IncidentResponse()
                .id(entity.getId())
                .status(toApiStatus(entity.getStatus()))
                .eventType(toApiEventType(entity.getEventType()))
                .location(entity.getLocation())
                .detectedAt(entity.getDetectedAt().atOffset(ZoneOffset.UTC))
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .alienId(entity.getAlienId())
                .responsibleUserId(entity.getResponsibleUserId())
                .executorUserIds(executorIds)
                .cleanupStatus(toApiCleanupStatus(entity.getCleanupStatus()))
                .cleanupReportId(entity.getCleanupReportId());
    }

    private CleanupStatusApi toApiCleanupStatus(CleanupStatus status) {
        if (status == null) {
            return null;
        }
        return switch (status) {
            case PREPARATION -> CleanupStatusApi.PREPARATION;
            case EXECUTION -> CleanupStatusApi.EXECUTION;
            case COMPLETED -> CleanupStatusApi.COMPLETED;
        };
    }
}
