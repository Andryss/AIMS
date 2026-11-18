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
import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.insertIssueMovement;
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

@DisplayName("Тестирование таблицы equipment_movement")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class EquipmentMovementDbTest extends BaseDbTest {

    private static final String TABLE = "equipment_movement";
    private static final String INSERT_MOVEMENT = """
            INSERT INTO equipment_movement (operation, status, incident_id, responsible_user_id,
                                            source_issue_movement_id, created_by_user_id, comment)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String DELETE_MOVEMENT = "DELETE FROM equipment_movement WHERE id = ?";

    private static final RowMapper<MovementRow> MOVEMENT_MAPPER = (row, index) -> new MovementRow(
            row.getString("operation"), row.getString("status"), row.getLong("incident_id"),
            row.getLong("responsible_user_id"), row.getObject("source_issue_movement_id", Long.class),
            row.getLong("created_by_user_id"), row.getString("comment"),
            row.getObject("created_at", OffsetDateTime.class), row.getObject("completed_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует миграции")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "operation", "status", "incident_id", "responsible_user_id",
                "source_issue_movement_id", "created_by_user_id", "comment", "created_at", "completed_at");
    }

    @Test
    @DisplayName("[Схема] Колонки имеют ожидаемые типы, допустимость NULL и значения по умолчанию")
    void shouldHaveExpectedColumnProperties() {
        assertAll(TABLE,
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("operation", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("incident_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("responsible_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("source_issue_movement_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("created_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.optional("comment", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")),
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
    @DisplayName("[Схема] Внешние ключи связывают движение с инцидентом, пользователями и исходной выдачей")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("incident_id", "incident", ID, "a"),
                new ForeignKey("responsible_user_id", "app_user", ID, "a"),
                new ForeignKey("source_issue_movement_id", TABLE, ID, "a"),
                new ForeignKey("created_by_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы заданы для поиска по инциденту и статусу и для сортировки по дате")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(false, List.of("incident_id", "status"), null),
                new Index(false, List.of("created_at DESC", "id DESC"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет выдачу без исходного движения и заполняет created_at")
    void shouldPersistIssueWithDefaults() {
        long creatorId = insertUser(jdbcTemplate, "db-test-movement-author");
        long responsibleId = insertUser(jdbcTemplate, "db-test-movement-responsible");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        OffsetDateTime before = databaseTime(jdbcTemplate);

        Long id = jdbcTemplate.queryForObject(INSERT_MOVEMENT + "RETURNING id", Long.class,
                "ISSUE", "PENDING", incidentId, responsibleId, null, creatorId, null);
        MovementRow saved = readMovement(id);
        OffsetDateTime after = databaseTime(jdbcTemplate);

        assertThat(id).isPositive();
        assertThat(saved.createdAt()).isBetween(before, after);
        assertThat(saved).isEqualTo(new MovementRow("ISSUE", "PENDING", incidentId, responsibleId, null,
                creatorId, null, saved.createdAt(), null));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE завершает возврат и сохраняет ссылку на исходную выдачу")
    void shouldCompleteReturnLinkedToIssue() {
        long creatorId = insertUser(jdbcTemplate, "db-test-movement-author");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long issueId = insertIssueMovement(jdbcTemplate, incidentId, creatorId);
        long returnId = insertReturnMovement(incidentId, issueId, creatorId);
        MovementRow original = readMovement(returnId);
        OffsetDateTime completedAt = databaseTime(jdbcTemplate);

        int updatedRows = jdbcTemplate.update("""
                UPDATE equipment_movement SET status = 'COMPLETED', completed_at = ? WHERE id = ?
                """, completedAt, returnId);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(readMovement(returnId)).isEqualTo(new MovementRow("RETURN", "COMPLETED", incidentId, creatorId,
                issueId, creatorId, "Возврат на склад", original.createdAt(), completedAt));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в status отклоняется")
    void shouldRejectMissingStatus() {
        long creatorId = insertUser(jdbcTemplate, "db-test-movement-author");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long countBefore = countMovements();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_MOVEMENT,
                "ISSUE", null, incidentId, creatorId, null, creatorId, null));

        assertAll(INSERT_MOVEMENT,
                () -> assertNotNullViolation(failure, "status"),
                () -> assertThat(countMovements()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT возврата со ссылкой на несуществующую выдачу отклоняется")
    void shouldRejectUnknownSourceIssue() {
        long creatorId = insertUser(jdbcTemplate, "db-test-movement-author");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long countBefore = countMovements();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_MOVEMENT,
                "RETURN", "PENDING", incidentId, creatorId, MISSING_ID, creatorId, null));

        assertAll(INSERT_MOVEMENT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countMovements()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateId() {
        long creatorId = insertUser(jdbcTemplate, "db-test-movement-author");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long id = insertIssueMovement(jdbcTemplate, incidentId, creatorId);
        MovementRow original = readMovement(id);
        long countBefore = countMovements();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO equipment_movement (id, operation, status, incident_id, responsible_user_id,
                                                created_by_user_id)
                VALUES (?, 'ISSUE', 'PENDING', ?, ?, ?)
                """, id, incidentId, creatorId, creatorId));

        assertAll(TABLE,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countMovements()).isEqualTo(countBefore),
                () -> assertThat(readMovement(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] DELETE исходной выдачи возможен после удаления связанного возврата")
    void shouldDeleteSourceIssueAfterDeletingReturn() {
        long creatorId = insertUser(jdbcTemplate, "db-test-movement-author");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        long issueId = insertIssueMovement(jdbcTemplate, incidentId, creatorId);
        long returnId = insertReturnMovement(incidentId, issueId, creatorId);
        MovementRow originalIssue = readMovement(issueId);
        MovementRow originalReturn = readMovement(returnId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(DELETE_MOVEMENT, issueId));

        assertAll(DELETE_MOVEMENT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readMovement(issueId)).isEqualTo(originalIssue),
                () -> assertThat(readMovement(returnId)).isEqualTo(originalReturn));

        assertThat(jdbcTemplate.update(DELETE_MOVEMENT, returnId)).isEqualTo(1);
        assertThat(jdbcTemplate.update(DELETE_MOVEMENT, issueId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT id FROM equipment_movement WHERE incident_id = ?", Long.class,
                incidentId)).isEmpty();
    }

    private long insertReturnMovement(long incidentId, long issueId, long creatorId) {
        Long id = jdbcTemplate.queryForObject(INSERT_MOVEMENT + "RETURNING id", Long.class, "RETURN", "PENDING",
                incidentId, creatorId, issueId, creatorId, "Возврат на склад");
        assertThat(id).as("id returned by INSERT INTO equipment_movement").isNotNull();
        return id;
    }

    private MovementRow readMovement(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT operation, status, incident_id, responsible_user_id, source_issue_movement_id,
                       created_by_user_id, comment, created_at, completed_at
                  FROM equipment_movement WHERE id = ?
                """, MOVEMENT_MAPPER, id);
    }

    private long countMovements() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM equipment_movement", Long.class);
    }

    private record MovementRow(String operation, String status, long incidentId, long responsibleUserId,
                               Long sourceIssueId, long creatorId, String comment, OffsetDateTime createdAt,
                               OffsetDateTime completedAt) {
    }
}
