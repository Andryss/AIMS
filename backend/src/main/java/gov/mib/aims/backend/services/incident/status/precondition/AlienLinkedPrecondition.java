package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import org.springframework.stereotype.Component;

/**
 * Проверяет, что к инциденту привязан тип инопланетянина.
 */
@Component
public class AlienLinkedPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    @Override
    public void check(IncidentEntity context) {
        if (context.getAlienId() == null) {
            throw Errors.invalidStatusTransition();
        }
    }
}
