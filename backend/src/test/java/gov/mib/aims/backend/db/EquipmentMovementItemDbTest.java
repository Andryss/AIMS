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

import java.util.List;

import static gov.mib.aims.backend.db.util.IncidentDbFixtures.insertIncident;
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertEquipmentItem;
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertIssueMovement;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.MISSING_ID;
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

@DisplayName("Тестирование таблицы equipment_movement_item")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class EquipmentMovementItemDbTest extends BaseDbTest {

    private static final String TABLE = "equipment_movement_item";
    private static final String INSERT_ITEM = """
            INSERT INTO equipment_movement_item (movement_id, equipment_item_id) VALUES (?, ?)
            """;
    private static final String UPDATE_ITEM = "UPDATE equipment_movement_item SET equipment_item_id = ? WHERE id = ?";
    private static final RowMapper<MovementItemRow> ITEM_MAPPER = (row, index) -> new MovementItemRow(
            row.getLong("id"), row.getLong("movement_id"), row.getLong("equipment_item_id"));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует миграции")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "movement_id", "equipment_item_id");
    }

    @Test
    @DisplayName("[Схема] Колонки имеют ожидаемые типы, допустимость NULL и значения по умолчанию")
    void shouldHaveExpectedColumnProperties() {
        assertAll(TABLE,
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("movement_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("equipment_item_id", BIGINT)));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE задано для пары movement_id и equipment_item_id")
    void shouldHaveUniqueKeyOnMovementAndItem() {
        schema.assertUniqueKeys(TABLE, new Key("movement_id", "equipment_item_id"));
    }

    @Test
    @DisplayName("[Схема] Внешние ключи ссылаются на движение и экземпляр оборудования")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("movement_id", "equipment_movement", ID, "a"),
                new ForeignKey("equipment_item_id", "equipment_item", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индекс задан для поиска по equipment_item_id")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE, new Index(false, List.of("equipment_item_id"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет разные пары с общим движением или экземпляром")
    void shouldPersistDistinctPairs() {
        long userId = insertUser(jdbcTemplate, "db-test-movement-item-user");
        long incidentId = insertIncident(jdbcTemplate, userId);
        long movementId = insertIssueMovement(jdbcTemplate, incidentId, userId);
        long otherMovementId = insertIssueMovement(jdbcTemplate, incidentId, userId);
        long itemId = insertEquipmentItem(jdbcTemplate, "NX-A100");
        long otherItemId = insertEquipmentItem(jdbcTemplate, "NX-A200");

        long id = linkItemToMovement(movementId, itemId);
        long otherItemLinkId = linkItemToMovement(movementId, otherItemId);
        long otherMovementLinkId = linkItemToMovement(otherMovementId, itemId);

        assertThat(id).isPositive();
        assertThat(itemsForMovement(movementId)).containsExactly(
                new MovementItemRow(id, movementId, itemId),
                new MovementItemRow(otherItemLinkId, movementId, otherItemId));
        assertThat(itemsForMovement(otherMovementId)).containsExactly(
                new MovementItemRow(otherMovementLinkId, otherMovementId, itemId));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE заменяет экземпляр в движении")
    void shouldReplaceLinkedItem() {
        long movementId = createMovement();
        long itemId = insertEquipmentItem(jdbcTemplate, "NX-A100");
        long otherItemId = insertEquipmentItem(jdbcTemplate, "NX-A200");
        long id = linkItemToMovement(movementId, itemId);

        int updatedRows = jdbcTemplate.update(UPDATE_ITEM, otherItemId, id);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(itemsForMovement(movementId)).containsExactly(new MovementItemRow(id, movementId, otherItemId));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторной парой движения и экземпляра отклоняется")
    void shouldRejectDuplicateMovementAndItemOnInsert() {
        long movementId = createMovement();
        long itemId = insertEquipmentItem(jdbcTemplate, "NX-A100");
        long id = linkItemToMovement(movementId, itemId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ITEM, movementId, itemId));

        assertAll(INSERT_ITEM,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(itemsForMovement(movementId))
                        .containsExactly(new MovementItemRow(id, movementId, itemId)));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE на существующую пару движения и экземпляра отклоняется")
    void shouldRejectDuplicateMovementAndItemOnUpdate() {
        long movementId = createMovement();
        long itemId = insertEquipmentItem(jdbcTemplate, "NX-A100");
        long otherItemId = insertEquipmentItem(jdbcTemplate, "NX-A200");
        long id = linkItemToMovement(movementId, itemId);
        long otherId = linkItemToMovement(movementId, otherItemId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(UPDATE_ITEM, itemId, otherId));

        assertAll(UPDATE_ITEM,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(itemsForMovement(movementId)).containsExactly(
                        new MovementItemRow(id, movementId, itemId),
                        new MovementItemRow(otherId, movementId, otherItemId)));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в equipment_item_id отклоняется")
    void shouldRejectNullItemId() {
        long movementId = createMovement();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ITEM, movementId, null));

        assertAll(INSERT_ITEM,
                () -> assertNotNullViolation(failure, "equipment_item_id"),
                () -> assertThat(itemsForMovement(movementId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующий экземпляр отклоняется")
    void shouldRejectUnknownEquipmentItem() {
        long movementId = createMovement();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ITEM, movementId, MISSING_ID));

        assertAll(INSERT_ITEM,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(itemsForMovement(movementId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующее движение отклоняется")
    void shouldRejectUnknownMovement() {
        long itemId = insertEquipmentItem(jdbcTemplate, "NX-A100");

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_ITEM, MISSING_ID, itemId));

        assertAll(INSERT_ITEM,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(itemsForMovement(MISSING_ID)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранную связь и сохраняет экземпляр оборудования")
    void shouldDeleteOnlySelectedLink() {
        long movementId = createMovement();
        long itemId = insertEquipmentItem(jdbcTemplate, "NX-A100");
        long otherItemId = insertEquipmentItem(jdbcTemplate, "NX-A200");
        long id = linkItemToMovement(movementId, itemId);
        long otherId = linkItemToMovement(movementId, otherItemId);

        int deletedRows = jdbcTemplate.update("DELETE FROM equipment_movement_item WHERE id = ?", id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(itemsForMovement(movementId)).containsExactly(new MovementItemRow(otherId, movementId, otherItemId));
        assertThat(jdbcTemplate.queryForList("SELECT id FROM equipment_item WHERE id = ?", Long.class, itemId))
                .containsExactly(itemId);
    }

    private long createMovement() {
        long userId = insertUser(jdbcTemplate, "db-test-movement-item-user");
        long incidentId = insertIncident(jdbcTemplate, userId);
        return insertIssueMovement(jdbcTemplate, incidentId, userId);
    }

    private long linkItemToMovement(long movementId, long itemId) {
        Long id = jdbcTemplate.queryForObject(INSERT_ITEM + "RETURNING id", Long.class, movementId, itemId);
        assertThat(id).as("id returned by INSERT INTO equipment_movement_item").isNotNull();
        return id;
    }

    private List<MovementItemRow> itemsForMovement(long movementId) {
        return jdbcTemplate.query("""
                SELECT id, movement_id, equipment_item_id
                  FROM equipment_movement_item WHERE movement_id = ? ORDER BY id
                """, ITEM_MAPPER, movementId);
    }

    private record MovementItemRow(long id, long movementId, long itemId) {
    }
}
