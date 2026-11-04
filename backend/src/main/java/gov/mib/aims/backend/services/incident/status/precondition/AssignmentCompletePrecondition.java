package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;
import org.springframework.stereotype.Component;

/**
 * Для перехода в PREPARED_FOR_EXECUTION нужны ответственный и хотя бы один исполнитель.
 */
@Component
public class AssignmentCompletePrecondition implements StatusTransitionPrecondition<IncidentTransitionContext> {

    @Override
    public void check(IncidentTransitionContext context) {
        if (context.incident().getResponsibleUserId() == null) {
            throw Errors.validationError("Responsible agent must be assigned");
        }
        if (context.incident().getExecutorUserIds() == null || context.incident().getExecutorUserIds().isEmpty()) {
            throw Errors.validationError("At least one executor must be assigned");
        }
    }
}
