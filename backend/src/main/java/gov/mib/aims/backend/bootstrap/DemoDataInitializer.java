package gov.mib.aims.backend.bootstrap;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.PermissionEntity;
import gov.mib.aims.backend.entity.RoleEntity;
import gov.mib.aims.backend.model.RoleNames;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.PermissionRepository;
import gov.mib.aims.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Инициализатор демо-данных RBAC и пользователей для локальной демонстрации.
 */
@Component
@ConditionalOnProperty(prefix = "aims.demo-data", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    private static final Map<String, String> PERMISSIONS = Map.of(
            "INCIDENT_READ", "Read incidents",
            "INCIDENT_CREATE", "Create incidents",
            "INCIDENT_STATUS_CHANGE", "Change incident status",
            "INCIDENT_COMMENT", "Add comments to incidents",
            "INCIDENT_ALIEN_LINK", "Link alien type to incident",
            "ALIEN_READ", "Read and search aliens in knowledge base"
    );

    private static final Map<String, List<String>> ROLE_PERMISSIONS = Map.of(
            RoleNames.OPERATOR, List.of(
                    "INCIDENT_READ", "INCIDENT_CREATE", "INCIDENT_STATUS_CHANGE", "INCIDENT_COMMENT"
            ),
            RoleNames.ANALYST, List.of(
                    "INCIDENT_READ", "INCIDENT_STATUS_CHANGE", "INCIDENT_COMMENT", "INCIDENT_ALIEN_LINK", "ALIEN_READ"
            ),
            RoleNames.AGENT, List.of("INCIDENT_READ"),
            RoleNames.ADMIN, List.of(
                    "INCIDENT_READ", "INCIDENT_CREATE", "INCIDENT_STATUS_CHANGE", "INCIDENT_COMMENT",
                    "INCIDENT_ALIEN_LINK", "ALIEN_READ"
            )
    );

    private static final Map<String, String> ROLE_DESCRIPTIONS = Map.of(
            RoleNames.OPERATOR, "Incident operator",
            RoleNames.ANALYST, "Incident analyst",
            RoleNames.AGENT, "Field agent",
            RoleNames.ADMIN, "Administrator"
    );

    private static final Map<String, String> DEMO_USERS = new LinkedHashMap<>();

    static {
        DEMO_USERS.put("operator", RoleNames.OPERATOR);
        DEMO_USERS.put("analyst", RoleNames.ANALYST);
        DEMO_USERS.put("agent", RoleNames.AGENT);
        DEMO_USERS.put("admin", RoleNames.ADMIN);
    }

    private final AppUserRepository appUserRepository;
    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (appUserRepository.count() > 0) {
            log.info("Demo data already present, skipping initialization");
            return;
        }

        Map<String, PermissionEntity> permissionsByCode = seedPermissions();
        Map<String, RoleEntity> rolesByName = seedRoles();
        linkRolePermissions(permissionsByCode, rolesByName);
        seedUsers(rolesByName);
        seedAliens();

        log.info("Demo data initialized: users {}", DEMO_USERS.keySet());
    }

    private Map<String, PermissionEntity> seedPermissions() {
        Map<String, PermissionEntity> result = new LinkedHashMap<>();
        PERMISSIONS.forEach((code, description) -> {
            PermissionEntity entity = permissionRepository.save(PermissionEntity.builder()
                    .code(code)
                    .description(description)
                    .build());
            result.put(code, entity);
        });
        return result;
    }

    private Map<String, RoleEntity> seedRoles() {
        Map<String, RoleEntity> result = new LinkedHashMap<>();
        ROLE_DESCRIPTIONS.forEach((name, description) -> {
            RoleEntity entity = roleRepository.save(RoleEntity.builder()
                    .name(name)
                    .description(description)
                    .build());
            result.put(name, entity);
        });
        return result;
    }

    private void linkRolePermissions(Map<String, PermissionEntity> permissionsByCode,
                                   Map<String, RoleEntity> rolesByName) {
        ROLE_PERMISSIONS.forEach((roleName, permissionCodes) -> {
            Long roleId = rolesByName.get(roleName).getId();
            for (String permissionCode : permissionCodes) {
                Long permissionId = permissionsByCode.get(permissionCode).getId();
                jdbcTemplate.update(
                        "INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)",
                        roleId,
                        permissionId
                );
            }
        });
    }

    private void seedUsers(Map<String, RoleEntity> rolesByName) {
        DEMO_USERS.forEach((login, roleName) -> {
            AppUserEntity user = appUserRepository.save(AppUserEntity.builder()
                    .login(login)
                    .passwordHash(passwordEncoder.encode(login))
                    .build());
            jdbcTemplate.update(
                    "INSERT INTO user_role (user_id, role_id) VALUES (?, ?)",
                    user.getId(),
                    rolesByName.get(roleName).getId()
            );
        });
    }

    private void seedAliens() {
        jdbcTemplate.update(
                """
                INSERT INTO alien (name, description, threat_level, created_at) VALUES
                ('Слизень', 'Небольшое слизистое существо', 3, CURRENT_TIMESTAMP),
                ('Слизистый червь', 'Крупный червеобразный инопланетянин', 6, CURRENT_TIMESTAMP)
                """
        );
    }
}
