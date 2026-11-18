package gov.mib.aims.backend.db.util;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class RbacDbFixtures {

    public static final long MISSING_ID = Long.MIN_VALUE;
    public static final String INSERT_ROLE = "INSERT INTO role (name, description) VALUES (?, ?)";

    private RbacDbFixtures() {
    }

    public static long insertRole(JdbcTemplate jdbcTemplate, String name, String description) {
        Long id = jdbcTemplate.queryForObject(INSERT_ROLE + " RETURNING id", Long.class, name, description);
        assertThat(id).as("id returned by INSERT INTO role").isNotNull();
        return id;
    }

    public static long insertUser(JdbcTemplate jdbcTemplate, String login) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO app_user (login, password_hash) VALUES (?, ?) RETURNING id
                """, Long.class, login, "db-test-hash");
        assertThat(id).as("id returned by INSERT INTO app_user").isNotNull();
        return id;
    }

    public static long insertPermission(JdbcTemplate jdbcTemplate, String code) {
        Long id = jdbcTemplate.queryForObject(
                "INSERT INTO permission (code) VALUES (?) RETURNING id", Long.class, code);
        assertThat(id).as("id returned by INSERT INTO permission").isNotNull();
        return id;
    }
}
