package gov.mib.aims.backend.db;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.db.util.TableSchemaQueries;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Column;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.core.RowMapper;

import static gov.mib.aims.backend.db.util.RbacDbFixtures.INSERT_ROLE;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertRole;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы role")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class RoleDbTest extends BaseDbTest {

    private static final String TABLE = "role";
    private static final String NAME = "name";
    private static final String DESCRIPTION = "description";

    private static final String TEST_NAME = "db-test-role";
    private static final String OTHER_NAME = "db-test-other-role";
    private static final String UPDATED_NAME = "db-test-renamed-role";
    private static final String TEST_DESCRIPTION = "Исходное описание роли";
    private static final String UPDATED_DESCRIPTION = "Обновлённое описание роли";

    private static final String INSERT_ROLE_WITH_ID = "INSERT INTO role (id, name, description) VALUES (?, ?, ?)";
    private static final String UPDATE_NAME = "UPDATE role SET name = ? WHERE id = ?";

    private static final RowMapper<RoleRow> ROLE_ROW_MAPPER =
            (row, index) -> new RoleRow(row.getString(NAME), row.getString(DESCRIPTION));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] role содержит колонки id, name и description")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, NAME, DESCRIPTION);
    }

    @Test
    @DisplayName("[Схема] id имеет свойства BIGSERIAL и привязанную последовательность")
    void shouldHaveBigSerialId() {
        schema.assertColumn(TABLE, Column.bigSerialId(TABLE));
    }

    @Test
    @DisplayName("[Схема] name объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredNameColumn() {
        schema.assertColumn(TABLE, Column.required(NAME, TEXT));
    }

    @Test
    @DisplayName("[Схема] description имеет тип TEXT и допускает NULL")
    void shouldHaveOptionalDescriptionColumn() {
        schema.assertColumn(TABLE, Column.optional(DESCRIPTION, TEXT));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE задано для name")
    void shouldHaveUniqueKeyOnName() {
        schema.assertUniqueKeys(TABLE, new Key(NAME));
    }

    @Test
    @DisplayName("[Схема] Таблица role не объявляет внешних ключей")
    void shouldHaveNoOutgoingForeignKeys() {
        schema.assertForeignKeys(TABLE);
    }

    @Test
    @DisplayName("[Сценарий] INSERT создаёт id и сохраняет название и описание роли")
    void shouldGenerateIdAndPersistRole() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);
        RoleRow saved = readRole(id);

        assertThat(id).as("generated role.id").isPositive();
        assertThat(saved).as("saved role").isEqualTo(new RoleRow(TEST_NAME, TEST_DESCRIPTION));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет роль без описания")
    void shouldPersistRoleWithoutDescription() {
        long id = insertRole(jdbcTemplate, TEST_NAME, null);

        assertThat(readRole(id)).as("saved role without description").isEqualTo(new RoleRow(TEST_NAME, null));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет название роли и сохраняет её описание")
    void shouldRenameRoleAndPreserveDescription() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);

        int updatedRows = jdbcTemplate.update(UPDATE_NAME, UPDATED_NAME, id);

        assertThat(updatedRows).as("number of roles renamed").isEqualTo(1);
        assertThat(readRole(id)).as("renamed role").isEqualTo(new RoleRow(UPDATED_NAME, TEST_DESCRIPTION));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет описание роли и сохраняет её название")
    void shouldUpdateDescriptionAndPreserveName() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);

        int updatedRows = jdbcTemplate.update("UPDATE role SET description = ? WHERE id = ?", UPDATED_DESCRIPTION, id);

        assertThat(updatedRows).as("number of roles updated").isEqualTo(1);
        assertThat(readRole(id)).as("updated role").isEqualTo(new RoleRow(TEST_NAME, UPDATED_DESCRIPTION));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в name отклоняется")
    void shouldRejectNullNameOnInsert() {
        long countBefore = countRoles();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ROLE, null, TEST_DESCRIPTION));

        assertAll(INSERT_ROLE,
                () -> assertNotNullViolation(failure, NAME),
                () -> assertThat(countRoles()).as("role count after rejected INSERT").isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateIdOnInsert() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);
        long countBefore = countRoles();

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_ROLE_WITH_ID, id, OTHER_NAME, UPDATED_DESCRIPTION));

        assertAll(INSERT_ROLE_WITH_ID,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countRoles()).as("role count after rejected INSERT").isEqualTo(countBefore),
                () -> assertThat(readRole(id)).as("original role").isEqualTo(new RoleRow(TEST_NAME, TEST_DESCRIPTION)));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным названием роли отклоняется")
    void shouldRejectDuplicateNameOnInsert() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);
        long countBefore = countRoles();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ROLE, TEST_NAME, UPDATED_DESCRIPTION));

        assertAll(INSERT_ROLE,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countRoles()).as("role count after rejected INSERT").isEqualTo(countBefore),
                () -> assertThat(readRole(id)).as("original role").isEqualTo(new RoleRow(TEST_NAME, TEST_DESCRIPTION)));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE на занятое название роли отклоняется")
    void shouldRejectDuplicateNameOnUpdate() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);
        long otherId = insertRole(jdbcTemplate, OTHER_NAME, UPDATED_DESCRIPTION);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_NAME, OTHER_NAME, id));

        assertAll(UPDATE_NAME,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(readRole(id)).as("role after rejected UPDATE")
                        .isEqualTo(new RoleRow(TEST_NAME, TEST_DESCRIPTION)),
                () -> assertThat(readRole(otherId)).as("role already using the name")
                        .isEqualTo(new RoleRow(OTHER_NAME, UPDATED_DESCRIPTION)));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет роль без связанных записей")
    void shouldDeleteUnreferencedRole() {
        long id = insertRole(jdbcTemplate, TEST_NAME, TEST_DESCRIPTION);

        int deletedRows = jdbcTemplate.update("DELETE FROM role WHERE id = ?", id);
        Long remainingRows = jdbcTemplate.queryForObject("SELECT count(*) FROM role WHERE id = ?", Long.class, id);

        assertThat(deletedRows).as("number of roles deleted").isEqualTo(1);
        assertThat(remainingRows).as("remaining roles with id %s", id).isZero();
    }

    private RoleRow readRole(long id) {
        return jdbcTemplate.queryForObject("SELECT name, description FROM role WHERE id = ?", ROLE_ROW_MAPPER, id);
    }

    private long countRoles() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM role", Long.class);
    }

    private record RoleRow(String name, String description) {
    }
}
