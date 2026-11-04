package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Проверяет, что у текущего пользователя есть одна из разрешённых ролей.
 */
public class RolePrecondition implements StatusTransitionPrecondition<IncidentTransitionContext> {

    private final CurrentUserService currentUserService;
    private final Set<Role> allowedRoles;

    public RolePrecondition(CurrentUserService currentUserService, Role... allowedRoles) {
        this.currentUserService = currentUserService;
        this.allowedRoles = Arrays.stream(allowedRoles).collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public void check(IncidentTransitionContext context) {
        if (!currentUserService.hasAnyRole(allowedRoles.toArray(Role[]::new))) {
            throw Errors.insufficientRole();
        }
    }
}
