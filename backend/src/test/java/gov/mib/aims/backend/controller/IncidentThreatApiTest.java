package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.mib.aims.backend.model.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты калькуляции угрозы инцидента (UC20).
 */
class IncidentThreatApiTest extends BaseApiTest {

    private static final String INCIDENTS_URL = "/api/v1/incidents";

    @Test
    void calculatesWithDefaultBaseWithoutSavingIncidentOrHistory() throws Exception {
        long id = createIncident(false);
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        var before = jdbcTemplate.queryForMap("select * from incident where id = ?", id);

        mockMvc.perform(post(INCIDENTS_URL + "/" + id + "/threat-calculation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedThreatLevel").value(3))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.contributions.length()").value(3))
                .andExpect(jsonPath("$.contributions[0].criterion").value("DEFAULT_THREAT"))
                .andExpect(jsonPath("$.contributions[0].adjustment").value(3))
                .andExpect(jsonPath("$.contributions[1].criterion").value("EVENT_TYPE"))
                .andExpect(jsonPath("$.contributions[1].adjustment").value(0))
                .andExpect(jsonPath("$.contributions[2].criterion").value("VICTIMS"))
                .andExpect(jsonPath("$.contributions[2].adjustment").value(0));

        assertThat(jdbcTemplate.queryForMap("select * from incident where id = ?", id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = "EXECUTION_COMPLETED", mode = EnumSource.Mode.EXCLUDE)
    void calculatesEveryNonCompletedStatus(IncidentStatus state) throws Exception {
        long id = createIncident(false);
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        jdbcTemplate.update("update incident set status = ? where id = ?", state.name(), id);

        mockMvc.perform(post(INCIDENTS_URL + "/" + id + "/threat-calculation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.calculatedThreatLevel").value(3));
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @Test
    void returnsSpeciesAndEventAndVictimContributions() throws Exception {
        long id = createIncident(true);
        String token = fixtures.signInAndGetToken("analyst", "analyst");

        mockMvc.perform(post(INCIDENTS_URL + "/" + id + "/threat-calculation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.calculatedThreatLevel").value(7))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$.contributions[0].criterion").value("ALIEN_THREAT"))
                .andExpect(jsonPath("$.contributions[0].adjustment").value(4))
                .andExpect(jsonPath("$.contributions[1].adjustment").value(2))
                .andExpect(jsonPath("$.contributions[2].adjustment").value(1));
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"analyst,200", "admin,200", "operator,403", "agent,403", "management,403"})
    void checksCalculationPermission(String login, int expected) throws Exception {
        long id = createIncident(false);
        String token = fixtures.signInAndGetToken(login, login.equals("management") ? "admin" : login);

        mockMvc.perform(post(INCIDENTS_URL + "/" + id + "/threat-calculation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().is(expected));
        var request = acceptance(3);
        accept(token, id, request).andExpect(status().is(expected));
        assertThat(historyCount(id)).isEqualTo(expected == 200 ? 2 : 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 7, 10})
    void savesAnyDomainThreatLevelWithoutCalculationContext(int selected) throws Exception {
        long id = createIncident(true);
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        var request = acceptance(selected);
        var before = jdbcTemplate.queryForMap("select * from incident where id = ?", id);

        accept(token, id, request).andExpect(status().isOk());

        var after = jdbcTemplate.queryForMap("select * from incident where id = ?", id);
        assertThat(after.get("threat_level")).isEqualTo(selected);
        assertThat(after.get("victim_count")).isEqualTo(2);
        assertThat(after.get("alien_id")).isEqualTo(before.get("alien_id"));
        assertThat(after.get("status")).isEqualTo(before.get("status"));
        mockMvc.perform(get(INCIDENTS_URL + "/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[1].snapshot.threatLevel").value(selected))
                .andExpect(jsonPath("$.items[1].snapshot.victimCount").value(2));
        accept(token, id, acceptance(selected)).andExpect(status().isOk());
        assertThat(historyCount(id)).isEqualTo(2);
    }

    @Test
    void requiresThreatLevel() throws Exception {
        long id = createIncident(false);
        String token = fixtures.signInAndGetToken("analyst", "analyst");

        accept(token, id, (ObjectNode) objectMapper.readTree("{}")).andExpect(status().isBadRequest());
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "11", "-1", "2147483648", "null", "true"})
    void rejectsInvalidThreatLevelWithoutChangingIncident(String value) throws Exception {
        long id = createIncident(false);
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        var request = (ObjectNode) objectMapper.readTree("{\"threatLevel\":" + value + "}");
        var before = jdbcTemplate.queryForMap("select * from incident where id = ?", id);

        accept(token, id, request).andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForMap("select * from incident where id = ?", id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @Test
    void missingCompletedAndUnauthenticatedRequestsDoNotCreateHistory() throws Exception {
        long id = createIncident(false);
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(post(INCIDENTS_URL + "/" + Long.MAX_VALUE + "/threat-calculation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isNotFound());
        accept(token, Long.MAX_VALUE, acceptance(3)).andExpect(status().isNotFound());
        jdbcTemplate.update("update incident set status = 'EXECUTION_COMPLETED' where id = ?", id);
        accept(token, id, acceptance(3)).andExpect(status().isConflict());
        mockMvc.perform(post(INCIDENTS_URL + "/" + id + "/threat-calculation")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)).andExpect(status().isConflict());
        mockMvc.perform(post(INCIDENTS_URL + "/" + id + "/threat-calculation")).andExpect(status().isUnauthorized());
        mockMvc.perform(put(INCIDENTS_URL + "/" + id + "/threat-level")
                        .contentType(APPLICATION_JSON).content("{\"threatLevel\":3}"))
                .andExpect(status().isUnauthorized());
        assertThat(historyCount(id)).isEqualTo(1);
    }

    private long createIncident(boolean withAlien) throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long id = fixtures.createIncident(token, fixtures.uploadFile(token));
        if (withAlien) {
            long alienId = jdbcTemplate.queryForObject("select min(id) from alien", Long.class);
            jdbcTemplate.update("update alien set threat_level = 4 where id = ?", alienId);
            jdbcTemplate.update("update incident set event_type = 'ATTACK_ON_PEOPLE', victim_count = 2, "
                    + "alien_id = ? where id = ?", alienId, id);
        } else {
            jdbcTemplate.update("update incident set event_type = 'CONTACT_SUSPECT' where id = ?", id);
        }
        return id;
    }

    private int historyCount(long id) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history "
                + "where entity_type = 'INCIDENT' and entity_id = ?", Integer.class, id);
    }

    private ObjectNode acceptance(int selected) throws Exception {
        return (ObjectNode) objectMapper.readTree("{\"threatLevel\":" + selected + "}");
    }

    private ResultActions accept(String token, long id, ObjectNode request) throws Exception {
        return mockMvc.perform(put(INCIDENTS_URL + "/" + id + "/threat-level")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(APPLICATION_JSON).content(request.toString()));
    }
}
