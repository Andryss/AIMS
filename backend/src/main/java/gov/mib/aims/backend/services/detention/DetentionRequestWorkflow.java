package gov.mib.aims.backend.services.detention;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.DetentionRequestStatus;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Рассмотрение заявок и переходы фактического заключения.
 */
@Component
@RequiredArgsConstructor
public class DetentionRequestWorkflow {

    private final CurrentUserService currentUserService;
    private final ChronoService chronoService;

    /**
     * Проверяет переход, полномочия и обязательную причину, затем изменяет состояние заявки.
     */
    public DetentionRequestEntity changeStatus(DetentionRequestEntity entity, DetentionRequestStatus target,
                                               String comment) {
        DetentionRequestStatus from = entity.getStatus();
        boolean allowed = (from == DetentionRequestStatus.NEW && (target == DetentionRequestStatus.ACCEPTED
                || target == DetentionRequestStatus.REJECTED || target == DetentionRequestStatus.CANCELLED))
                || (from == DetentionRequestStatus.ACCEPTED && (target == DetentionRequestStatus.CANCELLED
                || target == DetentionRequestStatus.IN_CUSTODY))
                || (from == DetentionRequestStatus.IN_CUSTODY && target == DetentionRequestStatus.RELEASED);
        if (!allowed) {
            throw Errors.validationError("Недопустимый переход статуса заявки на заключение");
        }

        assertActor(entity, target);
        assertComment(entity, target, comment);
        return applyStatus(entity, target);
    }

    private void assertActor(DetentionRequestEntity entity, DetentionRequestStatus target) {
        boolean authorCancellation = entity.getStatus() == DetentionRequestStatus.NEW
                && target == DetentionRequestStatus.CANCELLED
                && entity.getCreatedByUserId().equals(currentUserService.getCurrentUserId())
                && currentUserService.hasPermission(Permission.DETENTION_EDIT);
        if (!currentUserService.hasPermission(Permission.DETENTION_PROCESS) && !authorCancellation) {
            throw Errors.accessDenied();
        }
    }

    private void assertComment(DetentionRequestEntity entity, DetentionRequestStatus target, String comment) {
        boolean earlyRelease = target == DetentionRequestStatus.RELEASED
                && chronoService.isBefore(chronoService.now(),
                        chronoService.plus(entity.getAdmittedAt(), entity.getDurationDays(), ChronoUnit.DAYS));
        if ((target == DetentionRequestStatus.CANCELLED || target == DetentionRequestStatus.REJECTED || earlyRelease)
                && (comment == null || comment.trim().isBlank())) {
            throw Errors.validationError("Укажите причину отмены, отклонения или досрочного освобождения");
        }
    }

    private DetentionRequestEntity applyStatus(DetentionRequestEntity entity, DetentionRequestStatus target) {
        Instant now = chronoService.now();
        if (target == DetentionRequestStatus.ACCEPTED || target == DetentionRequestStatus.REJECTED) {
            entity.setReviewedByUserId(currentUserService.getCurrentUserId());
        } else if (target == DetentionRequestStatus.IN_CUSTODY) {
            entity.setAdmittedAt(now);
        } else if (target == DetentionRequestStatus.RELEASED) {
            entity.setReleasedAt(now);
        }

        entity.setStatus(target);
        entity.setUpdatedAt(now);

        return entity;
    }
}
