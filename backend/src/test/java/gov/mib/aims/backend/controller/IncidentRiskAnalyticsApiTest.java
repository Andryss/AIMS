package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.support.AnalyticsTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;

import java.time.Instant;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Исторические разбивки инцидентов по угрозе и количеству пострадавших.
 */
class IncidentRiskAnalyticsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/incidents/statistics/";
    private static final Instant FROM = Instant.parse("2025-05-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-05-02T00:00:00Z");
    private AnalyticsTestFixtures data;

    @BeforeEach
    void prepareFixtures() {
        data = new AnalyticsTestFixtures(jdbcTemplate, objectMapper, fixtures, mockMvc);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "2,2", "3,3", "4,4", "5,5", "6,6", "7,7", "8,8", "9,9", "10,10",
            "NULL,UNKNOWN", "MISSING,UNKNOWN"})
    void groupsAcceptedThreatLevels(String value, String key) throws Exception {
        historyWithField("threatLevel", value);

        report("by-threat-level").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()").value(11))
                .andExpect(jsonPath("$.groups[?(@.key=='" + key + "')].count").value(hasItem(1)));
    }

    @ParameterizedTest
    @CsvSource({"0,0", "1,1", "2,2", "3,3", "4,4", "5,5", "6,6", "7,7", "8,8", "9,9+",
            "10,9+", "2147483647,9+", "NULL,UNKNOWN", "MISSING,UNKNOWN"})
    void separatesUnknownVictimsFromZeroAndGroupsThresholds(String value, String key) throws Exception {
        historyWithField("victimCount", value);

        report("by-victim-count").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()").value(11))
                .andExpect(jsonPath("$.groups[?(@.key=='" + key + "')].count").value(hasItem(1)));
    }

    @ParameterizedTest
    @CsvSource({"by-threat-level,1;2;3;4;5;6;7;8;9;10;UNKNOWN",
            "by-victim-count,0;1;2;3;4;5;6;7;8;9+;UNKNOWN"})
    void includesEmptyGroupsInStableOrder(String metric, String keys) throws Exception {
        report(metric).andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[*].key").value(contains(keys.split(";"))))
                .andExpect(jsonPath("$.groups[*].count").value(everyItem(is(0))));
    }

    @ParameterizedTest
    @CsvSource({"by-threat-level,7", "by-victim-count,2"})
    void usesLastHistoricalStateBeforeToAndHistoricalFilters(String metric, String key) throws Exception {
        long id = data.incident();
        Long alien = jdbcTemplate.queryForObject("select min(id) from alien", Long.class);
        Long agent = appUserRepository.findByLogin("agent").orElseThrow().getId();
        Long admin = appUserRepository.findByLogin("admin").orElseThrow().getId();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        snapshot.put("eventType", "ATTACK_ON_PEOPLE").put("alienId", alien).put("responsibleUserId", agent)
                .put("threatLevel", 4).put("victimCount", 0);
        data.history("INCIDENT", id, FROM.minusSeconds(1), snapshot);
        snapshot.put("threatLevel", 7).put("victimCount", 2);
        data.history("INCIDENT", id, FROM.minusSeconds(1), snapshot);
        snapshot.put("eventType", "ALIEN_CAPTURE").putNull("alienId").putNull("responsibleUserId")
                .put("threatLevel", 10).put("victimCount", 10);
        data.history("INCIDENT", id, TO, snapshot);
        jdbcTemplate.update("update incident set threat_level=1,victim_count=0 where id=?", id);

        mockMvc.perform(get(URL + metric).param("from", FROM.toString()).param("to", TO.toString())
                .param("eventType", "ATTACK_ON_PEOPLE").param("alienId", alien.toString())
                .param("responsibleUserId", agent.toString()).header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='" + key + "')].count").value(hasItem(1)));
        mockMvc.perform(get(URL + metric).param("from", FROM.toString()).param("to", TO.toString())
                .param("responsibleUserId", admin.toString()).header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.groups[*].count").value(everyItem(is(0))));
    }

    private void historyWithField(String field, String value) throws Exception {
        long id = data.incident();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        if ("MISSING".equals(value)) {
            snapshot.remove(field);
        } else if ("NULL".equals(value)) {
            snapshot.putNull(field);
        } else {
            snapshot.put(field, Integer.parseInt(value));
        }
        data.history("INCIDENT", id, FROM, snapshot);
    }

    private org.springframework.test.web.servlet.ResultActions report(String metric) throws Exception {
        return mockMvc.perform(get(URL + metric).param("from", FROM.toString()).param("to", TO.toString())
                .header(HttpHeaders.AUTHORIZATION, token()));
    }

    private String token() throws Exception {
        return "Bearer " + fixtures.signInAndGetToken("admin", "admin");
    }
}
