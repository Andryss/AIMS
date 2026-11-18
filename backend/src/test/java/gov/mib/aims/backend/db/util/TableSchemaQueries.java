package gov.mib.aims.backend.db.util;

import org.postgresql.util.PSQLException;
import org.postgresql.util.ServerErrorMessage;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TableSchemaQueries {

    public static final String ID = "id";
    public static final String BIGINT = "bigint";
    public static final String INTEGER = "integer";
    public static final String TEXT = "text";
    public static final String TIMESTAMP_WITH_TIME_ZONE = "timestamp with time zone";

    public static final String FOREIGN_KEY_VIOLATION = "23503";
    public static final String UNIQUE_VIOLATION = "23505";
    private static final String NOT_NULL_VIOLATION = "23502";

    private static final RowMapper<Column> COLUMN_MAPPER = (row, index) -> new Column(
            row.getString("column_name"),
            row.getString("data_type"),
            "YES".equals(row.getString("is_nullable")),
            row.getString("column_default"),
            row.getString("sequence_name"));

    private static final RowMapper<Key> KEY_MAPPER = (row, index) -> new Key(
            List.of((String[]) row.getArray("columns").getArray()));

    private static final RowMapper<ForeignKey> FOREIGN_KEY_MAPPER = (row, index) -> {
        String column = row.getString("column_name");
        assertThat(row.getInt("column_count"))
                .as("number of columns in foreign key on %s.%s", row.getString("table_name"), column)
                .isEqualTo(1);

        return new ForeignKey(column, row.getString("referenced_table"), row.getString("referenced_column"),
                row.getString("confdeltype"));
    };

    private static final RowMapper<Index> INDEX_MAPPER = (row, index) -> new Index(
            row.getBoolean("indisunique"),
            List.of((String[]) row.getArray("columns").getArray()),
            row.getString("predicate"));

    private final JdbcTemplate jdbcTemplate;

    public TableSchemaQueries(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void assertColumns(String table, String... expectedColumns) {
        assertThat(columns(table).keySet())
                .as("%s columns", table)
                .containsExactlyInAnyOrder(expectedColumns);
    }

    public void assertColumn(String table, Column expected) {
        Column actual = columns(table).get(expected.name());
        String column = table + "." + expected.name();

        assertThat(actual).as("column %s", column).isNotNull();
        assertAll(column,
                () -> assertThat(actual.type()).as("type").isEqualTo(expected.type()),
                () -> assertThat(actual.nullable()).as("allows NULL").isEqualTo(expected.nullable()),
                () -> assertThat(actual.defaultValue()).as("default").isEqualTo(expected.defaultValue()),
                () -> assertThat(actual.sequenceName()).as("owned sequence").isEqualTo(expected.sequenceName()));
    }

    public void assertPrimaryKeys(String table, Key... expectedKeys) {
        assertThat(keys(table, "PRIMARY KEY"))
                .as("%s primary keys", table)
                .containsExactlyInAnyOrder(expectedKeys);
    }

    public void assertUniqueKeys(String table, Key... expectedKeys) {
        assertThat(keys(table, "UNIQUE"))
                .as("%s unique keys", table)
                .containsExactlyInAnyOrder(expectedKeys);
    }

    public void assertForeignKeys(String table, ForeignKey... expectedKeys) {
        assertThat(foreignKeys(table))
                .as("%s foreign keys", table)
                .containsExactlyInAnyOrder(expectedKeys);
    }

    public void assertIndexes(String table, Index... expectedIndexes) {
        assertThat(indexes(table))
                .as("%s standalone indexes", table)
                .containsExactlyInAnyOrder(expectedIndexes);
    }

    public static PSQLException assertSqlViolation(Throwable failure, String expectedState) {
        DataAccessException dataError = assertInstanceOf(DataAccessException.class, failure);
        PSQLException postgresError = assertInstanceOf(PSQLException.class, dataError.getMostSpecificCause());
        assertThat(postgresError.getSQLState()).as("SQLSTATE").isEqualTo(expectedState);
        return postgresError;
    }

    public static void assertNotNullViolation(Throwable failure, String expectedColumn) {
        ServerErrorMessage details = assertSqlViolation(failure, NOT_NULL_VIOLATION).getServerErrorMessage();
        assertThat(details).as("PostgreSQL error details").isNotNull();
        assertThat(details.getColumn()).as("column reported by PostgreSQL").isEqualTo(expectedColumn);
    }

    private Map<String, Column> columns(String table) {
        List<Column> columns = jdbcTemplate.query("""
                SELECT column_name, data_type, is_nullable, column_default,
                       pg_get_serial_sequence(format('%I.%I', table_schema, table_name), column_name) AS sequence_name
                  FROM information_schema.columns
                 WHERE table_schema = 'public' AND table_name = ?
                """, COLUMN_MAPPER, table);
        return columns.stream().collect(Collectors.toMap(Column::name, Function.identity()));
    }

    private List<ForeignKey> foreignKeys(String table) {
        return jdbcTemplate.query("""
                SELECT source_column.table_name,
                       source_column.column_name,
                       target_table.relname AS referenced_table,
                       target_column.attname AS referenced_column,
                       fk.confdeltype,
                       cardinality(fk.conkey) AS column_count
                  FROM information_schema.key_column_usage AS source_column
                  JOIN pg_constraint AS fk
                    ON fk.conrelid = to_regclass(format('%I.%I', source_column.table_schema, source_column.table_name))
                   AND fk.conname = source_column.constraint_name AND fk.contype = 'f'
                  JOIN pg_class AS target_table ON target_table.oid = fk.confrelid
                  JOIN pg_attribute AS target_column
                    ON target_column.attrelid = fk.confrelid
                   AND target_column.attnum = fk.confkey[1]
                 WHERE source_column.table_schema = 'public'
                   AND source_column.table_name = ?
                   AND source_column.ordinal_position = 1
                """, FOREIGN_KEY_MAPPER, table);
    }

    private List<Key> keys(String table, String type) {
        return jdbcTemplate.query("""
                SELECT array_agg(key_columns.column_name::text ORDER BY key_columns.ordinal_position) AS columns
                  FROM information_schema.table_constraints AS constraints
                  JOIN information_schema.key_column_usage AS key_columns
                    USING (constraint_catalog, constraint_schema, constraint_name,
                           table_catalog, table_schema, table_name)
                 WHERE constraints.table_schema = 'public'
                   AND constraints.table_name = ?
                   AND constraints.constraint_type = ?
                 GROUP BY constraints.constraint_name
                """, KEY_MAPPER, table, type);
    }

    private List<Index> indexes(String table) {
        return jdbcTemplate.query("""
                SELECT idx.indisunique,
                       ARRAY(
                           SELECT pg_get_indexdef(idx.indexrelid, key.position, true)
                                  || CASE WHEN pg_index_column_has_property(idx.indexrelid, key.position, 'desc')
                                          THEN ' DESC' ELSE '' END
                             FROM generate_series(1, idx.indnkeyatts) AS key(position)
                            ORDER BY key.position
                       ) AS columns,
                       pg_get_expr(idx.indpred, idx.indrelid, true) AS predicate
                  FROM pg_index AS idx
                 WHERE idx.indrelid = to_regclass(format('public.%I', ?))
                   AND idx.indisvalid
                   AND NOT EXISTS (
                       SELECT 1
                         FROM pg_constraint AS constraints
                        WHERE constraints.conindid = idx.indexrelid
                          AND constraints.contype IN ('p', 'u', 'x')
                   )
                """, INDEX_MAPPER, table);
    }

    public record Column(String name, String type, boolean nullable, String defaultValue, String sequenceName) {

        public static Column bigSerialId(String table) {
            String sequence = table + "_id_seq";
            return new Column(ID, BIGINT, false, "nextval('%s'::regclass)".formatted(sequence), "public." + sequence);
        }

        public static Column required(String name, String type) {
            return required(name, type, null);
        }

        public static Column required(String name, String type, String defaultValue) {
            return new Column(name, type, false, defaultValue, null);
        }

        public static Column optional(String name, String type) {
            return new Column(name, type, true, null, null);
        }
    }

    public record Key(List<String> columns) {
        public Key(String... columns) {
            this(List.of(columns));
        }
    }

    public record ForeignKey(String column, String referencedTable, String referencedColumn, String deleteRuleCode) {
    }

    public record Index(boolean unique, List<String> columns, String predicate) {
    }
}
