package gov.mib.aims.backend.db;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.db.util.TableSchemaQueries;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Column;
import gov.mib.aims.backend.db.util.TableSchemaQueries.ForeignKey;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Index;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.core.RowMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static gov.mib.aims.backend.db.util.IncidentDbFixtures.insertIncident;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.MISSING_ID;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertUser;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.BIGINT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.INTEGER;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TIMESTAMP_WITH_TIME_ZONE;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы detention_request")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class DetentionRequestDbTest extends BaseDbTest {

    private static final String TABLE = "detention_request";
    private static final String CREATOR_LOGIN = "db-test-detention-creator";
    private static final String SUBJECT_NAME = "Субъект A";
    private static final String SUBJECT_DESCRIPTION = "Задержан у места инцидента";
    private static final int DURATION_DAYS = 3;
    private static final OffsetDateTime UPDATED_AT = OffsetDateTime.parse("2026-02-01T10:00:00Z");
    private static final String INSERT_REQUEST = """
            INSERT INTO detention_request (
                incident_id, subject_name, subject_description, duration_days, status, created_by_user_id, updated_at
            )
            VALUES (?, ?, ?, ?, 'NEW', ?, ?)
            """;

    private static final RowMapper<RequestRow> REQUEST_ROW_MAPPER = (row, index) -> new RequestRow(
            row.getLong("incident_id"),
            row.getString("subject_name"),
            row.getString("subject_description"),
            row.getObject("alien_id", Long.class),
            row.getInt("duration_days"),
            row.getString("status"),
            row.getLong("created_by_user_id"),
            row.getObject("reviewed_by_user_id", Long.class),
            row.getObject("created_at", OffsetDateTime.class),
            row.getObject("updated_at", OffsetDateTime.class),
            row.getObject("admitted_at", OffsetDateTime.class),
            row.getObject("released_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок detention_request")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "incident_id", "subject_name", "subject_description", "alien_id",
                "duration_days", "status", "created_by_user_id", "reviewed_by_user_id", "created_at", "updated_at",
                "admitted_at", "released_at");
    }

    @Test
    @DisplayName("[Схема] Типы, допустимость NULL и значения по умолчанию полей заявки")
    void shouldHaveExpectedRequestColumns() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("incident_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("subject_name", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("subject_description", TEXT)),
                () -> schema.assertColumn(TABLE, Column.optional("alien_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("duration_days", INTEGER)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("created_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("reviewed_by_user_id", BIGINT)));
    }

    @Test
    @DisplayName("[Схема] Типы, допустимость NULL и значения по умолчанию временных полей")
    void shouldHaveExpectedTimestampColumns() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")),
                () -> schema.assertColumn(TABLE, Column.required("updated_at", TIMESTAMP_WITH_TIME_ZONE)),
                () -> schema.assertColumn(TABLE, Column.optional("admitted_at", TIMESTAMP_WITH_TIME_ZONE)),
                () -> schema.assertColumn(TABLE, Column.optional("released_at", TIMESTAMP_WITH_TIME_ZONE)));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ по id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Отсутствуют дополнительные ограничения UNIQUE")
    void shouldHaveNoUniqueKeys() {
        schema.assertUniqueKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] Внешние ключи на инцидент, инопланетянина, автора и проверяющего с ON DELETE NO ACTION")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("incident_id", "incident", ID, "a"),
                new ForeignKey("alien_id", "alien", ID, "a"),
                new ForeignKey("created_by_user_id", "app_user", ID, "a"),
                new ForeignKey("reviewed_by_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы по (incident_id, status) и (created_at DESC, id DESC)")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(false, List.of("incident_id", "status"), null),
                new Index(false, List.of("created_at DESC", "id DESC"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет новую заявку со ссылками на инцидент и автора")
    void shouldPersistNewRequestWithDefaults() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        OffsetDateTime before = databaseTime();

        long id = insertRequest(incidentId, creatorId);
        RequestRow saved = readRequest(id);
        OffsetDateTime after = databaseTime();

        assertThat(id).isPositive();
        assertThat(saved).isEqualTo(new RequestRow(incidentId, SUBJECT_NAME, SUBJECT_DESCRIPTION, null,
                DURATION_DAYS, "NEW", creatorId, null, saved.createdAt(), UPDATED_AT, null, null));
        assertThat(saved.createdAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE сохраняет статус, проверяющего, вид инопланетянина и даты заключения")
    void shouldUpdateReviewAndCustody() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertRequest(incidentId, creatorId);
        RequestRow original = readRequest(id);
        long reviewerId = insertUser(jdbcTemplate, "db-test-detention-reviewer");
        long alienId = jdbcTemplate.queryForObject("""
                INSERT INTO alien (name, description, threat_level)
                VALUES ('Субъект A', 'Описание вида', 2)
                RETURNING id
                """, Long.class);
        OffsetDateTime admittedAt = original.createdAt().plusHours(2);
        OffsetDateTime releasedAt = admittedAt.plusDays(DURATION_DAYS);

        int updatedRows = jdbcTemplate.update("""
                UPDATE detention_request
                   SET status = 'RELEASED', alien_id = ?, reviewed_by_user_id = ?,
                       admitted_at = ?, released_at = ?, updated_at = ?
                 WHERE id = ?
                """, alienId, reviewerId, admittedAt, releasedAt, releasedAt, id);
        RequestRow updated = readRequest(id);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(updated).isEqualTo(new RequestRow(incidentId, SUBJECT_NAME, SUBJECT_DESCRIPTION, alienId,
                DURATION_DAYS, "RELEASED", creatorId, reviewerId, original.createdAt(), releasedAt,
                admittedAt, releasedAt));
    }

    @Test
    @DisplayName("[Сценарий] INSERT без обязательного updated_at отклоняется")
    void shouldRejectRequestWithoutUpdatedAt() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long countBefore = countRequests();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO detention_request (
                    incident_id, subject_name, subject_description, duration_days, status, created_by_user_id
                )
                VALUES (?, ?, ?, ?, 'NEW', ?)
                """, incidentId, SUBJECT_NAME, SUBJECT_DESCRIPTION, DURATION_DAYS, creatorId));

        assertAll(
                () -> assertNotNullViolation(failure, "updated_at"),
                () -> assertThat(countRequests()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующий инцидент отклоняется")
    void shouldRejectMissingIncident() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long countBefore = countRequests();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_REQUEST, MISSING_ID, SUBJECT_NAME, SUBJECT_DESCRIPTION, DURATION_DAYS, creatorId, UPDATED_AT));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countRequests()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего автора отклоняется")
    void shouldRejectMissingCreator() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long countBefore = countRequests();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_REQUEST, incidentId, SUBJECT_NAME, SUBJECT_DESCRIPTION, DURATION_DAYS, MISSING_ID, UPDATED_AT));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countRequests()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE со ссылкой на несуществующего инопланетянина отклоняется")
    void shouldRejectMissingAlien() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertRequest(incidentId, creatorId);
        RequestRow original = readRequest(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                "UPDATE detention_request SET alien_id = ? WHERE id = ?", MISSING_ID, id));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readRequest(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE со ссылкой на несуществующего проверяющего отклоняется")
    void shouldRejectMissingReviewer() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertRequest(incidentId, creatorId);
        RequestRow original = readRequest(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                "UPDATE detention_request SET reviewed_by_user_id = ? WHERE id = ?", MISSING_ID, id));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readRequest(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateId() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertRequest(incidentId, creatorId);
        RequestRow original = readRequest(id);
        long countBefore = countRequests();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO detention_request (
                    id, incident_id, subject_name, subject_description, duration_days, status, created_by_user_id,
                    updated_at
                )
                VALUES (?, ?, 'Другой субъект', ?, ?, 'NEW', ?, ?)
                """, id, incidentId, SUBJECT_DESCRIPTION, DURATION_DAYS, creatorId, UPDATED_AT));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countRequests()).isEqualTo(countBefore),
                () -> assertThat(readRequest(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранную заявку и сохраняет остальные заявки по инциденту")
    void shouldDeleteOnlySelectedRequest() {
        long creatorId = insertUser(jdbcTemplate, CREATOR_LOGIN);
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertRequest(incidentId, creatorId);
        long otherId = insertRequest(incidentId, creatorId);
        RequestRow originalOther = readRequest(otherId);

        int deletedRows = jdbcTemplate.update("DELETE FROM detention_request WHERE id = ?", id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT id FROM detention_request WHERE incident_id = ?", Long.class, incidentId))
                .containsExactly(otherId);
        assertThat(readRequest(otherId)).isEqualTo(originalOther);
    }

    private long insertRequest(long incidentId, long creatorId) {
        return jdbcTemplate.queryForObject(INSERT_REQUEST + "RETURNING id", Long.class,
                incidentId, SUBJECT_NAME, SUBJECT_DESCRIPTION, DURATION_DAYS, creatorId, UPDATED_AT);
    }

    private RequestRow readRequest(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT incident_id, subject_name, subject_description, alien_id, duration_days, status,
                       created_by_user_id, reviewed_by_user_id, created_at, updated_at, admitted_at, released_at
                  FROM detention_request
                 WHERE id = ?
                """, REQUEST_ROW_MAPPER, id);
    }

    private long countRequests() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM detention_request", Long.class);
    }

    private OffsetDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private record RequestRow(long incidentId, String subjectName, String subjectDescription, Long alienId,
                              int durationDays, String status, long createdByUserId, Long reviewedByUserId,
                              OffsetDateTime createdAt, OffsetDateTime updatedAt, OffsetDateTime admittedAt,
                              OffsetDateTime releasedAt) {
    }
}
