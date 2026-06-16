package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Переход доступен только назначенному ответственному агенту.
 */
@Component
@RequiredArgsConstructor
public class ResponsibleAgentPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    private final CurrentUserService currentUserService;

    @Override
    public void check(IncidentEntity context) {
        Long responsibleUserId = context.getResponsibleUserId();
        if (responsibleUserId == null) {
            throw Errors.validationError("Responsible agent must be assigned");
        }
        if (!responsibleUserId.equals(currentUserService.getCurrentUserId())) {
            throw Errors.invalidStatusTransition();
        }
    }
}
