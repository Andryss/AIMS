package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;
import org.springframework.stereotype.Component;

/**
 * Проверяет, что к инциденту привязан тип инопланетянина.
 */
@Component
public class AlienLinkedPrecondition implements StatusTransitionPrecondition<IncidentTransitionContext> {

    @Override
    public void check(IncidentTransitionContext context) {
        if (context.incident().getAlienId() == null) {
            throw Errors.invalidStatusTransition();
        }
    }
}
