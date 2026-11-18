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

import java.time.OffsetDateTime;
import java.util.List;

import static gov.mib.aims.backend.db.util.IncidentDbFixtures.insertIncident;
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

@DisplayName("Тестирование таблицы incident_comment")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class IncidentCommentDbTest extends BaseDbTest {

    private static final String TABLE = "incident_comment";
    private static final String COMMENT = "Проверены записи камер";
    private static final String UPDATED_COMMENT = "Получены записи со всех камер";

    private static final String INSERT_COMMENT = """
            INSERT INTO incident_comment (incident_id, author_user_id, text)
            VALUES (?, ?, ?)
            """;

    private static final RowMapper<CommentRow> COMMENT_ROW_MAPPER = (row, index) -> new CommentRow(
            row.getLong("incident_id"),
            row.getLong("author_user_id"),
            row.getString("text"),
            row.getObject("created_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует схеме")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "incident_id", "author_user_id", "text", "created_at");
    }

    @Test
    @DisplayName("[Схема] Типы, обязательность и значения по умолчанию соответствуют схеме")
    void shouldHaveExpectedColumnProperties() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("incident_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("author_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("text", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")));
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
    @DisplayName("[Схема] Внешние ключи связывают комментарий с инцидентом и автором")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("incident_id", "incident", ID, "a"),
                new ForeignKey("author_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индексы создаются только для ограничений")
    void shouldHaveNoStandaloneIndexes() {
        schema.assertIndexes(TABLE);
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет текст, инцидент и автора, заполняет id и created_at")
    void shouldCreateCommentWithDefaults() {
        long authorId = insertAuthor();
        long incidentId = insertIncident(jdbcTemplate, authorId);
        OffsetDateTime before = databaseTime();

        long id = insertComment(incidentId, authorId, COMMENT);
        CommentRow saved = readComment(id);

        assertThat(id).isPositive();
        assertThat(saved.incidentId()).isEqualTo(incidentId);
        assertThat(saved.authorId()).isEqualTo(authorId);
        assertThat(saved.text()).isEqualTo(COMMENT);
        assertThat(saved.createdAt()).isBetween(before, databaseTime());
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет текст существующего комментария")
    void shouldUpdateCommentText() {
        long authorId = insertAuthor();
        long incidentId = insertIncident(jdbcTemplate, authorId);
        long id = insertComment(incidentId, authorId, COMMENT);
        CommentRow original = readComment(id);

        int updated = jdbcTemplate.update("UPDATE incident_comment SET text = ? WHERE id = ?", UPDATED_COMMENT, id);

        assertThat(updated).isEqualTo(1);
        assertThat(readComment(id)).isEqualTo(
                new CommentRow(incidentId, authorId, UPDATED_COMMENT, original.createdAt()));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранный комментарий и сохраняет второй комментарий инцидента")
    void shouldDeleteOnlySelectedComment() {
        long authorId = insertAuthor();
        long incidentId = insertIncident(jdbcTemplate, authorId);
        long id = insertComment(incidentId, authorId, COMMENT);
        long otherId = insertComment(incidentId, authorId, UPDATED_COMMENT);

        int deleted = jdbcTemplate.update("DELETE FROM incident_comment WHERE id = ?", id);

        assertThat(deleted).isEqualTo(1);
        assertThat(commentIds(incidentId)).containsExactly(otherId);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в text отклоняется")
    void shouldRejectNullText() {
        long authorId = insertAuthor();
        long incidentId = insertIncident(jdbcTemplate, authorId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_COMMENT, incidentId, authorId, null));

        assertAll(
                () -> assertNotNullViolation(failure, "text"),
                () -> assertThat(commentIds(incidentId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующий инцидент отклоняется")
    void shouldRejectMissingIncident() {
        long authorId = insertAuthor();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_COMMENT, MISSING_ID, authorId, COMMENT));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(commentIds(MISSING_ID)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего автора отклоняется")
    void shouldRejectMissingAuthor() {
        long incidentId = insertIncident(jdbcTemplate, insertAuthor());

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_COMMENT, incidentId, MISSING_ID, COMMENT));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(commentIds(incidentId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется и сохраняет исходный комментарий")
    void shouldRejectDuplicateId() {
        long authorId = insertAuthor();
        long incidentId = insertIncident(jdbcTemplate, authorId);
        long id = insertComment(incidentId, authorId, COMMENT);
        CommentRow original = readComment(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO incident_comment (id, incident_id, author_user_id, text)
                VALUES (?, ?, ?, ?)
                """, id, incidentId, authorId, UPDATED_COMMENT));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(commentIds(incidentId)).containsExactly(id),
                () -> assertThat(readComment(id)).isEqualTo(original));
    }

    private long insertAuthor() {
        return insertUser(jdbcTemplate, "db-test-comment-author");
    }

    private long insertComment(long incidentId, long authorId, String text) {
        Long id = jdbcTemplate.queryForObject(INSERT_COMMENT + " RETURNING id", Long.class, incidentId, authorId, text);
        assertThat(id).as("id returned by INSERT INTO incident_comment").isNotNull();
        return id;
    }

    private CommentRow readComment(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT incident_id, author_user_id, text, created_at
                  FROM incident_comment
                 WHERE id = ?
                """, COMMENT_ROW_MAPPER, id);
    }

    private List<Long> commentIds(long incidentId) {
        return jdbcTemplate.queryForList(
                "SELECT id FROM incident_comment WHERE incident_id = ?", Long.class, incidentId);
    }

    private OffsetDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private record CommentRow(long incidentId, long authorId, String text, OffsetDateTime createdAt) {
    }
}
