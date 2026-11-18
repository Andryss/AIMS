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
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertEquipmentItem;
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertIssueMovement;
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertTender;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.MISSING_ID;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertUser;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.BIGINT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TIMESTAMP_WITH_TIME_ZONE;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы equipment_item")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class EquipmentItemDbTest extends BaseDbTest {

    private static final String TABLE = "equipment_item";
    private static final String SERIAL_NUMBER = "NX-A100";

    private static final RowMapper<EquipmentRow> EQUIPMENT_MAPPER = (row, index) -> new EquipmentRow(
            row.getString("type"), row.getString("model"), row.getString("serial_number"), row.getString("status"),
            row.getObject("holder_user_id", Long.class), row.getObject("current_issue_movement_id", Long.class),
            row.getObject("source_tender_id", Long.class), row.getObject("created_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует миграции")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "type", "model", "serial_number", "status", "holder_user_id",
                "current_issue_movement_id", "source_tender_id", "created_at");
    }

    @Test
    @DisplayName("[Схема] Колонки имеют ожидаемые типы, допустимость NULL и значения по умолчанию")
    void shouldHaveExpectedColumnProperties() {
        assertAll(TABLE,
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("type", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("model", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("serial_number", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.optional("holder_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("current_issue_movement_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("source_tender_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")));
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
    @DisplayName("[Схема] Внешние ключи связывают экземпляр оборудования с держателем, выдачей и тендером")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("holder_user_id", "app_user", ID, "a"),
                new ForeignKey("current_issue_movement_id", "equipment_movement", ID, "a"),
                new ForeignKey("source_tender_id", "tender", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы включают уникальный lower(serial_number) и колонки выдачи и тендера")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(true, List.of("lower(serial_number)"), null),
                new Index(false, List.of("current_issue_movement_id"), null),
                new Index(false, List.of("source_tender_id"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет экземпляр без дополнительных ссылок и заполняет created_at")
    void shouldPersistItemWithDefaults() {
        OffsetDateTime before = databaseTime(jdbcTemplate);

        long id = insertEquipmentItem(jdbcTemplate, SERIAL_NUMBER);
        EquipmentRow saved = readItem(id);
        OffsetDateTime after = databaseTime(jdbcTemplate);

        assertThat(id).isPositive();
        assertThat(saved.createdAt()).isBetween(before, after);
        assertThat(saved).isEqualTo(new EquipmentRow("NEURALYZER", "NX-1", SERIAL_NUMBER, "AVAILABLE", null,
                null, null, saved.createdAt()));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE сохраняет держателя и текущую выдачу экземпляра, закупленного по тендеру")
    void shouldAssignItemToHolderAndIssueMovement() {
        long userId = insertUser(jdbcTemplate, "db-test-equipment-user");
        long tenderId = insertTender(jdbcTemplate, userId);
        long incidentId = insertIncident(jdbcTemplate, userId);
        long issueId = insertIssueMovement(jdbcTemplate, incidentId, userId);
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO equipment_item (type, model, serial_number, status, source_tender_id)
                VALUES ('NEURALYZER', 'NX-1', ?, 'AVAILABLE', ?) RETURNING id
                """, Long.class, SERIAL_NUMBER, tenderId);
        EquipmentRow original = readItem(id);

        int updatedRows = jdbcTemplate.update("""
                UPDATE equipment_item SET status = 'ISSUED', holder_user_id = ?, current_issue_movement_id = ?
                 WHERE id = ?
                """, userId, issueId, id);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(readItem(id)).isEqualTo(new EquipmentRow("NEURALYZER", "NX-1", SERIAL_NUMBER, "ISSUED", userId,
                issueId, tenderId, original.createdAt()));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в serial_number отклоняется")
    void shouldRejectMissingSerialNumber() {
        long countBefore = countItems();

        Throwable failure = catchThrowable(() -> insertEquipmentItem(jdbcTemplate, null));

        assertAll(TABLE,
                () -> assertNotNullViolation(failure, "serial_number"),
                () -> assertThat(countItems()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE со ссылкой на несуществующую выдачу отклоняется")
    void shouldRejectUnknownIssueMovement() {
        long id = insertEquipmentItem(jdbcTemplate, SERIAL_NUMBER);
        EquipmentRow original = readItem(id);
        String update = "UPDATE equipment_item SET current_issue_movement_id = ? WHERE id = ?";

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(update, MISSING_ID, id));

        assertAll(update,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readItem(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT номера, отличающегося только регистром, отклоняется")
    void shouldRejectSerialNumberWithDifferentCaseOnInsert() {
        long id = insertEquipmentItem(jdbcTemplate, SERIAL_NUMBER);
        EquipmentRow original = readItem(id);
        long countBefore = countItems();

        Throwable failure = catchThrowable(() -> insertEquipmentItem(jdbcTemplate, "nx-a100"));

        assertAll(TABLE,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countItems()).isEqualTo(countBefore),
                () -> assertThat(readItem(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE на занятый серийный номер в другом регистре отклоняется")
    void shouldRejectSerialNumberWithDifferentCaseOnUpdate() {
        long id = insertEquipmentItem(jdbcTemplate, SERIAL_NUMBER);
        long otherId = insertEquipmentItem(jdbcTemplate, "NX-A200");
        EquipmentRow original = readItem(id);
        EquipmentRow otherOriginal = readItem(otherId);
        String update = "UPDATE equipment_item SET serial_number = ? WHERE id = ?";

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(update, "Nx-A100", otherId));

        assertAll(update,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(readItem(id)).isEqualTo(original),
                () -> assertThat(readItem(otherId)).isEqualTo(otherOriginal));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранный экземпляр оборудования")
    void shouldDeleteSelectedItem() {
        long id = insertEquipmentItem(jdbcTemplate, SERIAL_NUMBER);
        long otherId = insertEquipmentItem(jdbcTemplate, "NX-A200");

        int deletedRows = jdbcTemplate.update("DELETE FROM equipment_item WHERE id = ?", id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT id FROM equipment_item WHERE id IN (?, ?)", Long.class,
                id, otherId)).containsExactly(otherId);
    }

    private EquipmentRow readItem(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT type, model, serial_number, status, holder_user_id, current_issue_movement_id,
                       source_tender_id, created_at
                  FROM equipment_item WHERE id = ?
                """, EQUIPMENT_MAPPER, id);
    }

    private long countItems() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM equipment_item", Long.class);
    }

    private record EquipmentRow(String type, String model, String serialNumber, String status, Long holderUserId,
                                Long currentIssueMovementId, Long sourceTenderId, OffsetDateTime createdAt) {
    }
}
