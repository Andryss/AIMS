package gov.mib.aims.backend.db;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.db.util.TableSchemaQueries;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Column;
import gov.mib.aims.backend.db.util.TableSchemaQueries.ForeignKey;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.util.List;

import static gov.mib.aims.backend.db.util.RbacDbFixtures.MISSING_ID;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertPermission;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertRole;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.BIGINT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы role_permission")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RolePermissionDbTest extends BaseDbTest {

    private static final String TABLE = "role_permission";
    private static final String ROLE_ID = "role_id";
    private static final String PERMISSION_ID = "permission_id";

    private static final String TEST_ROLE = "db-test-permitted-role";
    private static final String OTHER_ROLE = "db-test-other-permitted-role";
    private static final String TEST_PERMISSION = "DB_TEST_PERMISSION";
    private static final String OTHER_PERMISSION = "DB_TEST_OTHER_PERMISSION";

    private static final String INSERT_GRANT = "INSERT INTO role_permission (role_id, permission_id) VALUES (?, ?)";

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] role_permission содержит колонки role_id и permission_id")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ROLE_ID, PERMISSION_ID);
    }

    @Test
    @DisplayName("[Схема] role_id объявлен как BIGINT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredRoleIdColumn() {
        schema.assertColumn(TABLE, Column.required(ROLE_ID, BIGINT));
    }

    @Test
    @DisplayName("[Схема] permission_id объявлен как BIGINT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredPermissionIdColumn() {
        schema.assertColumn(TABLE, Column.required(PERMISSION_ID, BIGINT));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из role_id и permission_id")
    void shouldHaveCompositePrimaryKey() {
        schema.assertPrimaryKeys(TABLE, new Key(ROLE_ID, PERMISSION_ID));
    }

    @Test
    @DisplayName("[Схема] У role_permission нет дополнительных ограничений UNIQUE")
    void shouldHaveNoAdditionalUniqueKeys() {
        schema.assertUniqueKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] role_id ссылается на role.id, permission_id — на permission.id, оба с ON DELETE CASCADE")
    void shouldHaveCascadingForeignKeysToRoleAndPermission() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey(ROLE_ID, "role", ID, "c"),
                new ForeignKey(PERMISSION_ID, "permission", ID, "c"));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет назначение существующего разрешения роли")
    void shouldPersistRolePermissionGrant() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);

        int insertedRows = jdbcTemplate.update(INSERT_GRANT, roleId, permissionId);

        assertThat(insertedRows).as("number of grants inserted").isEqualTo(1);
        assertThat(permissionsForRole(roleId)).as("granted permissions").containsExactly(permissionId);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE заменяет назначенное разрешение другим существующим разрешением")
    void shouldUpdateGrantedPermission() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);
        long otherPermissionId = insertPermission(jdbcTemplate, OTHER_PERMISSION);
        jdbcTemplate.update(INSERT_GRANT, roleId, permissionId);

        int updatedRows = jdbcTemplate.update("""
                UPDATE role_permission SET permission_id = ? WHERE role_id = ? AND permission_id = ?
                """, otherPermissionId, roleId, permissionId);

        assertThat(updatedRows).as("number of grants updated").isEqualTo(1);
        assertThat(permissionsForRole(roleId)).as("granted permissions after UPDATE")
                .containsExactly(otherPermissionId);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторной парой role_id и permission_id отклоняется")
    void shouldRejectDuplicateGrant() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);
        jdbcTemplate.update(INSERT_GRANT, roleId, permissionId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_GRANT, roleId, permissionId));

        assertAll(INSERT_GRANT,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(permissionsForRole(roleId)).as("grants after rejected INSERT")
                        .containsExactly(permissionId));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в role_id отклоняется")
    void shouldRejectNullRoleId() {
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_GRANT, null, permissionId));

        assertAll(INSERT_GRANT,
                () -> assertNotNullViolation(failure, ROLE_ID),
                () -> assertThat(rolesWithPermission(permissionId)).as("grants after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в permission_id отклоняется")
    void shouldRejectNullPermissionId() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_GRANT, roleId, null));

        assertAll(INSERT_GRANT,
                () -> assertNotNullViolation(failure, PERMISSION_ID),
                () -> assertThat(permissionsForRole(roleId)).as("grants after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующую роль отклоняется")
    void shouldRejectMissingRole() {
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_GRANT, MISSING_ID, permissionId));

        assertAll(INSERT_GRANT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(rolesWithPermission(permissionId)).as("grants after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующее разрешение отклоняется")
    void shouldRejectMissingPermission() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_GRANT, roleId, MISSING_ID));

        assertAll(INSERT_GRANT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(permissionsForRole(roleId)).as("grants after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] DELETE назначения сохраняет остальные разрешения роли")
    void shouldDeleteOnlySelectedGrant() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);
        long otherPermissionId = insertPermission(jdbcTemplate, OTHER_PERMISSION);
        jdbcTemplate.update(INSERT_GRANT, roleId, permissionId);
        jdbcTemplate.update(INSERT_GRANT, roleId, otherPermissionId);

        int deletedRows = jdbcTemplate.update("DELETE FROM role_permission WHERE role_id = ? AND permission_id = ?",
                roleId, permissionId);

        assertThat(deletedRows).as("number of grants deleted").isEqualTo(1);
        assertThat(permissionsForRole(roleId)).as("remaining granted permissions").containsExactly(otherPermissionId);
    }

    @Test
    @DisplayName("[Сценарий] Удаление роли каскадно удаляет её связи с разрешениями")
    void shouldCascadeOnRoleDelete() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long otherRoleId = insertRole(jdbcTemplate, OTHER_ROLE, null);
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);
        jdbcTemplate.update(INSERT_GRANT, roleId, permissionId);
        jdbcTemplate.update(INSERT_GRANT, otherRoleId, permissionId);

        int deletedRows = jdbcTemplate.update("DELETE FROM role WHERE id = ?", roleId);

        assertThat(deletedRows).as("number of roles deleted").isEqualTo(1);
        assertThat(rolesWithPermission(permissionId)).as("roles still granted the permission")
                .containsExactly(otherRoleId);
    }

    @Test
    @DisplayName("[Сценарий] Удаление разрешения каскадно удаляет его назначения ролям")
    void shouldCascadeOnPermissionDelete() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long permissionId = insertPermission(jdbcTemplate, TEST_PERMISSION);
        long otherPermissionId = insertPermission(jdbcTemplate, OTHER_PERMISSION);
        jdbcTemplate.update(INSERT_GRANT, roleId, permissionId);
        jdbcTemplate.update(INSERT_GRANT, roleId, otherPermissionId);

        int deletedRows = jdbcTemplate.update("DELETE FROM permission WHERE id = ?", permissionId);

        assertThat(deletedRows).as("number of permissions deleted").isEqualTo(1);
        assertThat(permissionsForRole(roleId)).as("permissions still granted to the role")
                .containsExactly(otherPermissionId);
    }

    private List<Long> permissionsForRole(long roleId) {
        return jdbcTemplate.queryForList("SELECT permission_id FROM role_permission WHERE role_id = ?", Long.class,
                roleId);
    }

    private List<Long> rolesWithPermission(long permissionId) {
        return jdbcTemplate.queryForList("SELECT role_id FROM role_permission WHERE permission_id = ?", Long.class,
                permissionId);
    }
}
