package gov.mib.aims.backend.services.equipment;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.entity.EquipmentMovementItemEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEventApi;
import gov.mib.aims.backend.generated.model.EquipmentHistorySnapshot;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.EquipmentMovementOperation;
import gov.mib.aims.backend.model.EquipmentMovementStatus;
import gov.mib.aims.backend.model.EquipmentStatus;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.EquipmentMovementItemRepository;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.history.EntityHistoryService;
import gov.mib.aims.backend.services.mapping.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * Переходы заявки и атомарная передача оборудования в транзакции сервиса.
 */
@Component
@RequiredArgsConstructor
public class EquipmentMovementWorkflow {

    private final CurrentUserService currentUserService;
    private final EquipmentMovementItemRepository itemRepository;
    private final EquipmentRepository equipmentRepository;
    private final EntityHistoryService historyService;
    private final EquipmentMapper equipmentMapper;
    private final ChronoService chronoService;

    /**
     * Проверяет переход и права, затем применяет изменение в транзакции сервиса.
     */
    public EquipmentMovementEntity changeStatus(EquipmentMovementEntity movement, EquipmentMovementStatus target,
                                                String comment) {
        EquipmentMovementStatus from = movement.getStatus();
        boolean termination = target == EquipmentMovementStatus.CANCELLED || target == EquipmentMovementStatus.REJECTED;
        boolean allowed = (from == EquipmentMovementStatus.PENDING
                && (target == EquipmentMovementStatus.PROCESSING || termination))
                || (from == EquipmentMovementStatus.PROCESSING
                && (target == EquipmentMovementStatus.COMPLETED || termination));
        if (!allowed) {
            throw Errors.validationError("Недопустимый переход статуса заявки на движение оборудования");
        }

        assertActor(movement, target);
        assertComment(target, comment);
        return applyStatus(movement, target);
    }

    private void assertActor(EquipmentMovementEntity movement, EquipmentMovementStatus target) {
        boolean storekeeper = currentUserService.hasAnyRole(Role.STOREKEEPER, Role.ADMIN);
        boolean author = movement.getCreatedByUserId().equals(currentUserService.getCurrentUserId());
        if (!storekeeper && (target != EquipmentMovementStatus.CANCELLED || !author)) {
            throw Errors.accessDenied();
        }
    }

    private void assertComment(EquipmentMovementStatus target, String comment) {
        if ((target == EquipmentMovementStatus.CANCELLED || target == EquipmentMovementStatus.REJECTED)
                && (comment == null || comment.isBlank())) {
            throw Errors.validationError("Укажите причину отмены или отклонения заявки");
        }
    }

    private EquipmentMovementEntity applyStatus(EquipmentMovementEntity movement, EquipmentMovementStatus target) {
        if (target == EquipmentMovementStatus.COMPLETED) {
            complete(movement);
        } else {
            movement.setStatus(target);
        }

        return movement;
    }

    private void complete(EquipmentMovementEntity movement) {
        List<Long> ids = itemRepository.findAllByMovementIdInOrderByIdAsc(List.of(movement.getId())).stream()
                .map(EquipmentMovementItemEntity::getEquipmentItemId).toList();
        List<EquipmentEntity> equipment = equipmentRepository.findAllById(ids);

        boolean issue = movement.getOperation() == EquipmentMovementOperation.ISSUE;

        for (EquipmentEntity item : equipment) {
            if (issue ? item.getStatus() != EquipmentStatus.AVAILABLE
                    : item.getStatus() != EquipmentStatus.ISSUED
                    || !Objects.equals(item.getHolderUserId(), movement.getResponsibleUserId())
                    || !Objects.equals(item.getCurrentIssueMovementId(), movement.getSourceIssueMovementId())) {
                throw Errors.equipmentConflict("Экземпляр №" + item.getId()
                        + " больше недоступен для этой операции. Обновите сведения об оборудовании");
            }
        }

        movement.setStatus(EquipmentMovementStatus.COMPLETED);
        movement.setCompletedAt(chronoService.now());

        for (EquipmentEntity item : equipment) {
            item.setStatus(issue ? EquipmentStatus.ISSUED : EquipmentStatus.AVAILABLE);
            item.setHolderUserId(issue ? movement.getResponsibleUserId() : null);
            item.setCurrentIssueMovementId(issue ? movement.getId() : null);

            historyService.recordChange(EntityType.EQUIPMENT, item.getId(), new EquipmentHistorySnapshot()
                    .event(issue ? EquipmentHistoryEventApi.ISSUED : EquipmentHistoryEventApi.RETURNED)
                    .movementId(movement.getId()).equipment(equipmentMapper.toResponse(item)));
        }
    }
}
