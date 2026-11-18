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
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.databaseTime;
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertTender;
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

@DisplayName("Тестирование таблицы tender")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TenderDbTest extends BaseDbTest {

    private static final String TABLE = "tender";
    private static final String INSERT_TENDER = """
            INSERT INTO tender (equipment_type, quantity, conditions, incident_id, status, created_by_user_id)
            VALUES ('NEURALYZER', ?, ?, ?, 'NEED', ?)
            """;

    private static final RowMapper<TenderRow> TENDER_MAPPER = (row, index) -> new TenderRow(
            row.getString("equipment_type"), row.getInt("quantity"), row.getString("conditions"),
            row.getObject("incident_id", Long.class), row.getString("status"), row.getLong("created_by_user_id"),
            row.getObject("created_at", OffsetDateTime.class), row.getObject("updated_at", OffsetDateTime.class),
            row.getObject("completed_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует миграции")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "equipment_type", "quantity", "conditions", "incident_id", "status",
                "created_by_user_id", "created_at", "updated_at", "completed_at");
    }

    @Test
    @DisplayName("[Схема] Колонки имеют ожидаемые типы, допустимость NULL и значения по умолчанию")
    void shouldHaveExpectedColumnProperties() {
        assertAll(TABLE,
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("equipment_type", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("quantity", INTEGER)),
                () -> schema.assertColumn(TABLE, Column.optional("conditions", TEXT)),
                () -> schema.assertColumn(TABLE, Column.optional("incident_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("created_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")),
                () -> schema.assertColumn(TABLE, Column.required("updated_at", TIMESTAMP_WITH_TIME_ZONE, "now()")),
                () -> schema.assertColumn(TABLE, Column.optional("completed_at", TIMESTAMP_WITH_TIME_ZONE)));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Дополнительные ограничения UNIQUE отсутствуют")
    void shouldHaveNoAdditionalUniqueKeys() {
        schema.assertUniqueKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] Внешние ключи ссылаются на инцидент и создателя тендера")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("incident_id", "incident", ID, "a"),
                new ForeignKey("created_by_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы заданы для инцидента и сортировки по времени создания")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(false, List.of("incident_id"), null),
                new Index(false, List.of("created_at DESC", "id DESC"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет закупку без инцидента и заполняет служебные даты")
    void shouldPersistTenderWithDefaults() {
        long creatorId = insertUser(jdbcTemplate, "db-test-tender-author");
        OffsetDateTime before = databaseTime(jdbcTemplate);

        long id = insertTender(jdbcTemplate, creatorId);
        TenderRow saved = readTender(id);
        OffsetDateTime after = databaseTime(jdbcTemplate);

        assertThat(id).isPositive();
        assertThat(saved.createdAt()).isBetween(before, after);
        assertThat(saved).isEqualTo(new TenderRow("NEURALYZER", 2, null, null, "NEED", creatorId,
                saved.createdAt(), saved.createdAt(), null));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE завершает закупку и сохраняет её связь с инцидентом")
    void shouldCompleteTenderLinkedToIncident() {
        long creatorId = insertUser(jdbcTemplate, "db-test-tender-author");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        Long id = jdbcTemplate.queryForObject(INSERT_TENDER + "RETURNING id", Long.class,
                4, "Доставка на склад", incidentId, creatorId);
        TenderRow original = readTender(id);
        OffsetDateTime completedAt = databaseTime(jdbcTemplate);

        int updatedRows = jdbcTemplate.update("""
                UPDATE tender SET status = 'COMPLETED', updated_at = ?, completed_at = ? WHERE id = ?
                """, completedAt, completedAt, id);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(readTender(id)).isEqualTo(new TenderRow("NEURALYZER", 4, "Доставка на склад", incidentId,
                "COMPLETED", creatorId, original.createdAt(), completedAt, completedAt));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в quantity отклоняется")
    void shouldRejectMissingQuantity() {
        long creatorId = insertUser(jdbcTemplate, "db-test-tender-author");
        long countBefore = countTenders();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_TENDER, null, null, null, creatorId));

        assertAll(INSERT_TENDER,
                () -> assertNotNullViolation(failure, "quantity"),
                () -> assertThat(countTenders()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующий инцидент отклоняется")
    void shouldRejectUnknownIncident() {
        long creatorId = insertUser(jdbcTemplate, "db-test-tender-author");
        long countBefore = countTenders();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_TENDER, 2, null, MISSING_ID, creatorId));

        assertAll(INSERT_TENDER,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countTenders()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateId() {
        long creatorId = insertUser(jdbcTemplate, "db-test-tender-author");
        long id = insertTender(jdbcTemplate, creatorId);
        TenderRow original = readTender(id);
        long countBefore = countTenders();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO tender (id, equipment_type, quantity, status, created_by_user_id)
                VALUES (?, 'NEUTRALIZER', 5, 'NEED', ?)
                """, id, creatorId));

        assertAll(TABLE,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countTenders()).isEqualTo(countBefore),
                () -> assertThat(readTender(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранную закупку")
    void shouldDeleteSelectedTender() {
        long creatorId = insertUser(jdbcTemplate, "db-test-tender-author");
        long id = insertTender(jdbcTemplate, creatorId);
        long otherId = insertTender(jdbcTemplate, creatorId);

        int deletedRows = jdbcTemplate.update("DELETE FROM tender WHERE id = ?", id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT id FROM tender WHERE created_by_user_id = ?", Long.class,
                creatorId)).containsExactly(otherId);
    }

    private TenderRow readTender(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT equipment_type, quantity, conditions, incident_id, status, created_by_user_id,
                       created_at, updated_at, completed_at
                  FROM tender WHERE id = ?
                """, TENDER_MAPPER, id);
    }

    private long countTenders() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM tender", Long.class);
    }

    private record TenderRow(String equipmentType, int quantity, String conditions, Long incidentId, String status,
                             long creatorId, OffsetDateTime createdAt, OffsetDateTime updatedAt,
                             OffsetDateTime completedAt) {
    }
}
