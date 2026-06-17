package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.EntityHistoryEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.IncidentHistoryEntry;
import gov.mib.aims.backend.generated.model.IncidentHistoryListResponse;
import gov.mib.aims.backend.generated.model.IncidentHistorySnapshot;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.mapping.IncidentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link EntityHistoryQueryService}.
 */
@Service
@RequiredArgsConstructor
public class EntityHistoryQueryServiceImpl implements EntityHistoryQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EntityHistoryRepository entityHistoryRepository;
    private final IncidentRepository incidentRepository;
    private final ObjectMapperWrapper objectMapper;
    private final IncidentMapper incidentMapper;

    @Override
    @Transactional(readOnly = true)
    public IncidentHistoryListResponse listIncidentHistory(Long incidentId, int page, int size) {
        if (!incidentRepository.existsById(incidentId)) {
            throw Errors.incidentNotFound();
        }
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<EntityHistoryEntity> result = entityHistoryRepository.findByEntityTypeAndEntityIdOrderByChangedAtAsc(
                EntityType.INCIDENT,
                incidentId,
                PageRequest.of(page, pageSize)
        );
        return new IncidentHistoryListResponse()
                .items(result.getContent().stream()
                        .map(this::toEntry)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    private IncidentHistoryEntry toEntry(EntityHistoryEntity entity) {
        IncidentEntity snapshotEntity = objectMapper.readValue(entity.getSnapshot(), IncidentEntity.class);
        return new IncidentHistoryEntry()
                .id(entity.getId())
                .changedAt(entity.getChangedAt().atOffset(ZoneOffset.UTC))
                .changedByUserId(entity.getChangedByUserId())
                .snapshot(toSnapshot(snapshotEntity));
    }

    private IncidentHistorySnapshot toSnapshot(IncidentEntity entity) {
        List<Long> executorIds = entity.getExecutorUserIds() != null
                ? new ArrayList<>(entity.getExecutorUserIds())
                : List.of();
        return new IncidentHistorySnapshot()
                .status(incidentMapper.toApiStatus(entity.getStatus()))
                .eventType(incidentMapper.toApiEventType(entity.getEventType()))
                .location(entity.getLocation())
                .detectedAt(entity.getDetectedAt().atOffset(ZoneOffset.UTC))
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .alienId(entity.getAlienId())
                .responsibleUserId(entity.getResponsibleUserId())
                .executorUserIds(executorIds)
                .cleanupStatus(incidentMapper.toApiCleanupStatus(entity.getCleanupStatus()))
                .cleanupReportId(entity.getCleanupReportId());
    }
}
