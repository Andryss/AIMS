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

@DisplayName("Тестирование таблицы entity_history")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class EntityHistoryDbTest extends BaseDbTest {

    private static final String TABLE = "entity_history";
    private static final String EDITOR_LOGIN = "db-test-history-editor";
    private static final String ENTITY_TYPE = "INCIDENT";
    private static final long ENTITY_ID = 482_701L;
    private static final String SNAPSHOT = """
            {"status": "NEW", "details": {"threatLevel": 3, "attachmentFileIds": [42, 17]}}
            """;
    private static final String INSERT_HISTORY = """
            INSERT INTO entity_history (entity_type, entity_id, snapshot, changed_by_user_id)
            VALUES (?, ?, ?::jsonb, ?)
            """;

    private static final RowMapper<HistoryRow> HISTORY_ROW_MAPPER = (row, index) -> new HistoryRow(
            row.getString("entity_type"),
            row.getLong("entity_id"),
            row.getString("snapshot"),
            row.getLong("changed_by_user_id"),
            row.getObject("changed_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок entity_history")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "entity_type", "entity_id", "snapshot", "changed_by_user_id", "changed_at");
    }

    @Test
    @DisplayName("[Схема] Типы, допустимость NULL и значения по умолчанию полей истории")
    void shouldHaveExpectedColumnDefinitions() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("entity_type", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("entity_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("snapshot", "jsonb")),
                () -> schema.assertColumn(TABLE, Column.required("changed_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("changed_at", TIMESTAMP_WITH_TIME_ZONE, "now()")));
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
    @DisplayName("[Схема] changed_by_user_id ссылается на app_user.id с ON DELETE NO ACTION")
    void shouldHaveForeignKeyToEditor() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("changed_by_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы по (entity_type, entity_id, changed_at DESC, id DESC) и (entity_type, changed_at)")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(false, List.of("entity_type", "entity_id", "changed_at DESC", "id DESC"), null),
                new Index(false, List.of("entity_type", "changed_at"), null));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет снимок JSONB и заполняет id и changed_at")
    void shouldPersistSnapshotAndGenerateDefaults() {
        long editorId = insertUser(jdbcTemplate, EDITOR_LOGIN);
        OffsetDateTime before = databaseTime();

        long id = insertSnapshot(editorId);
        HistoryRow saved = readHistory(id);
        OffsetDateTime after = databaseTime();
        Boolean snapshotMatches = jdbcTemplate.queryForObject("""
                SELECT snapshot = ?::jsonb FROM entity_history WHERE id = ?
                """, Boolean.class, SNAPSHOT, id);

        assertThat(id).isPositive();
        assertThat(saved.entityType()).isEqualTo(ENTITY_TYPE);
        assertThat(saved.entityId()).isEqualTo(ENTITY_ID);
        assertThat(saved.editorId()).isEqualTo(editorId);
        assertThat(saved.changedAt()).isBetween(before, after);
        assertThat(snapshotMatches).isTrue();
    }

    @Test
    @DisplayName("[Сценарий] SELECT возвращает снимки выбранной сущности в порядке changed_at и id")
    void shouldReadSuccessiveSnapshotsInStableOrder() {
        long editorId = insertUser(jdbcTemplate, EDITOR_LOGIN);
        OffsetDateTime firstChange = OffsetDateTime.parse("2026-02-01T10:00:00Z");
        long recentId = insertSnapshot(ENTITY_TYPE, editorId, firstChange.plusHours(1), """
                {"status": "CLOSED"}
                """);
        long firstId = insertSnapshot(ENTITY_TYPE, editorId, firstChange, SNAPSHOT);
        long secondId = insertSnapshot(ENTITY_TYPE, editorId, firstChange, """
                {"status": "IN_PROGRESS"}
                """);
        insertSnapshot("TENDER", editorId, firstChange, """
                {"status": "OPEN"}
                """);

        List<Long> snapshotIds = jdbcTemplate.queryForList("""
                SELECT id
                  FROM entity_history
                 WHERE entity_type = ? AND entity_id = ?
                 ORDER BY changed_at, id
                """, Long.class, ENTITY_TYPE, ENTITY_ID);
        List<String> statuses = jdbcTemplate.queryForList("""
                SELECT snapshot ->> 'status'
                  FROM entity_history
                 WHERE entity_type = ? AND entity_id = ?
                 ORDER BY changed_at, id
                """, String.class, ENTITY_TYPE, ENTITY_ID);

        assertThat(snapshotIds).containsExactly(firstId, secondId, recentId);
        assertThat(statuses).containsExactly("NEW", "IN_PROGRESS", "CLOSED");
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в snapshot отклоняется")
    void shouldRejectNullSnapshot() {
        long editorId = insertUser(jdbcTemplate, EDITOR_LOGIN);
        long countBefore = countSnapshots();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_HISTORY, ENTITY_TYPE, ENTITY_ID, null, editorId));

        assertAll(
                () -> assertNotNullViolation(failure, "snapshot"),
                () -> assertThat(countSnapshots()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего автора изменения отклоняется")
    void shouldRejectMissingEditor() {
        long countBefore = countSnapshots();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_HISTORY, ENTITY_TYPE, ENTITY_ID, SNAPSHOT, MISSING_ID));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countSnapshots()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateId() {
        long editorId = insertUser(jdbcTemplate, EDITOR_LOGIN);
        long id = insertSnapshot(editorId);
        HistoryRow original = readHistory(id);
        long countBefore = countSnapshots();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO entity_history (id, entity_type, entity_id, snapshot, changed_by_user_id)
                VALUES (?, ?, ?, '{"status": "CLOSED"}'::jsonb, ?)
                """, id, ENTITY_TYPE, ENTITY_ID, editorId));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countSnapshots()).isEqualTo(countBefore),
                () -> assertThat(readHistory(id)).isEqualTo(original));
    }

    private long insertSnapshot(long editorId) {
        return jdbcTemplate.queryForObject(INSERT_HISTORY + "RETURNING id", Long.class,
                ENTITY_TYPE, ENTITY_ID, SNAPSHOT, editorId);
    }

    private long insertSnapshot(String entityType, long editorId, OffsetDateTime changedAt, String snapshot) {
        return jdbcTemplate.queryForObject("""
                INSERT INTO entity_history (entity_type, entity_id, snapshot, changed_by_user_id, changed_at)
                VALUES (?, ?, ?::jsonb, ?, ?)
                RETURNING id
                """, Long.class, entityType, ENTITY_ID, snapshot, editorId, changedAt);
    }

    private HistoryRow readHistory(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT entity_type, entity_id, snapshot, changed_by_user_id, changed_at
                  FROM entity_history
                 WHERE id = ?
                """, HISTORY_ROW_MAPPER, id);
    }

    private long countSnapshots() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM entity_history", Long.class);
    }

    private OffsetDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private record HistoryRow(String entityType, long entityId, String snapshot, long editorId,
                              OffsetDateTime changedAt) {
    }
}
