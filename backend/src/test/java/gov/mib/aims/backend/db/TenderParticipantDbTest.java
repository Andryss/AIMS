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

import static gov.mib.aims.backend.db.util.ProcurementDbFixtures.databaseTime;
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

@DisplayName("Тестирование таблицы tender_participant")
@TestMethodOrder(MethodOrderer.DisplayName.class)
class TenderParticipantDbTest extends BaseDbTest {

    private static final String TABLE = "tender_participant";
    private static final String INSERT_PARTICIPANT = """
            INSERT INTO tender_participant (tender_id, name, proposal, status, created_by_user_id)
            VALUES (?, ?, ?, ?, ?)
            """;
    private static final String MARK_WINNER = """
            UPDATE tender_participant SET status = 'WINNER', updated_at = ? WHERE id = ?
            """;

    private static final RowMapper<ParticipantRow> PARTICIPANT_MAPPER = (row, index) -> new ParticipantRow(
            row.getLong("tender_id"), row.getString("name"), row.getString("proposal"), row.getString("status"),
            row.getLong("created_by_user_id"), row.getObject("created_at", OffsetDateTime.class),
            row.getObject("updated_at", OffsetDateTime.class));

    private TableSchemaQueries schema;

    @BeforeEach
    void setUpSchemaQueries() {
        schema = new TableSchemaQueries(jdbcTemplate);
    }

    @Test
    @DisplayName("[Схема] Набор колонок соответствует миграции")
    void shouldHaveExpectedColumns() {
        schema.assertColumns(TABLE, ID, "tender_id", "name", "proposal", "status", "created_by_user_id",
                "created_at", "updated_at");
    }

    @Test
    @DisplayName("[Схема] Колонки имеют ожидаемые типы, допустимость NULL и значения по умолчанию")
    void shouldHaveExpectedColumnProperties() {
        assertAll(TABLE,
                () -> schema.assertColumn(TABLE, Column.bigSerialId(TABLE)),
                () -> schema.assertColumn(TABLE, Column.required("tender_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("name", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("proposal", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("status", TEXT)),
                () -> schema.assertColumn(TABLE, Column.required("created_by_user_id", BIGINT)),
                () -> schema.assertColumn(TABLE, Column.required("created_at", TIMESTAMP_WITH_TIME_ZONE, "now()")),
                () -> schema.assertColumn(TABLE, Column.required("updated_at", TIMESTAMP_WITH_TIME_ZONE, "now()")));
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
    @DisplayName("[Схема] Внешние ключи ссылаются на тендер и создателя предложения")
    void shouldHaveExpectedForeignKeys() {
        schema.assertForeignKeys(TABLE,
                new ForeignKey("tender_id", "tender", ID, "a"),
                new ForeignKey("created_by_user_id", "app_user", ID, "a"));
    }

    @Test
    @DisplayName("[Схема] Индекс участников и частичный уникальный индекс победителя заданы миграцией")
    void shouldHaveExpectedIndexes() {
        schema.assertIndexes(TABLE,
                new Index(false, List.of("tender_id", "created_at", ID), null),
                new Index(true, List.of("tender_id"), "status = 'WINNER'::text"));
    }

    @Test
    @DisplayName("[Сценарий] INSERT сохраняет предложение и заполняет служебные даты")
    void shouldPersistProposalWithDefaults() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");
        long tenderId = insertTender(jdbcTemplate, creatorId);
        OffsetDateTime before = databaseTime(jdbcTemplate);

        long id = insertParticipant(tenderId, "Поставщик А", "PARTICIPATING", creatorId);
        ParticipantRow saved = readParticipant(id);
        OffsetDateTime after = databaseTime(jdbcTemplate);

        assertThat(id).isPositive();
        assertThat(saved.createdAt()).isBetween(before, after);
        assertThat(saved).isEqualTo(new ParticipantRow(tenderId, "Поставщик А", "Поставка за неделю", "PARTICIPATING",
                creatorId, saved.createdAt(), saved.createdAt()));
    }

    @Test
    @DisplayName("[Сценарий] INSERT допускает несколько участников и одного победителя в каждом тендере")
    void shouldAllowSeveralParticipantsAndAWinnerPerTender() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");
        long tenderId = insertTender(jdbcTemplate, creatorId);
        long otherTenderId = insertTender(jdbcTemplate, creatorId);

        long winnerId = insertParticipant(tenderId, "Поставщик А", "WINNER", creatorId);
        long participantId = insertParticipant(tenderId, "Поставщик Б", "PARTICIPATING", creatorId);
        long otherParticipantId = insertParticipant(tenderId, "Поставщик В", "PARTICIPATING", creatorId);
        long otherWinnerId = insertParticipant(otherTenderId, "Поставщик Г", "WINNER", creatorId);

        assertThat(participantIds(tenderId)).containsExactly(winnerId, participantId, otherParticipantId);
        assertThat(readParticipant(winnerId).status()).isEqualTo("WINNER");
        assertThat(readParticipant(participantId).status()).isEqualTo("PARTICIPATING");
        assertThat(readParticipant(otherParticipantId).status()).isEqualTo("PARTICIPATING");
        assertThat(participantIds(otherTenderId)).containsExactly(otherWinnerId);
        assertThat(readParticipant(otherWinnerId).status()).isEqualTo("WINNER");
    }

    @Test
    @DisplayName("[Сценарий] INSERT второго победителя в том же тендере отклоняется")
    void shouldRejectInsertingSecondWinner() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");
        long tenderId = insertTender(jdbcTemplate, creatorId);
        long id = insertParticipant(tenderId, "Поставщик А", "WINNER", creatorId);
        ParticipantRow original = readParticipant(id);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_PARTICIPANT, tenderId, "Поставщик Б", "Поставка за месяц", "WINNER", creatorId));

        assertAll(INSERT_PARTICIPANT,
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(participantIds(tenderId)).containsExactly(id),
                () -> assertThat(readParticipant(id)).isEqualTo(original));
    }

    @Test
    @DisplayName("[Сценарий] UPDATE назначает первого победителя и отклоняет назначение второго")
    void shouldAllowFirstWinnerAndRejectSecondWinnerOnUpdate() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");
        long tenderId = insertTender(jdbcTemplate, creatorId);
        long id = insertParticipant(tenderId, "Поставщик А", "PARTICIPATING", creatorId);
        long otherId = insertParticipant(tenderId, "Поставщик Б", "PARTICIPATING", creatorId);
        ParticipantRow original = readParticipant(id);
        ParticipantRow otherOriginal = readParticipant(otherId);
        OffsetDateTime updatedAt = databaseTime(jdbcTemplate);

        int updatedRows = jdbcTemplate.update(MARK_WINNER, updatedAt, id);
        Throwable failure = catchThrowable(() -> jdbcTemplate.update(MARK_WINNER, updatedAt, otherId));

        assertAll(MARK_WINNER,
                () -> assertThat(updatedRows).isEqualTo(1),
                () -> assertSqlViolation(failure, UNIQUE_VIOLATION),
                () -> assertThat(readParticipant(id)).isEqualTo(new ParticipantRow(tenderId, original.name(),
                        original.proposal(), "WINNER", creatorId, original.createdAt(), updatedAt)),
                () -> assertThat(readParticipant(otherId)).isEqualTo(otherOriginal));
    }

    @Test
    @DisplayName("[Сценарий] INSERT с NULL в proposal отклоняется")
    void shouldRejectMissingProposal() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");
        long tenderId = insertTender(jdbcTemplate, creatorId);

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_PARTICIPANT, tenderId, "Поставщик А", null, "PARTICIPATING", creatorId));

        assertAll(INSERT_PARTICIPANT,
                () -> assertNotNullViolation(failure, "proposal"),
                () -> assertThat(participantIds(tenderId)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] INSERT со ссылкой на несуществующий тендер отклоняется")
    void shouldRejectUnknownTender() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");

        Throwable failure = catchThrowable(() -> jdbcTemplate.update(
                INSERT_PARTICIPANT, MISSING_ID, "Поставщик А", "Поставка за неделю", "PARTICIPATING", creatorId));

        assertAll(INSERT_PARTICIPANT,
                () -> assertSqlViolation(failure, FOREIGN_KEY_VIOLATION),
                () -> assertThat(participantIds(MISSING_ID)).isEmpty());
    }

    @Test
    @DisplayName("[Сценарий] DELETE удаляет выбранного участника тендера")
    void shouldDeleteSelectedParticipant() {
        long creatorId = insertUser(jdbcTemplate, "db-test-participant-author");
        long tenderId = insertTender(jdbcTemplate, creatorId);
        long id = insertParticipant(tenderId, "Поставщик А", "WINNER", creatorId);
        long otherId = insertParticipant(tenderId, "Поставщик Б", "PARTICIPATING", creatorId);

        int deletedRows = jdbcTemplate.update("DELETE FROM tender_participant WHERE id = ?", id);

        assertThat(deletedRows).isEqualTo(1);
        assertThat(participantIds(tenderId)).containsExactly(otherId);
    }

    private long insertParticipant(long tenderId, String name, String status, long creatorId) {
        Long id = jdbcTemplate.queryForObject(INSERT_PARTICIPANT + "RETURNING id", Long.class, tenderId, name,
                "Поставка за неделю", status, creatorId);
        assertThat(id).as("id returned by INSERT INTO tender_participant").isNotNull();
        return id;
    }

    private ParticipantRow readParticipant(long id) {
        return jdbcTemplate.queryForObject("""
                SELECT tender_id, name, proposal, status, created_by_user_id, created_at, updated_at
                  FROM tender_participant WHERE id = ?
                """, PARTICIPANT_MAPPER, id);
    }

    private List<Long> participantIds(long tenderId) {
        return jdbcTemplate.queryForList("SELECT id FROM tender_participant WHERE tender_id = ? ORDER BY id",
                Long.class, tenderId);
    }

    private record ParticipantRow(long tenderId, String name, String proposal, String status, long creatorId,
                                  OffsetDateTime createdAt, OffsetDateTime updatedAt) {
    }
}
