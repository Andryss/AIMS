package gov.mib.aims.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.mib.aims.backend.entity.EntityHistoryEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.EntityHistoryRecord;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Реализация {@link EntityHistoryService}.
 */
@Service
@RequiredArgsConstructor
public class EntityHistoryServiceImpl implements EntityHistoryService {

    private final EntityHistoryRepository entityHistoryRepository;
    private final ObjectMapper objectMapper;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public void recordChange(EntityType entityType, Long entityId, Object newState) {
        recordChange(entityType, entityId, newState, currentUserService.getCurrentUserId());
    }

    @Override
    @Transactional
    public void recordChange(EntityType entityType, Long entityId, Object newState, Long changedByUserId) {
        validateRecordRequest(entityType, entityId, newState, changedByUserId);
        String snapshot = serializeSnapshot(newState);
        EntityHistoryEntity entity = EntityHistoryEntity.builder()
                .entityType(entityType)
                .entityId(entityId)
                .snapshot(snapshot)
                .changedByUserId(changedByUserId)
                .changedAt(Instant.now())
                .build();
        entityHistoryRepository.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EntityHistoryRecord> getHistory(EntityType entityType, Long entityId) {
        if (entityType == null) {
            throw Errors.validationError("entityType is required");
        }
        if (entityId == null) {
            throw Errors.validationError("entityId is required");
        }
        return entityHistoryRepository.findByEntityTypeAndEntityIdOrderByChangedAtDesc(entityType, entityId)
                .stream()
                .map(this::toRecord)
                .toList();
    }

    private void validateRecordRequest(
            EntityType entityType,
            Long entityId,
            Object newState,
            Long changedByUserId
    ) {
        if (entityType == null) {
            throw Errors.validationError("entityType is required");
        }
        if (entityId == null) {
            throw Errors.validationError("entityId is required");
        }
        if (newState == null) {
            throw Errors.validationError("newState is required");
        }
        if (changedByUserId == null) {
            throw Errors.validationError("changedByUserId is required");
        }
    }

    private String serializeSnapshot(Object newState) {
        try {
            return objectMapper.writeValueAsString(newState);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize entity snapshot", e);
        }
    }

    private EntityHistoryRecord toRecord(EntityHistoryEntity entity) {
        return new EntityHistoryRecord(
                entity.getId(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getSnapshot(),
                entity.getChangedByUserId(),
                entity.getChangedAt()
        );
    }
}
