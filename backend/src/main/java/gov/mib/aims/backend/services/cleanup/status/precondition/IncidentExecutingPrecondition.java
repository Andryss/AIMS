package gov.mib.aims.backend.services.cleanup.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.incident.status.precondition.StatusTransitionPrecondition;
import org.springframework.stereotype.Component;

/**
 * Очистка доступна только когда инцидент выполняется или выполнение завершено.
 */
@Component
public class IncidentExecutingPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    @Override
    public void check(IncidentEntity context) {
        IncidentStatus status = context.getStatus();
        if (status != IncidentStatus.EXECUTING && status != IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.cleanupNotAllowed();
        }
    }
}
