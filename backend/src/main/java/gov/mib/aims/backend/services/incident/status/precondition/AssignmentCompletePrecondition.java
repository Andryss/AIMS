package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import org.springframework.stereotype.Component;

/**
 * Для перехода в PREPARED_FOR_EXECUTION нужны ответственный и хотя бы один исполнитель.
 */
@Component
public class AssignmentCompletePrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    @Override
    public void check(IncidentEntity context) {
        if (context.getResponsibleUserId() == null) {
            throw Errors.validationError("Responsible agent must be assigned");
        }
        if (context.getExecutorUserIds() == null || context.getExecutorUserIds().isEmpty()) {
            throw Errors.validationError("At least one executor must be assigned");
        }
    }
}
