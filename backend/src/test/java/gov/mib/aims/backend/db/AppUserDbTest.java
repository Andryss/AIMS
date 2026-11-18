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

import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы app_user")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AppUserDbTest extends BaseDbTest {

    private static final String TABLE = "app_user";
    private static final String LOGIN = "login";
    private static final String PASSWORD_HASH = "password_hash";

    private static final RowMapper<UserRow> USER_ROW_MAPPER =
            (row, index) -> new UserRow(row.getString(LOGIN), row.getString(PASSWORD_HASH));

    private static final String TEST_LOGIN = "db-test-оператор";
    private static final String OTHER_LOGIN = "db-test-other";
    private static final String UPDATED_LOGIN = "db-test-updated";
    private static final String ORIGINAL_HASH = "original-hash";
    private static final String UPDATED_HASH = "updated-hash";

    private static final String INSERT_USER = "INSERT INTO app_user (login, password_hash) VALUES (?, ?)";
    private static final String INSERT_USER_WITH_ID = "INSERT INTO app_user (id, login, password_hash) VALUES (?, ?, ?)";
    private static final String UPDATE_LOGIN = "UPDATE app_user SET login = ? WHERE id = ?";
    private static final String UPDATE_PASSWORD_HASH = "UPDATE app_user SET password_hash = ? WHERE id = ?";
    private static final String DELETE_USER = "DELETE FROM app_user WHERE id = ?";

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок app_user соответствует схеме")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, LOGIN, PASSWORD_HASH);
    }

    @Test
    @DisplayName("[Схема] id имеет свойства BIGSERIAL и привязанную последовательность")
    void shouldHaveBigSerialId() {
        schema.assertColumn(TABLE, Column.bigSerialId(TABLE));
    }

    @Test
    @DisplayName("[Схема] login объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredLoginColumn() {
        schema.assertColumn(TABLE, Column.required(LOGIN, TEXT));
    }

    @Test
    @DisplayName("[Схема] password_hash объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredPasswordHashColumn() {
        schema.assertColumn(TABLE, Column.required(PASSWORD_HASH, TEXT));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE задано для login")
    void shouldHaveUniqueKeyOnLogin() {
        schema.assertUniqueKeys(TABLE, new Key(LOGIN));
    }

    @Test
    @DisplayName("[Схема] Таблица app_user не объявляет внешних ключей")
    void shouldHaveNoOutgoingForeignKeys() {
        schema.assertForeignKeys(TABLE);
    }

    @Test
    @DisplayName("[Сценарий] INSERT создаёт id и сохраняет login и password_hash")
    void shouldGenerateIdAndPersistUser() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);
        UserRow saved = readUser(id);

        assertThat(id).as("generated app_user.id").isPositive();
        assertThat(saved.login()).as("saved login").isEqualTo(TEST_LOGIN);
        assertThat(saved.passwordHash()).as("saved password hash").isEqualTo(ORIGINAL_HASH);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет login и сохраняет прежний password_hash")
    void shouldUpdateLoginAndPreservePasswordHash() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);

        int updatedRows = jdbcTemplate.update(UPDATE_LOGIN, UPDATED_LOGIN, id);
        UserRow updated = readUser(id);

        assertThat(updatedRows).as("number of app_user rows updated").isEqualTo(1);
        assertThat(updated.login()).as("updated login").isEqualTo(UPDATED_LOGIN);
        assertThat(updated.passwordHash()).as("unchanged password hash").isEqualTo(ORIGINAL_HASH);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет password_hash и сохраняет прежний login")
    void shouldUpdatePasswordHashAndPreserveLogin() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);

        int updatedRows = jdbcTemplate.update(UPDATE_PASSWORD_HASH, UPDATED_HASH, id);
        UserRow updated = readUser(id);

        assertThat(updatedRows).as("number of app_user rows updated").isEqualTo(1);
        assertThat(updated.passwordHash()).as("updated password hash").isEqualTo(UPDATED_HASH);
        assertThat(updated.login()).as("unchanged login").isEqualTo(TEST_LOGIN);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в login отклоняется")
    void shouldRejectNullLoginOnInsert() {
        long countBefore = countUsers();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_USER, null, ORIGINAL_HASH));

        assertAll(INSERT_USER,
                () -> assertNotNullViolation(failure, LOGIN),
                () -> assertThat(countUsers()).as("app_user count after INSERT").isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в password_hash отклоняется")
    void shouldRejectNullPasswordHashOnInsert() {
        long countBefore = countUsers();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_USER, TEST_LOGIN, null));

        assertAll(INSERT_USER,
                () -> assertNotNullViolation(failure, PASSWORD_HASH),
                () -> assertThat(countUsers()).as("app_user count after INSERT").isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateIdOnInsert() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);
        UserRow original = readUser(id);
        long countBefore = countUsers();

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_USER_WITH_ID, id, OTHER_LOGIN, UPDATED_HASH));

        assertAll(INSERT_USER_WITH_ID,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countUsers()).as("app_user count after INSERT").isEqualTo(countBefore),
                () -> assertThat(readUser(id)).as("original user after INSERT").isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным login отклоняется")
    void shouldRejectDuplicateLoginOnInsert() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);
        UserRow original = readUser(id);
        long countBefore = countUsers();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_USER, TEST_LOGIN, UPDATED_HASH));

        assertAll(INSERT_USER,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countUsers()).as("app_user count after INSERT").isEqualTo(countBefore),
                () -> assertThat(readUser(id)).as("original user after INSERT").isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE на занятый login отклоняется")
    void shouldRejectDuplicateLoginOnUpdate() {
        long ownerId = insertUser(OTHER_LOGIN, UPDATED_HASH);
        long userId = insertUser(TEST_LOGIN, ORIGINAL_HASH);
        UserRow originalOwner = readUser(ownerId);
        UserRow originalUser = readUser(userId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_LOGIN, OTHER_LOGIN, userId));

        assertAll(UPDATE_LOGIN,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(readUser(userId)).as("updated user after rejected UPDATE").isEqualTo(originalUser),
                () -> assertThat(readUser(ownerId)).as("login owner after rejected UPDATE").isEqualTo(originalOwner));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE с NULL в password_hash отклоняется")
    void shouldRejectNullPasswordHashOnUpdate() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);
        UserRow original = readUser(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_PASSWORD_HASH, null, id));

        assertAll(UPDATE_PASSWORD_HASH,
                () -> assertNotNullViolation(failure, PASSWORD_HASH),
                () -> assertThat(readUser(id)).as("user after rejected UPDATE").isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет пользователя, на которого нет ссылок")
    void shouldDeleteUnreferencedUser() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);

        int deletedRows = jdbcTemplate.update(DELETE_USER, id);
        Long remainingRows = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM app_user WHERE id = ?", Long.class, id);

        assertThat(deletedRows).as("number of app_user rows deleted").isEqualTo(1);
        assertThat(remainingRows).as("app_user rows with id %s after DELETE", id).isZero();
    }

    @Test
    @DisplayName("[Сценарий] DELETE отклоняется, когда на пользователя ссылается инцидент")
    void shouldRejectDeletingUserReferencedByIncident() {
        long id = insertUser(TEST_LOGIN, ORIGINAL_HASH);
        UserRow original = readUser(id);
        Long incidentId = jdbcTemplate.queryForObject("""
                INSERT INTO incident (status, event_type, location, detected_at, description, created_by_user_id)
                VALUES ('NEW', 'OBSERVATION', 'Test location', LOCALTIMESTAMP, 'Test incident', ?)
                RETURNING id
                """, Long.class, id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(DELETE_USER, id));

        assertAll(DELETE_USER,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readUser(id)).as("user after rejected DELETE").isEqualTo(original),
                () -> assertThat(jdbcTemplate.queryForObject(
                        "SELECT created_by_user_id FROM incident WHERE id = ?", Long.class, incidentId))
                        .as("incident author after rejected DELETE")
                        .isEqualTo(id));
    }

    private long insertUser(String login, String passwordHash) {
        Long id = jdbcTemplate.queryForObject(INSERT_USER + " RETURNING id", Long.class, login, passwordHash);
        assertThat(id).as("id returned by INSERT INTO app_user").isNotNull();
        return id;
    }

    private UserRow readUser(long id) {
        return jdbcTemplate.queryForObject("SELECT login, password_hash FROM app_user WHERE id = ?",
                USER_ROW_MAPPER, id);
    }

    private long countUsers() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM app_user", Long.class);
    }

    private record UserRow(String login, String passwordHash) {
    }
}
