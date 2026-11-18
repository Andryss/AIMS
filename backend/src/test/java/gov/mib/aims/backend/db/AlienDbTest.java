package gov.mib.aims.backend.db;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.db.util.TableSchemaQueries;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Column;
import gov.mib.aims.backend.db.util.TableSchemaQueries.Key;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;

import static gov.mib.aims.backend.db.util.IncidentDbFixtures.insertIncident;
import static gov.mib.aims.backend.db.util.RbacDbFixtures.insertUser;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.FOREIGN_KEY_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.ID;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.TEXT;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.UNIQUE_VIOLATION;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertNotNullViolation;
import static gov.mib.aims.backend.db.util.TableSchemaQueries.assertSqlViolation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertAll;

@DisplayName("Тестирование таблицы alien")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class AlienDbTest extends BaseDbTest {

    private static final String TABLE = "alien";
    private static final String NAME = "Светящийся слизень";
    private static final String DESCRIPTION = "Замечен в канализации";
    private static final short THREAT_LEVEL = 2;
    private static final String CLASSIFICATION_KEY = "db-test-slug";
    private static final String INSERT_ALIEN = """
            INSERT INTO alien (name, description, threat_level, classification_key)
            VALUES (?, ?, ?, ?)
            """;
    private static final String DELETE_ALIEN = "DELETE FROM alien WHERE id = ?";

    private static final RowMapper<AlienRow> ALIEN_ROW_MAPPER = (row, index) -> new AlienRow(
            row.getString("name"),
            row.getString("description"),
            row.getShort("threat_level"),
            row.getString("classification_key"),
            row.getString("photo_file_ids"),
            row.getObject("created_at", LocalDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок alien")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "name", "description", "threat_level",
                "classification_key", "photo_file_ids", "created_at");
    }

    @Test
    @DisplayName("[Схема] Типы, допустимость NULL и значения по умолчанию полей справочника")
    void shouldHaveExpectedColumnDefinitions() {
        assertAll(
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("name", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("description", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("threat_level", "smallint")),
                () -> schema.assertColumn(TABLE, Column.optional("classification_key", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("photo_file_ids", "jsonb", "'[]'::jsonb")),
                () -> schema.assertColumn(TABLE,
                        Column.required("created_at", "timestamp without time zone", "now()")));
    }

    @Test
    @DisplayName("[Схема] Первичный ключ по id")
    void shouldHavePrimaryKeyOnId() {
        schema.assertPrimaryKeys(TABLE, new Key(ID));
    }

    @Test
    @DisplayName("[Схема] Ограничение UNIQUE по classification_key")
    void shouldHaveUniqueClassificationKey() {
        schema.assertUniqueKeys(TABLE, new Key("classification_key"));
    }

    @Test
    @DisplayName("[Схема] Отсутствуют внешние ключи")
    void shouldHaveNoForeignKeys() {
        schema.assertForeignKeys(TABLE);
    }

    @Test
    @DisplayName("[Схема] Отсутствуют индексы вне ограничений")
    void shouldHaveNoStandaloneIndexes() {
        schema.assertIndexes(TABLE);
    }

    @Test
    @DisplayName("[Сценарий] INSERT допускает несколько NULL в classification_key и заполняет значения по умолчанию")
    void shouldGenerateDefaultsAndAllowMultipleNullClassificationKeys() {
        LocalDateTime before = databaseLocalTime();
        long firstId = jdbcTemplate.queryForObject("""
                INSERT INTO alien (name, description, threat_level)
                VALUES (?, ?, ?)
                RETURNING id
                """, Long.class, NAME, DESCRIPTION, THREAT_LEVEL);
        long secondId = insertAlien(null);
        LocalDateTime after = databaseLocalTime();

        AlienRow first = readAlien(firstId);
        AlienRow second = readAlien(secondId);

        assertThat(firstId).isPositive().isNotEqualTo(secondId);
        assertThat(secondId).isPositive();
        assertThat(first.name()).isEqualTo(NAME);
        assertThat(first.description()).isEqualTo(DESCRIPTION);
        assertThat(first.threatLevel()).isEqualTo(THREAT_LEVEL);
        assertThat(first.classificationKey()).isNull();
        assertThat(first.photoFileIds()).isEqualTo("[]");
        assertThat(second.classificationKey()).isNull();
        assertThat(second.photoFileIds()).isEqualTo("[]");
        assertThat(first.createdAt()).isBetween(before, after);
        assertThat(second.createdAt()).isBetween(before, after);
    }

    @Test
    @DisplayName("[Сценарий] UPDATE сохраняет уровень угрозы и упорядоченный массив числовых идентификаторов фото")
    void shouldUpdateThreatLevelAndPhotoIds() {
        long id = insertAlien(CLASSIFICATION_KEY);
        AlienRow original = readAlien(id);

        short updatedThreatLevel = 6;
        int updatedRows = jdbcTemplate.update("""
                UPDATE alien SET threat_level = ?, photo_file_ids = ?::jsonb WHERE id = ?
                """, updatedThreatLevel, "[42, 17]", id);
        AlienRow updated = readAlien(id);
        Boolean photosMatch = jdbcTemplate.queryForObject("""
                SELECT photo_file_ids = '[42, 17]'::jsonb FROM alien WHERE id = ?
                """, Boolean.class, id);

        assertThat(updatedRows).isEqualTo(1);
        assertThat(updated.threatLevel()).isEqualTo(updatedThreatLevel);
        assertThat(updated.classificationKey()).isEqualTo(CLASSIFICATION_KEY);
        assertThat(photosMatch).isTrue();
        assertThat(updated).usingRecursiveComparison().ignoringFields("threatLevel", "photoFileIds")
                .isEqualTo(original);
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным непустым classification_key отклоняется")
    void shouldRejectDuplicateClassificationKey() {
        long id = insertAlien(CLASSIFICATION_KEY);
        AlienRow original = readAlien(id);
        long countBefore = countAliens();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_ALIEN, "Другой вид", DESCRIPTION, THREAT_LEVEL, CLASSIFICATION_KEY));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countAliens()).isEqualTo(countBefore),
                () -> assertThat(readAlien(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с повторным id отклоняется")
    void shouldRejectDuplicateId() {
        long id = insertAlien(null);
        AlienRow original = readAlien(id);
        long countBefore = countAliens();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update("""
                INSERT INTO alien (id, name, description, threat_level) VALUES (?, ?, ?, ?)
                """, id, "Другой вид", DESCRIPTION, THREAT_LEVEL));

        assertAll(
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(countAliens()).isEqualTo(countBefore),
                () -> assertThat(readAlien(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в name отклоняется")
    void shouldRejectAlienWithoutName() {
        long countBefore = countAliens();

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_ALIEN, null, DESCRIPTION, THREAT_LEVEL, CLASSIFICATION_KEY));

        assertAll(
                () -> assertNotNullViolation(failure, "name"),
                () -> assertThat(countAliens()).isEqualTo(countBefore));
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет запись справочника без ссылок")
    void shouldDeleteUnreferencedAlien() {
        long id = insertAlien(CLASSIFICATION_KEY);
        long countBefore = countAliens();

        int deletedRows = jdbcTemplate.update(DELETE_ALIEN, id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT id FROM alien WHERE id = ?", Long.class, id)).isEmpty();
        assertThat(countAliens()).isEqualTo(countBefore - 1);
    }

    @Test
    @DisplayName("[Сценарий] DELETE отклоняется при наличии ссылки из incident")
    void shouldRejectDeletingAlienReferencedByIncident() {
        long id = insertAlien(CLASSIFICATION_KEY);
        AlienRow original = readAlien(id);
        long creatorId = insertUser(jdbcTemplate, "db-test-alien-incident-creator");
        long incidentId = insertIncident(jdbcTemplate, creatorId);
        jdbcTemplate.update("UPDATE incident SET alien_id = ? WHERE id = ?", id, incidentId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(DELETE_ALIEN, id));

        assertAll(
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(readAlien(id)).isEqualTo(original),
                () -> assertThat(jdbcTemplate.queryForObject(
                        "SELECT alien_id FROM incident WHERE id = ?", Long.class, incidentId)).isEqualTo(id));
    }

    private long insertAlien(String classificationKey) {
        return jdbcTemplate.queryForObject(INSERT_ALIEN + "RETURNING id", Long.class,
                NAME, DESCRIPTION, THREAT_LEVEL, classificationKey);
    }

    private AlienRow readAlien(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT name, description, threat_level, classification_key, photo_file_ids, created_at
                  FROM alien
                 WHERE id = ?
                """, ALIEN_ROW_MAPPER, id);
    }

    private long countAliens() {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM alien", Long.class);
    }

    private LocalDateTime databaseLocalTime() {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()::timestamp", LocalDateTime.class);
    }

    private record AlienRow(String name, String description, short threatLevel, String classificationKey,
                            String photoFileIds, LocalDateTime createdAt) {
    }
}
