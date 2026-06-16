package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.EntityHistoryEntity;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentHistoryEntry;
import gov.mib.aims.backend.generated.model.IncidentHistoryListResponse;
import gov.mib.aims.backend.generated.model.IncidentHistorySnapshot;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Реализация {@link EntityHistoryQueryService}.
 */
@Service
@RequiredArgsConstructor
public class EntityHistoryQueryServiceImpl implements EntityHistoryQueryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EntityHistoryRepository entityHistoryRepository;
    private final IncidentRepository incidentRepository;
    private final AppUserRepository appUserRepository;
    private final ObjectMapperWrapper objectMapper;

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
        Map<Long, String> loginsByUserId = resolveLogins(result.getContent().stream()
                .map(EntityHistoryEntity::getChangedByUserId)
                .collect(Collectors.toSet()));
        return new IncidentHistoryListResponse()
                .items(result.getContent().stream()
                        .map(entity -> toEntry(entity, loginsByUserId))
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    private Map<Long, String> resolveLogins(Set<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> logins = new HashMap<>();
        for (AppUserEntity user : appUserRepository.findAllById(userIds)) {
            logins.put(user.getId(), user.getLogin());
        }
        return logins;
    }

    private IncidentHistoryEntry toEntry(EntityHistoryEntity entity, Map<Long, String> loginsByUserId) {
        IncidentEntity snapshotEntity = objectMapper.readValue(entity.getSnapshot(), IncidentEntity.class);
        return new IncidentHistoryEntry()
                .id(entity.getId())
                .changedAt(entity.getChangedAt().atOffset(ZoneOffset.UTC))
                .changedByLogin(loginsByUserId.getOrDefault(entity.getChangedByUserId(), "unknown"))
                .snapshot(toSnapshot(snapshotEntity));
    }

    private IncidentHistorySnapshot toSnapshot(IncidentEntity entity) {
        return new IncidentHistorySnapshot()
                .status(toApiStatus(entity.getStatus()))
                .eventType(toApiEventType(entity.getEventType()))
                .location(entity.getLocation())
                .detectedAt(entity.getDetectedAt().atOffset(ZoneOffset.UTC))
                .description(entity.getDescription())
                .attachmentFileIds(new ArrayList<>(entity.getAttachmentFileIds()))
                .alienId(entity.getAlienId());
    }

    private IncidentStatusApi toApiStatus(IncidentStatus status) {
        return switch (status) {
            case DRAFT -> IncidentStatusApi.DRAFT;
            case READY_FOR_ANALYSIS -> IncidentStatusApi.READY_FOR_ANALYSIS;
            case READY_FOR_EXECUTION -> IncidentStatusApi.READY_FOR_EXECUTION;
            case CLARIFICATION_REQUIRED -> IncidentStatusApi.CLARIFICATION_REQUIRED;
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
}
