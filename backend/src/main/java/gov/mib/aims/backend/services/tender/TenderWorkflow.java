package gov.mib.aims.backend.services.tender;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.model.TenderStatus;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Переходы тендера и выбор победителя в транзакции сервиса.
 */
@Component
@RequiredArgsConstructor
public class TenderWorkflow {

    private final CurrentUserService currentUserService;
    private final ChronoService chronoService;

    /**
     * Проверяет переход, полномочия и обязательную причину отмены.
     */
    public TenderEntity changeStatus(TenderEntity entity, TenderStatus target, String comment) {
        TenderStatus from = entity.getStatus();
        boolean cancellation = target == TenderStatus.CANCELLED;
        boolean allowed = (from == TenderStatus.NEED && target == TenderStatus.COLLECTING_OFFERS)
                || (cancellation && (from == TenderStatus.NEED || from == TenderStatus.COLLECTING_OFFERS
                || from == TenderStatus.AWAITING_DELIVERY));
        if (!allowed) {
            throw Errors.validationError("Недопустимый переход статуса тендера");
        }

        assertActor(entity, cancellation);
        if (cancellation && (comment == null || comment.trim().isBlank())) {
            throw Errors.validationError("Укажите причину отмены тендера");
        }
        return applyStatus(entity, target);
    }

    /**
     * Переводит тендер в ожидание поставки при выборе участника сервисом.
     */
    public TenderEntity selectWinner(TenderEntity entity) {
        if (entity.getStatus() != TenderStatus.COLLECTING_OFFERS) {
            throw Errors.tenderConflict("Выбрать победителя можно только во время сбора предложений");
        }

        assertActor(entity, false);
        return applyStatus(entity, TenderStatus.AWAITING_DELIVERY);
    }

    /**
     * Завершает тендер при полной приёмке поставки в транзакции сервиса.
     */
    public TenderEntity receive(TenderEntity entity) {
        if (entity.getStatus() != TenderStatus.AWAITING_DELIVERY) {
            throw Errors.tenderConflict("Приёмка доступна только в ожидании поставки");
        }

        if (!currentUserService.hasPermission(Permission.TENDER_RECEIVE)) {
            throw Errors.accessDenied();
        }
        return applyStatus(entity, TenderStatus.COMPLETED);
    }

    private void assertActor(TenderEntity entity, boolean cancellation) {
        boolean manager = currentUserService.hasPermission(Permission.TENDER_MANAGE);
        boolean ownNeed = entity.getStatus() == TenderStatus.NEED
                && entity.getCreatedByUserId().equals(currentUserService.getCurrentUserId());
        if (!manager && (!cancellation || !ownNeed)) {
            throw Errors.accessDenied();
        }
    }

    private TenderEntity applyStatus(TenderEntity entity, TenderStatus target) {
        entity.setStatus(target);
        entity.setUpdatedAt(chronoService.now());
        if (target == TenderStatus.COMPLETED) {
            entity.setCompletedAt(entity.getUpdatedAt());
        }

        return entity;
    }
}
