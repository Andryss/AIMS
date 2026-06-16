package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.RoleNames;
import gov.mib.aims.backend.services.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Переход доступен оперативному агенту или администратору.
 */
@Component
@RequiredArgsConstructor
public class AgentRolePrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    private final CurrentUserService currentUserService;

    @Override
    public void check(IncidentEntity context) {
        if (!currentUserService.hasAnyRole(RoleNames.AGENT, RoleNames.ADMIN)) {
            throw Errors.invalidStatusTransition();
        }
    }
}
