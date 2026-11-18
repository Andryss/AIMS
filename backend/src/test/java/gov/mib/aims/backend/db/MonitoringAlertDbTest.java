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

import static gov.mib.aims.backend.db.util.IncidentDbFixtures.insertIncident;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.MISSING_ID;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertUser;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.BIGINT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы monitoring_alert")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class MonitoringAlertDbTest extends BaseDbTest {

    private static final String TABLE = "monitoring_alert";
    private static final String TIMESTAMP = "timestamp without time zone";
    private static final String EXTERNAL_EVENT_ID = "db-test-monitoring-event";
    private static final String SOURCE_SYSTEM = "camera-network";
    private static final String STATUS = "NEW";
    private static final String EVENT_TYPE = "SIGHTING";
    private static final String LOCATION = "Сектор 7";
    private static final LocalDateTime DETECTED_AT = LocalDateTime.of(2026, 1, 15, 10, 30);
    private static final String DESCRIPTION = "Камера зафиксировала движение";
    private static final String RAW_PAYLOAD = "{\"signal\": 7}";

    private static final String INSERT_ALERT = """
            INSERT INTO monitoring_alert
                (external_event_id, source_system, status, event_type, location, detected_at, description, raw_payload)
            VALUES (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb))
            """;

    private static final RowMapper<AlertRow> ALERT_ROW_MAPPER = (row, index) -> new AlertRow(
            row.getString("external_event_id"),
            row.getString("source_system"),
            row.getString("status"),
            row.getString("event_type"),
            row.getString("location"),
            row.getObject("detected_at", LocalDateTime.class),
            row.getString("description"),
            row.getString("media_urls"),
            row.getString("raw_payload"),
            row.getObject("incident_id", Long.class),
            row.getObject("received_at", LocalDateTime.class),
            row.getObject("created_at", LocalDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует схеме")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "external_event_id", "source_system", "status", "event_type", "location",
                "detected_at", "description", "media_urls", "raw_payload", "incident_id", "received_at", "created_at");
    }

    @Test
    @DisplayName("[Схема] Типы, обязательность и значения по умолчанию соответствуют схеме")
    void shouldHaveExpectedColumnProperties() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("external_event_id", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("source_system", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("event_type", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("location", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("detected_at", TIMESTAMP)),
                () -> schema.assertColumn(TABLE, Column.required("description", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("media_urls", "jsonb", "'[]'::jsonb")),
                () -> schema.assertColumn(TABLE, Column.required("raw_payload", "jsonb")),
                () -> schema.assertColumn(TABLE, Column.optional("incident_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("received_at", TIMESTAMP, "now()")),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP, "now()")));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE задано для external_event_id")
    void shouldHaveUniqueKeyOnExternalEventId() {
        schema.assertUniqueKeys(TABLE, new Key("external_event_id"));
    }

    @Test
    @DisplayName("[Схема] Внешний ключ incident_id ссылается на incident.id")
    void shouldHaveForeignKeyToIncident() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("incident_id", "incident", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Неуникальные индексы заданы для status и incident_id")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(false, List.of("status"), null),
                new Index(false, List.of("incident_id"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет событие, генерирует id, даты и пустой массив media_urls")
    void shouldCreateAlertWithDefaults() {
        LocalDateTime before = databaseTime();

        long id = insertAlert(EXTERNAL_EVENT_ID);
        AlertRow saved = readAlert(id);
        LocalDateTime after = databaseTime();

        assertThat(id).isPositive();
        assertThat(saved.externalEventId()).isEqualTo(EXTERNAL_EVENT_ID);
        assertThat(saved.sourceSystem()).isEqualTo(SOURCE_SYSTEM);
        assertThat(saved.status()).isEqualTo(STATUS);
        assertThat(saved.eventType()).isEqualTo(EVENT_TYPE);
        assertThat(saved.location()).isEqualTo(LOCATION);
        assertThat(saved.detectedAt()).isEqualTo(DETECTED_AT);
        assertThat(saved.description()).isEqualTo(DESCRIPTION);
        assertThat(saved.mediaUrls()).isEqualTo("[]");
        assertThat(saved.rawPayload()).isEqualTo(RAW_PAYLOAD);
        assertThat(saved.incidentId()).isNull();
        assertThat(saved.receivedAt()).isBetween(before, after);
        assertThat(saved.createdAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет статус алерта и связывает его с существующим инцидентом")
    void shouldUpdateStatusAndLinkToIncident() {
        long incidentId = insertIncident(jdbcTemplate, insertUser(jdbcTemplate, "db-test-monitoring-incident-creator"));
        long id = insertAlert(EXTERNAL_EVENT_ID);
        AlertRow original = readAlert(id);

        int updated = jdbcTemplate.update("UPDATE monitoring_alert SET status = ?, incident_id = ? WHERE id = ?",
                "LINKED", incidentId, id);
        AlertRow saved = readAlert(id);

        assertThat(updated).isEqualTo(1);
        assertThat(saved.status()).isEqualTo("LINKED");
        assertThat(saved.incidentId()).isEqualTo(incidentId);
        assertThat(saved).usingRecursiveComparison().ignoringFields("status", "incidentId").isEqualTo(original);
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранный алерт и сохраняет другое событие")
    void shouldDeleteOnlySelectedAlert() {
        long id = insertAlert(EXTERNAL_EVENT_ID);
        String otherEventId = "db-test-monitoring-other-event";
        long otherId = insertAlert(otherEventId);

        int deleted = jdbcTemplate.update("DELETE FROM monitoring_alert WHERE id = ?", id);

        assertThat(deleted).isEqualTo(1);
        assertThat(alertIds(EXTERNAL_EVENT_ID)).isEmpty();
        assertThat(alertIds(otherEventId)).containsExactly(otherId);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в raw_payload отклоняется")
    void shouldRejectNullRawPayload() {
        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ALERT,
                EXTERNAL_EVENT_ID, SOURCE_SYSTEM, STATUS, EVENT_TYPE, LOCATION, DETECTED_AT, DESCRIPTION, null));

        assertAll(
                () -> assertNotNullViolation(failure, "raw_payload"),
                () -> assertThat(alertIds(EXTERNAL_EVENT_ID)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] UPDATE со ссылкой на несуществующий инцидент отклоняется")
    void shouldRejectMissingIncident() {
        long id = insertAlert(EXTERNAL_EVENT_ID);
        AlertRow original = readAlert(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                "UPDATE monitoring_alert SET incident_id = ? WHERE id = ?", MISSING_ID, id));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readAlert(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным external_event_id отклоняется даже для другого источника")
    void shouldRejectDuplicateExternalEventId() {
        long id = insertAlert(EXTERNAL_EVENT_ID);
        AlertRow original = readAlert(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ALERT,
                EXTERNAL_EVENT_ID, "other-monitoring-system", STATUS, EVENT_TYPE, LOCATION, DETECTED_AT, DESCRIPTION,
                RAW_PAYLOAD));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(alertIds(EXTERNAL_EVENT_ID)).containsExactly(id),
                () -> assertThat(readAlert(id)).isEqualTo(original));
    }

    private long insertAlert(String externalEventId) {
        Long id = jdbcTemplate.queryForObject(INSERT_ALERT + " RETURNING id", Long.class,
                externalEventId, SOURCE_SYSTEM, STATUS, EVENT_TYPE, LOCATION, DETECTED_AT, DESCRIPTION, RAW_PAYLOAD);
        assertThat(id).as("id returned by INSERT INTO monitoring_alert").isNotNull();
        return id;
    }

    private AlertRow readAlert(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT external_event_id, source_system, status, event_type, location, detected_at, description,
                       media_urls, raw_payload, incident_id, received_at, created_at
                  FROM monitoring_alert
                 WHERE id = ?
                """, ALERT_ROW_MAPPER, id);
    }

    private List<Long> alertIds(String externalEventId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM monitoring_alert WHERE external_event_id = ?", Long.class, externalEventId);
    }

    private LocalDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()::timestamp", LocalDateTime.class);
    }

    private record AlertRow(String externalEventId, String sourceSystem, String status, String eventType,
                            String location, LocalDateTime detectedAt, String description, String mediaUrls,
                            String rawPayload, Long incidentId, LocalDateTime receivedAt, LocalDateTime createdAt) {
    }
}
