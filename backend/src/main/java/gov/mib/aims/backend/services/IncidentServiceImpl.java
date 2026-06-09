package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.StoredFileRepository;
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
    private final StoredFileRepository storedFileRepository;
    private final CurrentUserService currentUserService;
    private final EntityHistoryService entityHistoryService;
    private final IncidentStatusWorkflow incidentStatusWorkflow;
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
        entity = incidentStatusWorkflow.changeStatus(entity, target);
        entityHistoryService.recordChange(EntityType.INCIDENT, entity.getId(), entity);
        return toResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentResponse getById(Long id) {
        IncidentEntity entity = incidentRepository.findById(id)
                .orElseThrow(Errors::incidentNotFound);
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
                .eventType(toApiEventType(entity.getEventType()))
                .location(entity.getLocation())
                .detectedAt(entity.getDetectedAt().atOffset(ZoneOffset.UTC))
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
}
