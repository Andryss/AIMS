package gov.mib.aims.backend.db.util;

import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

public class IncidentDbFixtures {

    private IncidentDbFixtures() {
    }

    public static long insertIncident(JdbcTemplate jdbcTemplate, long creatorId) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO incident (status, event_type, location, detected_at, description, created_by_user_id)
                VALUES ('NEW', 'OBSERVATION', 'Test location', LOCALTIMESTAMP, 'Test incident', ?)
                RETURNING id
                """, Long.class, creatorId);
        assertThat(id).as("id returned by INSERT INTO incident").isNotNull();
        return id;
    }
}
