package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.IncidentEventType;
import org.springframework.stereotype.Component;

/**
 * Проверяет корректность кода типа события.
 */
@Component
public class ValidEventTypePrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    @Override
    public void check(IncidentEntity context) {
        if (IncidentEventType.fromCode(context.getEventType()).isEmpty()) {
            throw Errors.invalidEventType();
        }
    }
}
