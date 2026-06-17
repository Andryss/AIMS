package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.services.CurrentUserService;
import gov.mib.aims.backend.services.incident.status.precondition.RolePrecondition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Бины предусловий ролей для графа переходов инцидента.
 */
@Configuration
public class IncidentStatusPreconditionConfig {

    @Bean
    RolePrecondition operatorRolePrecondition(CurrentUserService currentUserService) {
        return new RolePrecondition(currentUserService, Role.OPERATOR, Role.ADMIN);
    }

    @Bean
    RolePrecondition analystRolePrecondition(CurrentUserService currentUserService) {
        return new RolePrecondition(currentUserService, Role.ANALYST, Role.ADMIN);
    }

    @Bean
    RolePrecondition agentRolePrecondition(CurrentUserService currentUserService) {
        return new RolePrecondition(currentUserService, Role.AGENT, Role.ADMIN);
    }
}
