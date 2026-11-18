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

@DisplayName("Тестирование таблицы stored_file")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class StoredFileDbTest extends BaseDbTest {

    private static final String TABLE = "stored_file";
    private static final String STORAGE_ID = "storage_id";
    private static final String FILE_NAME = "file_name";
    private static final String CONTENT_TYPE = "content_type";
    private static final String FILE_SIZE = "file_size";
    private static final String CREATED_AT = "created_at";
    private static final String CREATED_BY_USER_ID = "created_by_user_id";

    private static final RowMapper<FileRow> FILE_ROW_MAPPER = (row, index) -> new FileRow(
            row.getString(STORAGE_ID),
            row.getString(FILE_NAME),
            row.getString(CONTENT_TYPE),
            row.getLong(FILE_SIZE),
            row.getObject(CREATED_AT, OffsetDateTime.class),
            row.getObject(CREATED_BY_USER_ID, Long.class));

    private static final String TEST_STORAGE_ID = "db-test-storage-id";
    private static final String TEST_FILE_NAME = "incident.txt";
    private static final String UPDATED_FILE_NAME = "incident-updated.txt";
    private static final String TEST_CONTENT_TYPE = "text/plain";
    private static final long TEST_FILE_SIZE = 1024L;
    private static final long MISSING_USER_ID = Long.MIN_VALUE;

    private static final String INSERT_FILE = """
            INSERT INTO stored_file (storage_id, file_name, content_type, file_size, created_by_user_id)
            VALUES (?, ?, ?, ?, ?)
            """;

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок stored_file соответствует схеме")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, STORAGE_ID, FILE_NAME, CONTENT_TYPE, FILE_SIZE, CREATED_AT, CREATED_BY_USER_ID);
    }

    @Test
    @DisplayName("[Схема] id имеет свойства BIGSERIAL и привязанную последовательность")
    void shouldHaveBigSerialId() {
        schema.assertColumn(TABLE, Column.bigSerialId(TABLE));
    }

    @Test
    @DisplayName("[Схема] storage_id объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredStorageIdColumn() {
        schema.assertColumn(TABLE, Column.required(STORAGE_ID, TEXT));
    }

    @Test
    @DisplayName("[Схема] file_name объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredFileNameColumn() {
        schema.assertColumn(TABLE, Column.required(FILE_NAME, TEXT));
    }

    @Test
    @DisplayName("[Схема] content_type объявлен как TEXT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredContentTypeColumn() {
        schema.assertColumn(TABLE, Column.required(CONTENT_TYPE, TEXT));
    }

    @Test
    @DisplayName("[Схема] file_size объявлен как BIGINT NOT NULL без значения по умолчанию")
    void shouldHaveRequiredFileSizeColumn() {
        schema.assertColumn(TABLE, Column.required(FILE_SIZE, BIGINT));
    }

    @Test
    @DisplayName("[Схема] created_at объявлен как TIMESTAMPTZ NOT NULL DEFAULT now()")
    void shouldDefaultCreatedAtToNow() {
        schema.assertColumn(TABLE, Column.required(CREATED_AT, TIMESTAMP_WITH_TIME_ZONE, "now()"));
    }

    @Test
    @DisplayName("[Схема] created_by_user_id имеет тип BIGINT и допускает NULL")
    void shouldHaveOptionalUploaderColumn() {
        schema.assertColumn(TABLE, Column.optional(CREATED_BY_USER_ID, BIGINT));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ состоит из id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE задано для storage_id")
    void shouldHaveUniqueKeyOnStorageId() {
        schema.assertUniqueKeys(TABLE, new Key(STORAGE_ID));
    }

    @Test
    @DisplayName("[Схема] created_by_user_id ссылается на app_user.id с ON DELETE NO ACTION")
    void shouldHaveForeignKeyFromUploaderToAppUser() {
        // PostgreSQL stores ON DELETE NO ACTION as 'a'.
        schema.assertForeignKeys(TABLE,
                new ForeignKey(CREATED_BY_USER_ID, "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Сценарий] INSERT без загрузившего пользователя сохраняет файл и заполняет created_at")
    void shouldPersistFileWithoutUploaderAndSetCreationTime() {
        OffsetDateTime before = databaseTime();

        long id = insertFile(null);
        FileRow saved = readFile(id);
        OffsetDateTime after = databaseTime();

        assertThat(id).as("generated stored_file.id").isPositive();
        assertThat(saved.storageId()).isEqualTo(TEST_STORAGE_ID);
        assertThat(saved.fileName()).isEqualTo(TEST_FILE_NAME);
        assertThat(saved.contentType()).isEqualTo(TEST_CONTENT_TYPE);
        assertThat(saved.fileSize()).isEqualTo(TEST_FILE_SIZE);
        assertThat(saved.createdByUserId()).isNull();
        assertThat(saved.createdAt()).as("generated stored_file.created_at").isBetween(before, after);
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет ссылку на существующего пользователя")
    void shouldPersistFileWithExistingUploader() {
        Long uploaderId = jdbcTemplate.queryForObject("""
                INSERT INTO app_user (login, password_hash) VALUES (?, ?) RETURNING id
                """, Long.class, "db-test-uploader", "uploader-hash");

        long id = insertFile(uploaderId);
        FileRow saved = readFile(id);

        assertThat(saved.createdByUserId()).as("stored_file.created_by_user_id").isEqualTo(uploaderId);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE меняет file_name и сохраняет остальные метаданные")
    void shouldUpdateFileNameAndPreserveOtherMetadata() {
        long id = insertFile(null);
        FileRow original = readFile(id);

        int updatedRows = jdbcTemplate.update("UPDATE stored_file SET file_name = ? WHERE id = ?", UPDATED_FILE_NAME, id);
        FileRow saved = readFile(id);

        assertThat(updatedRows).as("number of stored_file rows updated").isEqualTo(1);
        assertThat(saved.fileName()).as("updated name").isEqualTo(UPDATED_FILE_NAME);
        assertThat(saved).usingRecursiveComparison().ignoringFields("fileName").isEqualTo(original);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в file_name отклоняется")
    void shouldRejectFileWithoutName() {
        long countBefore = countFiles();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_FILE, TEST_STORAGE_ID, null, TEST_CONTENT_TYPE, TEST_FILE_SIZE, null));

        assertAll(INSERT_FILE,
                () -> assertNotNullViolation(failure, FILE_NAME),
                () -> assertThat(countFiles()).as("stored_file count after INSERT").isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным storage_id отклоняется")
    void shouldRejectDuplicateStorageId() {
        long id = insertFile(null);
        FileRow original = readFile(id);
        long countBefore = countFiles();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_FILE, TEST_STORAGE_ID, UPDATED_FILE_NAME, TEST_CONTENT_TYPE, TEST_FILE_SIZE, null));

        assertAll(INSERT_FILE,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countFiles()).as("stored_file count after INSERT").isEqualTo(countBefore),
                () -> assertThat(readFile(id)).as("original file after INSERT").isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующего пользователя отклоняется")
    void shouldRejectFileWithMissingUploader() {
        long countBefore = countFiles();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_FILE, TEST_STORAGE_ID, TEST_FILE_NAME, TEST_CONTENT_TYPE, TEST_FILE_SIZE, MISSING_USER_ID));

        assertAll(INSERT_FILE,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(countFiles()).as("stored_file count after INSERT").isEqualTo(countBefore));
    }

    private long insertFile(Long uploaderId) {
        Long id = jdbcTemplate.queryForObject(INSERT_FILE + "RETURNING id",
                Long.class, TEST_STORAGE_ID, TEST_FILE_NAME, TEST_CONTENT_TYPE, TEST_FILE_SIZE, uploaderId);
        assertThat(id).as("id returned by INSERT INTO stored_file").isNotNull();
        return id;
    }

    private FileRow readFile(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT storage_id, file_name, content_type, file_size, created_at, created_by_user_id
                  FROM stored_file
                 WHERE id = ?
                """, FILE_ROW_MAPPER, id);
    }

    private long countFiles() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM stored_file", Long.class);
    }

    private OffsetDateTime databaseTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }

    private record FileRow(String storageId, String fileName, String contentType, long fileSize,
                           OffsetDateTime createdAt, Long createdByUserId) {
    }
}
