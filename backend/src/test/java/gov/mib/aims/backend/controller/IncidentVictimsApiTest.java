package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты количества пострадавших и типов событий инцидента.
 */
class IncidentVictimsApiTest extends BaseApiTest {

    private static final String INCIDENTS_URL = "/api/v1/incidents";

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"null", "0", "2", "2147483647"})
    void createsIncidentWithOptionalVictimCount(String count) throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        var request = createRequest(token);
        if (count != null) {
            request.set("victimCount", objectMapper.readTree(count));
        }

        var result = mockMvc.perform(post(INCIDENTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.threatLevel").value(nullValue()))
                .andReturn();
        var incident = objectMapper.readTree(result.getResponse().getContentAsString());
        Integer expected = count == null || count.equals("null") ? null : Integer.valueOf(count);
        assertThat(incident.has("victimCount")).isTrue();
        assertThat(incident.get("victimCount").isNull() ? null : incident.get("victimCount").intValue())
                .isEqualTo(expected);

        long id = incident.get("id").longValue();
        assertThat(jdbcTemplate.queryForObject("select victim_count from incident where id = ?", Integer.class, id))
                .isEqualTo(expected);
        mockMvc.perform(get(INCIDENTS_URL + "/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].snapshot.victimCount").value(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "2147483648", "true"})
    void rejectsInvalidVictimCountOnCreate(String count) throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        var request = createRequest(token);
        request.set("victimCount", objectMapper.readTree(count));

        mockMvc.perform(post(INCIDENTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("select count(*) from incident", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"ATTACK_ON_PEOPLE", "PROPERTY_DAMAGE", "BIOLOGICAL_CONTAMINATION",
            "CRITICAL_INFRASTRUCTURE_DISRUPTION"})
    void createsAndReadsNewEventTypes(String eventType) throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        var request = createRequest(token).put("eventType", eventType);

        var result = mockMvc.perform(post(INCIDENTS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.eventType").value(eventType)).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").longValue();
        mockMvc.perform(get(INCIDENTS_URL + "/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].snapshot.eventType").value(eventType));
    }

    @Test
    void readsLegacyHistoryWithoutNewFields() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long id = fixtures.createIncident(token, fixtures.uploadFile(token));
        jdbcTemplate.update("update entity_history set snapshot = (snapshot - 'victimCount' - 'threatLevel') "
                + "where entity_type = 'INCIDENT' and entity_id = ?", id);

        mockMvc.perform(get(INCIDENTS_URL + "/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].snapshot.victimCount").value(nullValue()))
                .andExpect(jsonPath("$.items[0].snapshot.threatLevel").value(nullValue()));
    }

    private ObjectNode createRequest(String token) throws Exception {
        return (ObjectNode) objectMapper.readTree(objectMapper.writeValueAsString(
                fixtures.minimalCreateRequest(fixtures.uploadFile(token))));
    }

}
