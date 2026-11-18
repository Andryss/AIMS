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

@DisplayName("Тестирование таблицы cleanup_report")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class CleanupReportDbTest extends BaseDbTest {

    private static final String TABLE = "cleanup_report";
    private static final String DESCRIPTION = "Территория очищена";
    private static final String UPDATED_DESCRIPTION = "Территория очищена, снимки приложены";

    private static final String INSERT_REPORT = """
            INSERT INTO cleanup_report (incident_id, description, created_by_user_id)
            VALUES (?, ?, ?)
            """;

    private static final RowMapper<ReportRow> REPORT_ROW_MAPPER = (row, index) -> new ReportRow(
            row.getLong("incident_id"),
            row.getString("description"),
            row.getString("attachment_file_ids"),
            row.getLong("created_by_user_id"),
            row.getObject("created_at", LocalDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует схеме")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "incident_id", "description", "attachment_file_ids", "created_by_user_id",
                "created_at");
    }

    @Test
    @DisplayName("[Схема] Типы, обязательность и значения по умолчанию соответствуют схеме")
    void shouldHaveExpectedColumnProperties() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("incident_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("description", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("attachment_file_ids", "jsonb", "'[]'::jsonb")),
                () -> schema.assertColumn(TABLE, Column.required("created_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE,
                        Column.required("created_at", "timestamp without time zone", "now()")));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE задано для incident_id")
    void shouldHaveUniqueKeyOnIncidentId() {
        schema.assertUniqueKeys(TABLE, new Key("incident_id"));
    }

    @Test
    @DisplayName("[Схема] Внешние ключи связывают отчёт с инцидентом и создателем")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("incident_id", "incident", ID, "a"),
                new ForeignKey("created_by_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы создаются только для ограничений")
    void shouldHaveNoStandaloneIndexes() {
        schema.assertIndexes(TABLE);
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет отчёт, генерирует id, дату создания и пустой массив вложений")
    void shouldCreateReportWithDefaults() {
        long creatorId = insertCreator();
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        LocalDateTime before = databaseTime();

        long id = insertReport(incidentId, creatorId);
        ReportRow saved = readReport(id);

        assertThat(id).isPositive();
        assertThat(saved.incidentId()).isEqualTo(incidentId);
        assertThat(saved.description()).isEqualTo(DESCRIPTION);
        assertThat(saved.attachmentFileIds()).isEqualTo("[]");
        assertThat(saved.creatorId()).isEqualTo(creatorId);
        assertThat(saved.createdAt()).isBetween(before, databaseTime());
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет описание и список вложений отчёта")
    void shouldUpdateReport() {
        long creatorId = insertCreator();
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertReport(incidentId, creatorId);
        ReportRow original = readReport(id);

        int updated = jdbcTemplate.update("""
                UPDATE cleanup_report
                   SET description = ?, attachment_file_ids = CAST(? AS jsonb)
                 WHERE id = ?
                """, UPDATED_DESCRIPTION, "[42]", id);

        assertThat(updated).isEqualTo(1);
        assertThat(readReport(id)).isEqualTo(
                new ReportRow(incidentId, UPDATED_DESCRIPTION, "[42]", creatorId, original.createdAt()));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет отчёт и сохраняет инцидент")
    void shouldDeleteReportAndPreserveIncident() {
        long creatorId = insertCreator();
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertReport(incidentId, creatorId);

        int deleted = jdbcTemplate.update("DELETE FROM cleanup_report WHERE id = ?", id);

        assertThat(deleted).isEqualTo(1);
        assertThat(reportIds(incidentId)).isEmpty();
        assertThat(jdbcTemplate.queryForList("SELECT id FROM incident WHERE id = ?", Long.class, incidentId))
                .containsExactly(incidentId);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в description отклоняется")
    void shouldRejectNullDescription() {
        long creatorId = insertCreator();
        long incidentId = insertIncident(jdbcTemplate, creatorId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_REPORT, incidentId, null, creatorId));

        assertAll(
                () -> assertNotNullViolation(failure, "description"),
                () -> assertThat(reportIds(incidentId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующий инцидент отклоняется")
    void shouldRejectMissingIncident() {
        long creatorId = insertCreator();

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_REPORT, MISSING_ID, DESCRIPTION, creatorId));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(reportIds(MISSING_ID)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего создателя отклоняется")
    void shouldRejectMissingCreator() {
        long incidentId = insertIncident(jdbcTemplate, insertCreator());

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_REPORT, incidentId, DESCRIPTION, MISSING_ID));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(reportIds(incidentId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT второго отчёта для одного инцидента отклоняется")
    void shouldRejectSecondReportForIncident() {
        long creatorId = insertCreator();
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertReport(incidentId, creatorId);
        ReportRow original = readReport(id);

        Throwable failure = catchThrowable(
                () -> jdbcTemplate.update(INSERT_REPORT, incidentId, UPDATED_DESCRIPTION, creatorId));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(reportIds(incidentId)).containsExactly(id),
                () -> assertThat(readReport(id)).isEqualTo(original));
    }

    private long insertCreator() {
        return insertUser(jdbcTemplate, "db-test-cleanup-report-creator");
    }

    private long insertReport(long incidentId, long creatorId) {
        Long id = jdbcTemplate.queryForObject(INSERT_REPORT + " RETURNING id", Long.class, incidentId, DESCRIPTION,
                creatorId);
        assertThat(id).as("id returned by INSERT INTO cleanup_report").isNotNull();
        return id;
    }

    private ReportRow readReport(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT incident_id, description, attachment_file_ids, created_by_user_id, created_at
                  FROM cleanup_report
                 WHERE id = ?
                """, REPORT_ROW_MAPPER, id);
    }

    private List<Long> reportIds(long incidentId) {
        return jdbcTemplate.queryForList("SELECT id FROM cleanup_report WHERE incident_id = ?", Long.class, incidentId);
    }

    private LocalDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()::timestamp", LocalDateTime.class);
    }

    private record ReportRow(long incidentId, String description, String attachmentFileIds, long creatorId,
                             LocalDateTime createdAt) {
    }
}
