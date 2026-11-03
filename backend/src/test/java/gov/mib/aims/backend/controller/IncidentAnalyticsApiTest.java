package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.support.AnalyticsTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Исторические состояния инцидентов, переходы и перцентили длительности.
 */
class IncidentAnalyticsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/incidents/statistics";
    private static final Instant FROM = Instant.parse("2025-05-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-05-02T00:00:00Z");
    private AnalyticsTestFixtures data;

    @BeforeEach
    void prepareFixtures() {
        data = new AnalyticsTestFixtures(jdbcTemplate, objectMapper, fixtures, mockMvc);
    }

    @Test
    void countsTransitionsAndExactPercentilesForStartsBeforePeriod() throws Exception {
        completed(10);
        completed(20);
        completed(30);
        completed(40);
        completed(50);

        report("completed-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(5));
        report("completion-duration").andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles.sampleCount").value(5))
                .andExpect(jsonPath("$.percentiles.p50Seconds").value(30.0))
                .andExpect(jsonPath("$.percentiles.p90Seconds").value(46.0))
                .andExpect(jsonPath("$.percentiles.p95Seconds").value(48.0))
                .andExpect(jsonPath("$.percentiles.p99Seconds").value(closeTo(49.6, 0.000001)));
    }

    @Test
    void preservesPastStatusAndIncludesOnlyLeftBoundary() throws Exception {
        long id = data.incident();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        snapshot.put("createdAt", FROM.toString().replace("Z", ""));
        data.history("INCIDENT", id, FROM, snapshot);
        snapshot.put("status", "EXECUTION_COMPLETED");
        data.history("INCIDENT", id, TO, snapshot);
        jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", id);

        report("created-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("completed-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        report("open-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("by-status").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='DRAFT')].count").value(hasItem(1)));
    }

    @Test
    void filtersByPastSpeciesAndResponsibleAndDetectsStaleCards() throws Exception {
        long id = data.incident();
        Long alien = jdbcTemplate.queryForObject("select min(id) from alien", Long.class);
        Long agent = appUserRepository.findByLogin("agent").orElseThrow().getId();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        snapshot.put("alienId", alien).put("responsibleUserId", agent);
        data.history("INCIDENT", id, FROM.minus(5, ChronoUnit.DAYS), snapshot);
        snapshot.remove("alienId");
        snapshot.remove("responsibleUserId");
        data.history("INCIDENT", id, TO.plusSeconds(1), snapshot);

        mockMvc.perform(get(URL + "/stale").param("from", FROM.toString()).param("to", TO.toString())
                .param("alienId", alien.toString()).param("responsibleUserId", agent.toString())
                .header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items[0].incidentId").value(id));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    void handlesEmptyAndSingleDurationSamples(int count) throws Exception {
        if (count == 1) {
            completed(0);
        }
        var response = report("completion-duration").andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles.sampleCount").value(count));
        if (count == 0) {
            response.andExpect(jsonPath("$.percentiles.p99Seconds").doesNotExist());
        } else {
            response.andExpect(jsonPath("$.percentiles.p99Seconds").value(0.0));
        }
    }

    @Test
    void reportsUnavailableHistoryWithoutUsingCurrentState() throws Exception {
        long id = data.incident();
        data.clearHistory("INCIDENT", id);
        jdbcTemplate.update("update incident set created_at=timestamp '2025-04-01',status='EXECUTING' where id=?", id);

        report("total-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0))
                .andExpect(jsonPath("$.meta.recordsWithoutSnapshot").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "NEGATIVE", "NO_BIRTH"})
    void excludesInvalidCompletionDurations(String reason) throws Exception {
        long id = data.incident();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        if ("MISSING".equals(reason)) {
            snapshot.remove("createdAt");
        } else {
            snapshot.put("createdAt", FROM.plusSeconds(10).toString().replace("Z", ""));
        }
        if ("NO_BIRTH".equals(reason)) {
            snapshot.put("status", "EXECUTING");
        }
        data.history("INCIDENT", id, FROM.minusSeconds(1), snapshot);
        snapshot.put("status", "EXECUTION_COMPLETED");
        data.history("INCIDENT", id, FROM, snapshot);

        report("completion-duration").andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles.sampleCount").value(0))
                .andExpect(jsonPath("$.percentiles.excludedSampleCount").value(1))
                .andExpect(jsonPath("$.percentiles.p50Seconds").doesNotExist());
    }

    @Test
    void usesHistoryIdToOrderChangesAtTheSameTime() throws Exception {
        long id = data.incident();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        snapshot.put("createdAt", FROM.toString().replace("Z", ""));
        data.history("INCIDENT", id, FROM, snapshot);
        snapshot.put("status", "EXECUTION_COMPLETED");
        data.history("INCIDENT", id, FROM, snapshot);
        snapshot.put("description", "Уточнение");
        data.history("INCIDENT", id, FROM, snapshot);

        report("completed-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("completed-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("trend").andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets[0].values.created").value(1))
                .andExpect(jsonPath("$.buckets[0].values.completed").value(1))
                .andExpect(jsonPath("$.buckets[1].values.completed").value(0));
    }

    @Test
    void keepsFullStaleCountAndTruncatesStableList() throws Exception {
        long first = 0;
        for (int index = 0; index < 12; index++) {
            long id = data.incident();
            if (index == 0) {
                first = id;
            }
            var snapshot = data.snapshot("INCIDENT", id);
            data.clearHistory("INCIDENT", id);
            snapshot.put("location", index == 0 ? "OTHER" : "Локация " + index);
            data.history("INCIDENT", id, FROM.minus(5, ChronoUnit.DAYS), snapshot);
        }

        report("stale").andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(12))
                .andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.items[0].incidentId").value(first));
        report("by-alien").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='UNKNOWN')].count").value(hasItem(12)));
        report("by-location").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups.length()").value(11))
                .andExpect(jsonPath("$.groups[?(@.key=='OTHER')].count").value(hasItem(2)))
                .andExpect(jsonPath("$.groups[?(@.key=='LOCATION:OTHER')].count").value(hasItem(1)));
    }

    private void completed(int seconds) throws Exception {
        long id = data.incident();
        var snapshot = data.snapshot("INCIDENT", id);
        data.clearHistory("INCIDENT", id);
        snapshot.put("createdAt", FROM.minusSeconds(seconds).toString().replace("Z", ""));
        data.history("INCIDENT", id, FROM.minusSeconds(seconds + 1L), snapshot);
        snapshot.put("status", "EXECUTION_COMPLETED");
        data.history("INCIDENT", id, FROM, snapshot);
        snapshot.put("description", "Последующее редактирование");
        data.history("INCIDENT", id, FROM.plusSeconds(1), snapshot);
    }

    private org.springframework.test.web.servlet.ResultActions report(String metric) throws Exception {
        return mockMvc.perform(get(URL + "/" + metric).param("from", FROM.toString()).param("to", TO.toString())
                .header(HttpHeaders.AUTHORIZATION, token()));
    }

    private String token() throws Exception {
        return "Bearer " + fixtures.signInAndGetToken("admin", "admin");
    }
}
