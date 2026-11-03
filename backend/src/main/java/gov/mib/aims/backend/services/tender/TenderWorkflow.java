package gov.mib.aims.backend.services.tender;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.model.TenderStatus;
import gov.mib.aims.backend.services.CurrentUserService;
import gov.mib.aims.backend.services.status.AbstractStatusWorkflow;
import gov.mib.aims.backend.services.status.StatusTransition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.List;

/**
 * Переходы тендера и выбор победителя в транзакции сервиса.
 */
@Component
@RequiredArgsConstructor
public class TenderWorkflow extends AbstractStatusWorkflow<TenderEntity, TenderStatus> {

    private final CurrentUserService currentUserService;
    private final Clock clock;

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

        var transition = StatusTransition.<TenderEntity, TenderStatus>builder().from(from).to(target)
                .preconditions(List.of(tender -> assertActor(tender, cancellation), tender -> {
                    if (cancellation && (comment == null || comment.trim().isBlank())) {
                        throw Errors.validationError("Укажите причину отмены тендера");
                    }
                })).postActions(List.of()).build();

        return applyTransition(entity, transition);
    }

    /**
     * Переводит тендер в ожидание поставки при выборе участника сервисом.
     */
    public TenderEntity selectWinner(TenderEntity entity) {
        if (entity.getStatus() != TenderStatus.COLLECTING_OFFERS) {
            throw Errors.tenderConflict("Выбрать победителя можно только во время сбора предложений");
        }

        var transition = StatusTransition.<TenderEntity, TenderStatus>builder()
                .from(entity.getStatus()).to(TenderStatus.AWAITING_DELIVERY)
                .preconditions(List.of(tender -> assertActor(tender, false))).postActions(List.of()).build();

        return applyTransition(entity, transition);
    }

    private void assertActor(TenderEntity entity, boolean cancellation) {
        boolean manager = currentUserService.hasPermission(Permission.TENDER_MANAGE);
        boolean ownNeed = entity.getStatus() == TenderStatus.NEED
                && entity.getCreatedByUserId().equals(currentUserService.getCurrentUserId());
        if (!manager && (!cancellation || !ownNeed)) {
            throw Errors.accessDenied();
        }
    }

    @Override
    protected TenderEntity applyStatus(TenderEntity entity, TenderStatus target) {
        entity.setStatus(target);
        entity.setUpdatedAt(clock.instant());

        return entity;
    }
}
