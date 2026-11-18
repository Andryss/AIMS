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

import java.time.LocalDateTime;
import java.util.List;

import static gov.mib.aims.backend.db.util.RbacDbFixtures.MISSING_ID;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertUser;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.BIGINT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.INTEGER;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы incident")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class IncidentDbTest extends BaseDbTest {

    private static final String TABLE = "incident";
    private static final String TIMESTAMP = "timestamp without time zone";
    private static final String STATUS = "NEW";
    private static final String EVENT_TYPE = "OBSERVATION";
    private static final String LOCATION = "Сектор 3";
    private static final LocalDateTime DETECTED_AT = LocalDateTime.of(2026, 1, 15, 11, 30);
    private static final String DESCRIPTION = "Обнаружены следы на месте инцидента";

    private static final String INSERT_INCIDENT = """
            INSERT INTO incident (status, event_type, location, detected_at, description, created_by_user_id)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
    private static final String UPDATE_MONITORING_ALERT = "UPDATE incident SET monitoring_alert_id = ? WHERE id = ?";

    private static final RowMapper<IncidentRow> INCIDENT_ROW_MAPPER = (row, index) -> new IncidentRow(
            row.getString("status"),
            row.getString("event_type"),
            row.getString("location"),
            row.getObject("detected_at", LocalDateTime.class),
            row.getString("description"),
            row.getObject("victim_count", Integer.class),
            row.getObject("threat_level", Integer.class),
            row.getString("attachment_file_ids"),
            row.getLong("created_by_user_id"),
            row.getObject("responsible_user_id", Long.class),
            row.getString("executor_user_ids"),
            row.getObject("created_at", LocalDateTime.class),
            row.getObject("updated_at", LocalDateTime.class),
            row.getObject("alien_id", Long.class),
            row.getString("cleanup_status"),
            row.getObject("cleanup_report_id", Long.class),
            row.getObject("monitoring_alert_id", Long.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок учитывает поля, добавленные последующими миграциями")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "status", "event_type", "location", "detected_at", "description",
                "victim_count", "threat_level", "attachment_file_ids", "created_by_user_id", "responsible_user_id",
                "executor_user_ids", "created_at", "updated_at", "alien_id", "cleanup_status", "cleanup_report_id",
                "monitoring_alert_id");
    }

    @Test
    @DisplayName("[Схема] Типы, обязательность и значения по умолчанию соответствуют схеме")
    void shouldHaveExpectedColumnProperties() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("event_type", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("location", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("detected_at", TIMESTAMP)),
                () -> schema.assertColumn(TABLE, Column.required("description", TEXT)),
                () -> schema.assertColumn(TABLE, Column.optional("victim_count", INTEGER)),
                () -> schema.assertColumn(TABLE, Column.optional("threat_level", INTEGER)),
                () -> schema.assertColumn(TABLE, Column.required("attachment_file_ids", "jsonb", "'[]'::jsonb")),
                () -> schema.assertColumn(TABLE, Column.required("created_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("responsible_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("executor_user_ids", "jsonb", "'[]'::jsonb")),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP, "now()")),
                () -> schema.assertColumn(TABLE, Column.required("updated_at", TIMESTAMP, "now()")),
                () -> schema.assertColumn(TABLE, Column.optional("alien_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("cleanup_status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.optional("cleanup_report_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("monitoring_alert_id", BIGINT)));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Набор ограничений UNIQUE пуст")
    void shouldHaveNoUniqueConstraints() {
        schema.assertUniqueKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] Внешние ключи ссылаются на пользователей, вид, отчёт об очистке и алерт")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("created_by_user_id", "app_user", ID, "a"),
                new ForeignKey("responsible_user_id", "app_user", ID, "a"),
                new ForeignKey("alien_id", "alien", ID, "a"),
                new ForeignKey("cleanup_report_id", "cleanup_report", ID, "a"),
                new ForeignKey("monitoring_alert_id", "monitoring_alert", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Частичный уникальный индекс включает только строки с monitoring_alert_id")
    void shouldHaveUniquePartialIndexOnMonitoringAlert() {
        schema.assertIndexes(TABLE, new Index(true, List.of("monitoring_alert_id"), "monitoring_alert_id IS NOT NULL"));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет инцидент, генерирует id, даты и пустые JSON-массивы")
    void shouldCreateIncidentWithDefaults() {
        long creatorId = insertCreator();
        LocalDateTime before = databaseTime();

        long id = insertIncident(creatorId);
        IncidentRow saved = readIncident(id);
        LocalDateTime after = databaseTime();

        assertThat(id).isPositive();
        assertThat(saved.status()).isEqualTo(STATUS);
        assertThat(saved.eventType()).isEqualTo(EVENT_TYPE);
        assertThat(saved.location()).isEqualTo(LOCATION);
        assertThat(saved.detectedAt()).isEqualTo(DETECTED_AT);
        assertThat(saved.description()).isEqualTo(DESCRIPTION);
        assertThat(saved.creatorId()).isEqualTo(creatorId);
        assertThat(saved.attachmentFileIds()).isEqualTo("[]");
        assertThat(saved.executorUserIds()).isEqualTo("[]");
        assertThat(saved.createdAt()).isBetween(before, after);
        assertThat(saved.updatedAt()).isBetween(before, after);
        assertThat(saved.victimCount()).isNull();
        assertThat(saved.threatLevel()).isNull();
        assertThat(saved.responsibleId()).isNull();
        assertThat(saved.alienId()).isNull();
        assertThat(saved.cleanupStatus()).isNull();
        assertThat(saved.cleanupReportId()).isNull();
        assertThat(saved.monitoringAlertId()).isNull();
    }

    @Test
    @DisplayName("[Сценарий] UPDATE сохраняет оценку инцидента, назначения, вид и отчёт об очистке")
    void shouldUpdateIncidentDetailsAndLinks() {
        long creatorId = insertCreator();
        long id = insertIncident(creatorId);
        IncidentRow original = readIncident(id);
        long responsibleId = insertUser(jdbcTemplate, "db-test-incident-responsible");
        long alienId = jdbcTemplate.queryForObject("""
                INSERT INTO alien (name, description, threat_level)
                VALUES ('Тестовый вид', 'Описание вида', 3)
                RETURNING id
                """, Long.class);
        long reportId = jdbcTemplate.queryForObject("""
                INSERT INTO cleanup_report (incident_id, description, created_by_user_id)
                VALUES (?, 'Работы завершены', ?)
                RETURNING id
                """, Long.class, id, creatorId);
        String executorIds = "[" + responsibleId + "]";
        LocalDateTime updatedAt = databaseTime();

        int updated = jdbcTemplate.update("""
                UPDATE incident
                   SET status = ?, victim_count = ?, threat_level = ?, responsible_user_id = ?,
                       executor_user_ids = CAST(? AS jsonb), alien_id = ?, cleanup_status = ?, cleanup_report_id = ?,
                       updated_at = ?
                 WHERE id = ?
                """, "IN_PROGRESS", 2, 3, responsibleId, executorIds, alienId, "COMPLETED", reportId, updatedAt, id);

        assertThat(updated).isEqualTo(1);
        assertThat(readIncident(id)).isEqualTo(new IncidentRow("IN_PROGRESS", EVENT_TYPE, LOCATION, DETECTED_AT,
                DESCRIPTION, 2, 3, "[]", creatorId, responsibleId, executorIds, original.createdAt(), updatedAt,
                alienId, "COMPLETED", reportId, null));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранный инцидент и сохраняет второй инцидент пользователя")
    void shouldDeleteOnlySelectedIncident() {
        long creatorId = insertCreator();
        long id = insertIncident(creatorId);
        long otherId = insertIncident(creatorId);

        int deleted = jdbcTemplate.update("DELETE FROM incident WHERE id = ?", id);

        assertThat(deleted).isEqualTo(1);
        assertThat(incidentIds(creatorId)).containsExactly(otherId);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в status отклоняется")
    void shouldRejectNullStatus() {
        long creatorId = insertCreator();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_INCIDENT, null, EVENT_TYPE, LOCATION, DETECTED_AT, DESCRIPTION, creatorId));

        assertAll(
                () -> assertNotNullViolation(failure, "status"),
                () -> assertThat(incidentIds(creatorId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего создателя отклоняется")
    void shouldRejectMissingCreator() {
        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_INCIDENT, STATUS, EVENT_TYPE, LOCATION, DETECTED_AT, DESCRIPTION, MISSING_ID));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(incidentIds(MISSING_ID)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] UPDATE со ссылкой на несуществующий алерт отклоняется")
    void shouldRejectMissingMonitoringAlert() {
        long id = insertIncident(insertCreator());
        IncidentRow original = readIncident(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_MONITORING_ALERT, MISSING_ID, id));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readIncident(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] Два инцидента могут иметь NULL в monitoring_alert_id")
    void shouldAllowMultipleIncidentsWithoutMonitoringAlerts() {
        long creatorId = insertCreator();

        long firstId = insertIncident(creatorId);
        long secondId = insertIncident(creatorId);

        assertThat(firstId).isNotEqualTo(secondId);
        assertThat(readIncident(firstId).monitoringAlertId()).isNull();
        assertThat(readIncident(secondId).monitoringAlertId()).isNull();
    }

    @Test
    @DisplayName("[Сценарий] Повторная привязка одного алерта к другому инциденту отклоняется")
    void shouldRejectDuplicateMonitoringAlertLink() {
        long creatorId = insertCreator();
        long firstId = insertIncident(creatorId);
        long secondId = insertIncident(creatorId);
        long alertId = jdbcTemplate.queryForObject("""
                INSERT INTO monitoring_alert
                    (external_event_id, source_system, status, event_type, location, detected_at, description,
                     raw_payload)
                VALUES ('db-test-incident-alert', 'camera-network', 'NEW', 'OBSERVATION', ?, ?, ?, '{}'::jsonb)
                RETURNING id
                """, Long.class, LOCATION, DETECTED_AT, DESCRIPTION);
        assertThat(jdbcTemplate.update(UPDATE_MONITORING_ALERT, alertId, firstId)).isEqualTo(1);
        IncidentRow first = readIncident(firstId);
        IncidentRow second = readIncident(secondId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_MONITORING_ALERT, alertId, secondId));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(first.monitoringAlertId()).isEqualTo(alertId),
                () -> assertThat(second.monitoringAlertId()).isNull(),
                () -> assertThat(readIncident(firstId)).isEqualTo(first),
                () -> assertThat(readIncident(secondId)).isEqualTo(second));
    }

    private long insertCreator() {
        return insertUser(jdbcTemplate, "db-test-incident-creator");
    }

    private long insertIncident(long creatorId) {
        Long id = jdbcTemplate.queryForObject(INSERT_INCIDENT + " RETURNING id", Long.class,
                STATUS, EVENT_TYPE, LOCATION, DETECTED_AT, DESCRIPTION, creatorId);
        assertThat(id).as("id returned by INSERT INTO incident").isNotNull();
        return id;
    }

    private IncidentRow readIncident(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT status, event_type, location, detected_at, description, victim_count, threat_level,
                       attachment_file_ids, created_by_user_id, responsible_user_id, executor_user_ids, created_at,
                       updated_at, alien_id, cleanup_status, cleanup_report_id, monitoring_alert_id
                  FROM incident
                 WHERE id = ?
                """, INCIDENT_ROW_MAPPER, id);
    }

    private List<Long> incidentIds(long creatorId) {
        return jdbcTemplate.queryForList("SELECT id FROM incident WHERE created_by_user_id = ?", Long.class, creatorId);
    }

    private LocalDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()::timestamp", LocalDateTime.class);
    }

    private record IncidentRow(String status, String eventType, String location, LocalDateTime detectedAt,
                               String description, Integer victimCount, Integer threatLevel, String attachmentFileIds,
                               long creatorId, Long responsibleId, String executorUserIds, LocalDateTime createdAt,
                               LocalDateTime updatedAt, Long alienId, String cleanupStatus, Long cleanupReportId,
                               Long monitoringAlertId) {
    }
}
