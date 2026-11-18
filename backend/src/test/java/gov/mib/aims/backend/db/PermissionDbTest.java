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

import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы permission")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class PermissionDbTest extends BaseDbTest {

    private static final String TABLE = "permission";
    private static final String CODE = "code";
    private static final String DESCRIPTION = "description";

    private static final String TEST_CODE = "DB_TEST_NEW_PERMISSION";
    private static final String OTHER_CODE = "DB_TEST_OTHER_PERMISSION_CODE";
    private static final String UPDATED_CODE = "DB_TEST_UPDATED_PERMISSION";
    private static final String TEST_DESCRIPTION = "Исходное описание разрешения";
    private static final String UPDATED_DESCRIPTION = "Обновлённое описание разрешения";

    private static final String INSERT_PERMISSION = "INSERT INTO permission (code, description) VALUES (?, ?)";
    private static final String INSERT_PERMISSION_WITH_ID =
            "INSERT INTO permission (id, code, description) VALUES (?, ?, ?)";
    private static final String UPDATE_CODE = "UPDATE permission SET code = ? WHERE id = ?";

    private static final RowMapper<PermissionRow> PERMISSION_ROW_MAPPER =
            (row, index) -> new PermissionRow(row.getString(CODE), row.getString(DESCRIPTION));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] permission содержит колонки id, code и description")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, CODE, DESCRIPTION);
    }

    @Test
    @DisplayName("[Схема] id имеет свойства BIGSERIAL и привязанную последовательность")
    void shouldHaveBigSerialId() {
        schema.assertColumn(TABLE, Column.bigSerialId(TABLE));
    }

    @Test
    @DisplayName("[Схема] code объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredCodeColumn() {
        schema.assertColumn(TABLE, Column.required(CODE, TEXT));
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
    @DisplayName("[Схема] Ограничение UNIQUE задано для code")
    void shouldHaveUniqueKeyOnCode() {
        schema.assertUniqueKeys(TABLE, new Key(CODE));
    }

    @Test
    @DisplayName("[Схема] Таблица permission не объявляет внешних ключей")
    void shouldHaveNoOutgoingForeignKeys() {
        schema.assertForeignKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] У permission нет отдельных индексов")
    void shouldHaveNoStandaloneIndexes() {
        schema.assertIndexes(TABLE);
    }

    @Test
    @DisplayName("[Сценарий] INSERT создаёт id и сохраняет код и описание разрешения")
    void shouldGenerateIdAndPersistPermission() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);
        PermissionRow saved = readPermission(id);

        assertThat(id).as("generated permission.id").isPositive();
        assertThat(saved).as("saved permission").isEqualTo(new PermissionRow(TEST_CODE, TEST_DESCRIPTION));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет разрешение без описания")
    void shouldPersistPermissionWithoutDescription() {
        long id = insertPermission(TEST_CODE, null);

        assertThat(readPermission(id)).as("saved permission without description")
                .isEqualTo(new PermissionRow(TEST_CODE, null));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет код разрешения и сохраняет его описание")
    void shouldUpdatePermissionCodeAndPreserveDescription() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);

        int updatedRows = jdbcTemplate.update(UPDATE_CODE, UPDATED_CODE, id);

        assertThat(updatedRows).as("number of permissions updated").isEqualTo(1);
        assertThat(readPermission(id)).as("permission with updated code")
                .isEqualTo(new PermissionRow(UPDATED_CODE, TEST_DESCRIPTION));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет описание разрешения и сохраняет его код")
    void shouldUpdateDescriptionAndPreserveCode() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);

        int updatedRows = jdbcTemplate.update(
                "UPDATE permission SET description = ? WHERE id = ?", UPDATED_DESCRIPTION, id);

        assertThat(updatedRows).as("number of permissions updated").isEqualTo(1);
        assertThat(readPermission(id)).as("updated permission")
                .isEqualTo(new PermissionRow(TEST_CODE, UPDATED_DESCRIPTION));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в code отклоняется")
    void shouldRejectNullCodeOnInsert() {
        long countBefore = countPermissions();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_PERMISSION, null, TEST_DESCRIPTION));

        assertAll(INSERT_PERMISSION,
                () -> assertNotNullViolation(failure, CODE),
                () -> assertThat(countPermissions()).as("permission count after rejected INSERT")
                        .isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateIdOnInsert() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);
        long countBefore = countPermissions();

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_PERMISSION_WITH_ID, id, OTHER_CODE, UPDATED_DESCRIPTION));

        assertAll(INSERT_PERMISSION_WITH_ID,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countPermissions()).as("permission count after rejected INSERT")
                        .isEqualTo(countBefore),
                () -> assertThat(readPermission(id)).as("original permission")
                        .isEqualTo(new PermissionRow(TEST_CODE, TEST_DESCRIPTION)));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным кодом разрешения отклоняется")
    void shouldRejectDuplicateCodeOnInsert() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);
        long countBefore = countPermissions();

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_PERMISSION, TEST_CODE, UPDATED_DESCRIPTION));

        assertAll(INSERT_PERMISSION,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countPermissions()).as("permission count after rejected INSERT")
                        .isEqualTo(countBefore),
                () -> assertThat(readPermission(id)).as("original permission")
                        .isEqualTo(new PermissionRow(TEST_CODE, TEST_DESCRIPTION)));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE на занятый код разрешения отклоняется")
    void shouldRejectDuplicateCodeOnUpdate() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);
        long otherId = insertPermission(OTHER_CODE, UPDATED_DESCRIPTION);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_CODE, OTHER_CODE, id));

        assertAll(UPDATE_CODE,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(readPermission(id)).as("permission after rejected UPDATE")
                        .isEqualTo(new PermissionRow(TEST_CODE, TEST_DESCRIPTION)),
                () -> assertThat(readPermission(otherId)).as("permission already using the code")
                        .isEqualTo(new PermissionRow(OTHER_CODE, UPDATED_DESCRIPTION)));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет разрешение без связанных записей")
    void shouldDeleteUnreferencedPermission() {
        long id = insertPermission(TEST_CODE, TEST_DESCRIPTION);

        int deletedRows = jdbcTemplate.update("DELETE FROM permission WHERE id = ?", id);
        Long remainingRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM permission WHERE id = ?", Long.class, id);

        assertThat(deletedRows).as("number of permissions deleted").isEqualTo(1);
        assertThat(remainingRows).as("remaining permissions with id %s", id).isZero();
    }

    private long insertPermission(String code, String description) {
        Long id = jdbcTemplate.queryForObject(INSERT_PERMISSION + " RETURNING id", Long.class, code, description);
        assertThat(id).as("id returned by INSERT INTO permission").isNotNull();
        return id;
    }

    private PermissionRow readPermission(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT code, description FROM permission WHERE id = ?", PERMISSION_ROW_MAPPER, id);
    }

    private long countPermissions() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM permission", Long.class);
    }

    private record PermissionRow(String code, String description) {
    }
}
