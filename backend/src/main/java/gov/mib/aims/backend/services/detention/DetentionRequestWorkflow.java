package gov.mib.aims.backend.services.detention;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.DetentionRequestStatus;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.services.CurrentUserService;
import gov.mib.aims.backend.services.status.AbstractStatusWorkflow;
import gov.mib.aims.backend.services.status.StatusTransition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Рассмотрение заявок и переходы фактического заключения.
 */
@Component
@RequiredArgsConstructor
public class DetentionRequestWorkflow
        extends AbstractStatusWorkflow<DetentionRequestEntity, DetentionRequestStatus> {

    private final CurrentUserService currentUserService;
    private final Clock clock;

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

        var transition = StatusTransition.<DetentionRequestEntity, DetentionRequestStatus>builder()
                .from(from).to(target)
                .preconditions(List.of(request -> assertActor(request, target),
                        request -> assertComment(request, target, comment))).postActions(List.of()).build();

        return applyTransition(entity, transition);
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
                && clock.instant().isBefore(entity.getPlannedReleaseAt());
        if ((target == DetentionRequestStatus.CANCELLED || target == DetentionRequestStatus.REJECTED || earlyRelease)
                && (comment == null || comment.trim().isBlank())) {
            throw Errors.validationError("Укажите причину отмены, отклонения или досрочного освобождения");
        }
    }

    @Override
    protected DetentionRequestEntity applyStatus(DetentionRequestEntity entity, DetentionRequestStatus target) {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
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
