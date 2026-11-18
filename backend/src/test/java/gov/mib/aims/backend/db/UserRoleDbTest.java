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
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertRole;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertUser;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.BIGINT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы user_role")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class UserRoleDbTest extends BaseDbTest {

    private static final String TABLE = "user_role";
    private static final String USER_ID = "user_id";
    private static final String ROLE_ID = "role_id";

    private static final String TEST_LOGIN = "db-test-assigned-user";
    private static final String OTHER_LOGIN = "db-test-other-assigned-user";
    private static final String TEST_ROLE = "db-test-assigned-role";
    private static final String OTHER_ROLE = "db-test-other-assigned-role";

    private static final String INSERT_ASSIGNMENT = "INSERT INTO user_role (user_id, role_id) VALUES (?, ?)";

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] user_role содержит колонки user_id и role_id")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, USER_ID, ROLE_ID);
    }

    @Test
    @DisplayName("[Схема] user_id объявлен как BIGINT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredUserIdColumn() {
        schema.assertColumn(TABLE, Column.required(USER_ID, BIGINT));
    }

    @Test
    @DisplayName("[Схема] role_id объявлен как BIGINT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredRoleIdColumn() {
        schema.assertColumn(TABLE, Column.required(ROLE_ID, BIGINT));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из user_id и role_id")
    void shouldHaveCompositePrimaryKey() {
        schema.assertPrimaryKeys(TABLE, new Key(USER_ID, ROLE_ID));
    }

    @Test
    @DisplayName("[Схема] У user_role нет дополнительных ограничений UNIQUE")
    void shouldHaveNoAdditionalUniqueKeys() {
        schema.assertUniqueKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] user_id ссылается на app_user.id, role_id — на role.id, оба с ON DELETE CASCADE")
    void shouldHaveCascadingForeignKeysToUserAndRole() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey(USER_ID, "app_user", ID, "c"),
                new ForeignKey(ROLE_ID, "role", ID, "c"));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет назначение существующей роли пользователю")
    void shouldPersistUserRoleAssignment() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);

        int insertedRows = jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId);

        assertThat(insertedRows).as("number of assignments inserted").isEqualTo(1);
        assertThat(rolesForUser(userId)).as("assigned roles").containsExactly(roleId);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE заменяет назначенную роль другой существующей ролью")
    void shouldUpdateAssignedRole() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long otherRoleId = insertRole(jdbcTemplate, OTHER_ROLE, null);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId);

        int updatedRows = jdbcTemplate.update("UPDATE user_role SET role_id = ? WHERE user_id = ? AND role_id = ?",
                otherRoleId, userId, roleId);

        assertThat(updatedRows).as("number of assignments updated").isEqualTo(1);
        assertThat(rolesForUser(userId)).as("assigned roles after UPDATE").containsExactly(otherRoleId);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторной парой user_id и role_id отклоняется")
    void shouldRejectDuplicateAssignment() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId));

        assertAll(INSERT_ASSIGNMENT,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(rolesForUser(userId)).as("assignments after rejected INSERT").containsExactly(roleId));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в user_id отклоняется")
    void shouldRejectNullUserId() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ASSIGNMENT, null, roleId));

        assertAll(INSERT_ASSIGNMENT,
                () -> assertNotNullViolation(failure, USER_ID),
                () -> assertThat(usersForRole(roleId)).as("assignments after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в role_id отклоняется")
    void shouldRejectNullRoleId() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ASSIGNMENT, userId, null));

        assertAll(INSERT_ASSIGNMENT,
                () -> assertNotNullViolation(failure, ROLE_ID),
                () -> assertThat(rolesForUser(userId)).as("assignments after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего пользователя отклоняется")
    void shouldRejectMissingUser() {
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ASSIGNMENT, MISSING_ID, roleId));

        assertAll(INSERT_ASSIGNMENT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(usersForRole(roleId)).as("assignments after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующую роль отклоняется")
    void shouldRejectMissingRole() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ASSIGNMENT, userId, MISSING_ID));

        assertAll(INSERT_ASSIGNMENT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(rolesForUser(userId)).as("assignments after rejected INSERT").isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] DELETE назначения сохраняет остальные роли пользователя")
    void shouldDeleteOnlySelectedAssignment() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long otherRoleId = insertRole(jdbcTemplate, OTHER_ROLE, null);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, otherRoleId);

        int deletedRows = jdbcTemplate.update(
                "DELETE FROM user_role WHERE user_id = ? AND role_id = ?", userId, roleId);

        assertThat(deletedRows).as("number of assignments deleted").isEqualTo(1);
        assertThat(rolesForUser(userId)).as("remaining assigned roles").containsExactly(otherRoleId);
    }

    @Test
    @DisplayName("[Сценарий] Удаление пользователя каскадно удаляет его назначения")
    void shouldCascadeOnUserDelete() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);
        long otherUserId = insertUser(jdbcTemplate, OTHER_LOGIN);
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId);
        jdbcTemplate.update(INSERT_ASSIGNMENT, otherUserId, roleId);

        int deletedRows = jdbcTemplate.update("DELETE FROM app_user WHERE id = ?", userId);

        assertThat(deletedRows).as("number of users deleted").isEqualTo(1);
        assertThat(usersForRole(roleId)).as("users still assigned to the role").containsExactly(otherUserId);
    }

    @Test
    @DisplayName("[Сценарий] Удаление роли каскадно удаляет её назначения")
    void shouldCascadeOnRoleDelete() {
        long userId = insertUser(jdbcTemplate, TEST_LOGIN);
        long roleId = insertRole(jdbcTemplate, TEST_ROLE, null);
        long otherRoleId = insertRole(jdbcTemplate, OTHER_ROLE, null);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, roleId);
        jdbcTemplate.update(INSERT_ASSIGNMENT, userId, otherRoleId);

        int deletedRows = jdbcTemplate.update("DELETE FROM role WHERE id = ?", roleId);

        assertThat(deletedRows).as("number of roles deleted").isEqualTo(1);
        assertThat(rolesForUser(userId)).as("roles still assigned to the user").containsExactly(otherRoleId);
    }

    private List<Long> rolesForUser(long userId) {
        return jdbcTemplate.queryForList("SELECT role_id FROM user_role WHERE user_id = ?", Long.class, userId);
    }

    private List<Long> usersForRole(long roleId) {
        return jdbcTemplate.queryForList("SELECT user_id FROM user_role WHERE role_id = ?", Long.class, roleId);
    }
}
