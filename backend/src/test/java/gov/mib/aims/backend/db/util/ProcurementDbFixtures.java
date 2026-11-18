package gov.mib.aims.backend.db.util;

import org.springframework.jdbc.core.JdbcTemplate;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

public class ProcurementDbFixtures {

    private ProcurementDbFixtures() {
    }

    public static long insertTender(JdbcTemplate jdbcTemplate, long creatorId) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO tender (equipment_type, quantity, status, created_by_user_id)
                VALUES ('NEURALYZER', 2, 'NEED', ?) RETURNING id
                """, Long.class, creatorId);
        assertThat(id).as("id returned by INSERT INTO tender").isNotNull();
        return id;
    }

    public static long insertIssueMovement(JdbcTemplate jdbcTemplate, long incidentId, long userId) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO equipment_movement (operation, status, incident_id, responsible_user_id, created_by_user_id)
                VALUES ('ISSUE', 'PENDING', ?, ?, ?) RETURNING id
                """, Long.class, incidentId, userId, userId);
        assertThat(id).as("id returned by INSERT INTO equipment_movement").isNotNull();
        return id;
    }

    public static long insertEquipmentItem(JdbcTemplate jdbcTemplate, String serialNumber) {
        Long id = jdbcTemplate.queryForObject("""
                INSERT INTO equipment_item (type, model, serial_number, status)
                VALUES ('NEURALYZER', 'NX-1', ?, 'AVAILABLE') RETURNING id
                """, Long.class, serialNumber);
        assertThat(id).as("id returned by INSERT INTO equipment_item").isNotNull();
        return id;
    }

    public static OffsetDateTime databaseTime(JdbcTemplate jdbcTemplate) {
        return jdbcTemplate.queryForObject("SELECT clock_timestamp()", OffsetDateTime.class);
    }
}
