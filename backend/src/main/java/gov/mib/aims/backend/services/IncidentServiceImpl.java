package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.StoredFileRepository;
import gov.mib.aims.backend.services.incident.status.IncidentStatusWorkflow;
import lombok.RequiredArgsConstructor;
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
    private final StoredFileRepository storedFileRepository;
    private final CurrentUserService currentUserService;
    private final EntityHistoryService entityHistoryService;
    private final IncidentStatusWorkflow incidentStatusWorkflow;
    private final Clock clock;

    @Override
    @Transactional
    public IncidentResponse create(CreateIncidentRequest request) {
        validateCreateRequest(request);
        validateAttachmentIds(request.getAttachmentFileIds());
        LocalDateTime now = LocalDateTime.now(clock);
        IncidentEntity entity = IncidentEntity.builder()
                .status(IncidentStatus.DRAFT)
                .eventType(request.getEventType())
                .location(request.getLocation().trim())
                .detectedAt(request.getDetectedAt().toLocalDateTime())
                .description(request.getDescription().trim())
                .attachmentFileIds(request.getAttachmentFileIds())
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
        if (id == null) {
            throw Errors.validationError("id is required");
        }
        if (request == null || request.getStatus() == null) {
            throw Errors.validationError("status is required");
        }
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        IncidentStatus target = toModelStatus(request.getStatus());
        entity = incidentStatusWorkflow.changeStatus(entity, target);
        entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        if (id == null) {
            throw Errors.validationError("id is required");
        }
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
        return toResponse(entity);
    }

    private void validateCreateRequest(CreateIncidentRequest request) {
        if (request == null) {
            throw Errors.validationError("request is required");
        }
        if (request.getEventType() == null) {
            throw Errors.validationError("eventType is required");
        }
        if (IncidentEventType.fromCode(request.getEventType()).isEmpty()) {
            throw Errors.invalidEventType();
        }
        if (request.getLocation() == null || request.getLocation().isBlank()) {
            throw Errors.validationError("location is required");
        }
        if (request.getDetectedAt() == null) {
            throw Errors.validationError("detectedAt is required");
        }
        if (request.getDescription() == null || request.getDescription().isBlank()) {
            throw Errors.validationError("description is required");
        }
        if (request.getAttachmentFileIds() == null || request.getAttachmentFileIds().isEmpty()) {
            throw Errors.validationError("attachmentFileIds is required");
        }
    }

    private void validateAttachmentIds(List<Long> attachmentFileIds) {
        for (Long fileId : attachmentFileIds) {
            if (fileId == null || !storedFileRepository.existsById(fileId)) {
                throw Errors.attachmentNotFound();
            }
        }
    }

    private IncidentStatus toModelStatus(IncidentStatusApi dto) {
        return switch (dto) {
            case DRAFT -> IncidentStatus.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatus.READY_FOR_ANALYSIS;
        };
    }

    private IncidentStatusApi toApiStatus(IncidentStatus status) {
        return switch (status) {
            case DRAFT -> IncidentStatusApi.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatusApi.READY_FOR_ANALYSIS;
        };
    }

    private IncidentResponse toResponse(IncidentEntity entity) {
        return new IncidentResponse()
                .id(entity.getId())
                .status(toApiStatus(entity.getStatus()))
                .eventType(entity.getEventType())
                .location(entity.getLocation())
                .detectedAt(entity.getDetectedAt().atOffset(ZoneOffset.UTC))
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
}
