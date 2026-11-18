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

@DisplayName("Тестирование таблицы notification")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class NotificationDbTest extends BaseDbTest {

    private static final String TABLE = "notification";
    private static final String RECIPIENT_LOGIN = "db-test-notification-recipient";
    private static final String MESSAGE = "Изменился статус инцидента";
    private static final String INSERT_NOTIFICATION = """
            INSERT INTO notification (recipient_user_id, message) VALUES (?, ?)
            """;

    private static final RowMapper<NotificationRow> NOTIFICATION_ROW_MAPPER = (row, index) -> new NotificationRow(
            row.getLong("recipient_user_id"),
            row.getString("message"),
            row.getString("related_entities"),
            row.getObject("read_at", OffsetDateTime.class),
            row.getObject("created_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок notification")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "recipient_user_id", "message", "related_entities", "read_at", "created_at");
    }

    @Test
    @DisplayName("[Схема] Типы, допустимость NULL и значения по умолчанию полей уведомления")
    void shouldHaveExpectedColumnDefinitions() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("recipient_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("message", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("related_entities", "jsonb", "'[]'::jsonb")),
                () -> schema.assertColumn(TABLE, Column.optional("read_at", TIMESTAMP_WITH_TIME_ZONE)),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")));
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
    @DisplayName("[Схема] Отсутствуют индексы вне ограничений")
    void shouldHaveNoStandaloneIndexes() {
        schema.assertIndexes(TABLE);
    }

    @Test
    @DisplayName("[Схема] recipient_user_id ссылается на app_user.id с ON DELETE NO ACTION")
    void shouldHaveForeignKeyToRecipient() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("recipient_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет непрочитанное уведомление с пустым массивом ссылок")
    void shouldPersistUnreadNotificationWithDefaults() {
        long recipientId = insertUser(jdbcTemplate, RECIPIENT_LOGIN);
        OffsetDateTime before = databaseTime();

        long id = insertNotification(recipientId);
        NotificationRow saved = readNotification(id);
        OffsetDateTime after = databaseTime();

        assertThat(id).isPositive();
        assertThat(saved.recipientId()).isEqualTo(recipientId);
        assertThat(saved.message()).isEqualTo(MESSAGE);
        assertThat(saved.relatedEntities()).isEqualTo("[]");
        assertThat(saved.readAt()).isNull();
        assertThat(saved.createdAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет связанные сущности как упорядоченный массив строк JSONB")
    void shouldStoreRelatedEntitiesAsJsonArray() {
        long recipientId = insertUser(jdbcTemplate, RECIPIENT_LOGIN);
        String relatedEntities = """
                ["INCIDENT:612", "DETENTION_REQUEST:27"]
                """;

        long id = jdbcTemplate.queryForObject("""
                INSERT INTO notification (recipient_user_id, message, related_entities)
                VALUES (?, ?, ?::jsonb)
                RETURNING id
                """, Long.class, recipientId, MESSAGE, relatedEntities);
        Boolean relatedEntitiesMatch = jdbcTemplate.queryForObject("""
                SELECT related_entities = ?::jsonb FROM notification WHERE id = ?
                """, Boolean.class, relatedEntities, id);

        assertThat(relatedEntitiesMatch).isTrue();
    }

    @Test
    @DisplayName("[Сценарий] UPDATE заполняет read_at и сохраняет остальные поля уведомления")
    void shouldMarkNotificationAsRead() {
        long recipientId = insertUser(jdbcTemplate, RECIPIENT_LOGIN);
        long id = insertNotification(recipientId);
        NotificationRow original = readNotification(id);
        OffsetDateTime readAt = original.createdAt().plusMinutes(5);

        int updatedRows = jdbcTemplate.update("UPDATE notification SET read_at = ? WHERE id = ?", readAt, id);
        NotificationRow updated = readNotification(id);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(updated.readAt()).isEqualTo(readAt);
        assertThat(updated).usingRecursiveComparison().ignoringFields("readAt").isEqualTo(original);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в related_entities отклоняется")
    void shouldRejectNullRelatedEntities() {
        long recipientId = insertUser(jdbcTemplate, RECIPIENT_LOGIN);
        long countBefore = countNotifications();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO notification (recipient_user_id, message, related_entities) VALUES (?, ?, NULL)
                """, recipientId, MESSAGE));

        assertAll(
                () -> assertNotNullViolation(failure, "related_entities"),
                () -> assertThat(countNotifications()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего получателя отклоняется")
    void shouldRejectMissingRecipient() {
        long countBefore = countNotifications();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(INSERT_NOTIFICATION, MISSING_ID, MESSAGE));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countNotifications()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateId() {
        long recipientId = insertUser(jdbcTemplate, RECIPIENT_LOGIN);
        long id = insertNotification(recipientId);
        NotificationRow original = readNotification(id);
        long countBefore = countNotifications();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO notification (id, recipient_user_id, message) VALUES (?, ?, ?)
                """, id, recipientId, "Другое уведомление"));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countNotifications()).isEqualTo(countBefore),
                () -> assertThat(readNotification(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранное уведомление и сохраняет остальные уведомления получателя")
    void shouldDeleteOnlySelectedNotification() {
        long recipientId = insertUser(jdbcTemplate, RECIPIENT_LOGIN);
        long id = insertNotification(recipientId);
        long otherId = insertNotification(recipientId);
        NotificationRow originalOther = readNotification(otherId);

        int deletedRows = jdbcTemplate.update("DELETE FROM notification WHERE id = ?", id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList(
                "SELECT id FROM notification WHERE recipient_user_id = ?", Long.class, recipientId))
                .containsExactly(otherId);
        assertThat(readNotification(otherId)).isEqualTo(originalOther);
    }

    private long insertNotification(long recipientId) {
        return jdbcTemplate.queryForObject(INSERT_NOTIFICATION + "RETURNING id", Long.class, recipientId, MESSAGE);
    }

    private NotificationRow readNotification(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT recipient_user_id, message, related_entities, read_at, created_at
                  FROM notification
                 WHERE id = ?
                """, NOTIFICATION_ROW_MAPPER, id);
    }

    private long countNotifications() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM notification", Long.class);
    }

    private OffsetDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private record NotificationRow(long recipientId, String message, String relatedEntities, OffsetDateTime readAt,
                                   OffsetDateTime createdAt) {
    }
}
