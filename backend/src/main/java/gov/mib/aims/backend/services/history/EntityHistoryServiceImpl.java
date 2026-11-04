package gov.mib.aims.backend.services.history;

import gov.mib.aims.backend.entity.EntityHistoryEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link EntityHistoryService}.
 */
@Service
@RequiredArgsConstructor
public class EntityHistoryServiceImpl implements EntityHistoryService {

    private final EntityHistoryRepository entityHistoryRepository;
    private final ObjectMapperWrapper objectMapper;
    private final CurrentUserService currentUserService;
    private final ChronoService chronoService;

    @Override
    @Transactional
    public void recordChange(EntityType entityType, Long entityId, Object newState) {
        recordChange(entityType, entityId, newState, currentUserService.getCurrentUserId());
    }

    @Override
    @Transactional
    public void recordChange(EntityType entityType, Long entityId, Object newState, Long changedByUserId) {
        validateRecordRequest(entityType, entityId, newState, changedByUserId);
        String snapshot = objectMapper.writeValueAsStringOrThrow(newState);
        EntityHistoryEntity entity = EntityHistoryEntity.builder()
                .entityType(entityType)
                .entityId(entityId)
                .snapshot(snapshot)
                .changedByUserId(changedByUserId)
                .changedAt(chronoService.now())
                .build();
        entityHistoryRepository.save(entity);
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
}
