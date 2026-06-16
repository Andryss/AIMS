package gov.mib.aims.backend.bootstrap;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.PermissionEntity;
import gov.mib.aims.backend.entity.RoleEntity;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.model.Role;
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Инициализатор демо-данных RBAC и пользователей для локальной демонстрации.
 */
@Component
@ConditionalOnProperty(prefix = "aims.demo-data", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DemoDataInitializer implements ApplicationRunner {

    private static final Map<String, Role> DEMO_USERS = new LinkedHashMap<>();

    static {
        DEMO_USERS.put("operator", Role.OPERATOR);
        DEMO_USERS.put("analyst", Role.ANALYST);
        DEMO_USERS.put("agent", Role.AGENT);
        DEMO_USERS.put("agent2", Role.AGENT);
        DEMO_USERS.put("cleaner", Role.CLEANER);
        DEMO_USERS.put("admin", Role.ADMIN);
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
        Arrays.stream(Permission.values()).forEach(permission -> {
            PermissionEntity entity = permissionRepository.save(PermissionEntity.builder()
                    .code(permission.getCode())
                    .description(permission.getDescription())
                    .build());
            result.put(permission.getCode(), entity);
        });
        return result;
    }

    private Map<String, RoleEntity> seedRoles() {
        Map<String, RoleEntity> result = new LinkedHashMap<>();
        Arrays.stream(Role.values()).forEach(role -> {
            RoleEntity entity = roleRepository.save(RoleEntity.builder()
                    .name(role.getCode())
                    .description(role.getDescription())
                    .build());
            result.put(role.getCode(), entity);
        });
        return result;
    }

    private void linkRolePermissions(Map<String, PermissionEntity> permissionsByCode,
                                   Map<String, RoleEntity> rolesByName) {
        Arrays.stream(Role.values()).forEach(role -> {
            Long roleId = rolesByName.get(role.getCode()).getId();
            for (Permission permission : role.getPermissions()) {
                Long permissionId = permissionsByCode.get(permission.getCode()).getId();
                jdbcTemplate.update(
                        "INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)",
                        roleId,
                        permissionId
                );
            }
        });
    }

    private void seedUsers(Map<String, RoleEntity> rolesByName) {
        DEMO_USERS.forEach((login, role) -> {
            AppUserEntity user = appUserRepository.save(AppUserEntity.builder()
                    .login(login)
                    .passwordHash(passwordEncoder.encode(login))
                    .build());
            jdbcTemplate.update(
                    "INSERT INTO user_role (user_id, role_id) VALUES (?, ?)",
                    user.getId(),
                    rolesByName.get(role.getCode()).getId()
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
