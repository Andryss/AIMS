package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import org.springframework.stereotype.Component;

/**
 * Проверяет заполненность обязательных полей инцидента.
 */
@Component
public class RequiredFieldsPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    @Override
    public void check(IncidentEntity context) {
        if (isBlank(context.getLocation())) {
            throw Errors.transitionPreconditionFailed("location is required");
        }
        if (isBlank(context.getDescription())) {
            throw Errors.transitionPreconditionFailed("description is required");
        }
        if (context.getDetectedAt() == null) {
            throw Errors.transitionPreconditionFailed("detectedAt is required");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
