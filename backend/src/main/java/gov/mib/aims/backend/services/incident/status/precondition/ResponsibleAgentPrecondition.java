package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Переход доступен только назначенному ответственному агенту.
 */
@Component
@RequiredArgsConstructor
public class ResponsibleAgentPrecondition implements StatusTransitionPrecondition<IncidentTransitionContext> {

    private final CurrentUserService currentUserService;

    @Override
    public void check(IncidentTransitionContext context) {
        Long responsibleUserId = context.incident().getResponsibleUserId();
        if (responsibleUserId == null) {
            throw Errors.validationError("Responsible agent must be assigned");
        }
        if (!responsibleUserId.equals(currentUserService.getCurrentUserId())) {
            throw Errors.insufficientRole();
        }
    }
}
