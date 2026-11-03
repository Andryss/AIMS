package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.mib.aims.backend.support.AnalyticsTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Историческая аналитика заявок, сроков и фактического содержания.
 */
class DetentionAnalyticsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/detention-requests/statistics";
    private static final Instant FROM = Instant.parse("2025-05-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-05-02T00:00:00Z");
    private AnalyticsTestFixtures data;

    @BeforeEach
    void prepareFixtures() {
        data = new AnalyticsTestFixtures(jdbcTemplate, objectMapper, fixtures, mockMvc);
    }

    @Test
    void computesActualCustodyPercentilesForReleasesInsidePeriod() throws Exception {
        released(10);
        released(20);
        released(30);
        released(40);
        released(50);

        report("created-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        report("released-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(5));
        report("custody-duration").andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles.sampleCount").value(5))
                .andExpect(jsonPath("$.percentiles.p50Seconds").value(30.0))
                .andExpect(jsonPath("$.percentiles.p90Seconds").value(46.0))
                .andExpect(jsonPath("$.percentiles.p95Seconds").value(48.0))
                .andExpect(jsonPath("$.percentiles.p99Seconds").value(closeTo(49.6, 0.000001)));
    }

    @Test
    void keepsPastExpiryWhenDurationAndStatusChangeLater() throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        data.clearHistory("DETENTION_REQUEST", id);
        var value = (ObjectNode) snapshot.get("request");
        value.put("status", "IN_CUSTODY").put("admittedAt", FROM.minus(2, ChronoUnit.DAYS).toString())
                .put("durationDays", 1);
        data.history("DETENTION_REQUEST", id, FROM.minusSeconds(1), snapshot);
        value.put("durationDays", 90).put("status", "RELEASED").put("releasedAt", TO.plusSeconds(1).toString());
        data.history("DETENTION_REQUEST", id, TO.plusSeconds(1), snapshot);

        report("in-custody-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("overdue-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("released-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        report("by-remaining-time").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='OVERDUE')].count").value(hasItem(1)));
        report("upcoming-releases").andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(0));
    }

    @ParameterizedTest
    @CsvSource({"30,UP_TO_30", "31,FROM_31_TO_90", "90,FROM_31_TO_90", "91,OVER_90"})
    void groupsAssignedTermAtHistoricalCutoff(int days, String group) throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        data.clearHistory("DETENTION_REQUEST", id);
        ((ObjectNode) snapshot.get("request")).put("status", "ACCEPTED").put("durationDays", days);
        data.history("DETENTION_REQUEST", id, FROM.minusSeconds(1), snapshot);

        report("by-assigned-term").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='" + group + "')].count").value(hasItem(1)));
    }

    @ParameterizedTest
    @CsvSource({"0,OVERDUE", "7,UP_TO_7", "30,FROM_7_TO_30", "31,OVER_30"})
    void groupsRemainingTermWithoutRounding(int remaining, String group) throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        data.clearHistory("DETENTION_REQUEST", id);
        ((ObjectNode) snapshot.get("request")).put("status", "IN_CUSTODY").put("durationDays", 60)
                .put("admittedAt", TO.minus(60L - remaining, ChronoUnit.DAYS).toString());
        data.history("DETENTION_REQUEST", id, FROM.minusSeconds(1), snapshot);

        report("by-remaining-time").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='" + group + "')].count").value(hasItem(1)));
    }

    @ParameterizedTest
    @CsvSource({"0,1,0", "-1,0,1"})
    void countsValidZeroAndExcludesInvalidDuration(int seconds, int samples, int excluded) throws Exception {
        released(seconds);

        report("custody-duration").andExpect(status().isOk()).andExpect(jsonPath("$.percentiles.sampleCount").value(samples))
                .andExpect(jsonPath("$.percentiles.excludedSampleCount").value(excluded));
    }

    @Test
    void emptyReportHasAllStatusesAndNoSyntheticDurations() throws Exception {
        report("by-status").andExpect(status().isOk()).andExpect(jsonPath("$.groups.length()").value(6));
        report("custody-duration").andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles.sampleCount").value(0))
                .andExpect(jsonPath("$.percentiles.p99Seconds").doesNotExist());
    }

    @ParameterizedTest
    @CsvSource({"NEW,0", "REJECTED,0", "CANCELLED,0", "ACCEPTED,1"})
    void onlyAcceptedRequestsAwaitAdmission(String state, int awaiting) throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        data.clearHistory("DETENTION_REQUEST", id);
        ((ObjectNode) snapshot.get("request")).put("status", state);
        data.history("DETENTION_REQUEST", id, FROM, snapshot);

        report("awaiting-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(awaiting));
        report("admitted-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void excludesMissingActualDates() throws Exception {
        released(30);
        jdbcTemplate.update("""
                update entity_history set snapshot=snapshot #- '{request,admittedAt}'
                where entity_type='DETENTION_REQUEST'
                """);

        report("custody-duration").andExpect(status().isOk())
                .andExpect(jsonPath("$.percentiles.sampleCount").value(0))
                .andExpect(jsonPath("$.percentiles.excludedSampleCount").value(1));
    }

    @Test
    void countsAdmissionOnceDespiteEditsAndCompletedIncident() throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        var value = (ObjectNode) snapshot.get("request");
        long incident = value.get("incidentId").asLong();
        data.clearHistory("DETENTION_REQUEST", id);
        value.put("status", "ACCEPTED");
        data.history("DETENTION_REQUEST", id, FROM.minusSeconds(1), snapshot);
        value.put("status", "IN_CUSTODY").put("admittedAt", FROM.toString());
        data.history("DETENTION_REQUEST", id, FROM, snapshot);
        value.put("subjectDescription", "Уточнение");
        data.history("DETENTION_REQUEST", id, FROM.plusSeconds(1), snapshot);
        jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", incident);

        report("admitted-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("in-custody-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("by-alien").andExpect(status().isOk())
                .andExpect(jsonPath("$.groups[?(@.key=='UNKNOWN')].count").value(hasItem(1)));
    }

    @Test
    void keepsFullUpcomingCountAndStableTenRows() throws Exception {
        long first = 0;
        for (int index = 0; index < 12; index++) {
            long id = request();
            if (index == 0) {
                first = id;
            }
            var snapshot = data.snapshot("DETENTION_REQUEST", id);
            data.clearHistory("DETENTION_REQUEST", id);
            ((ObjectNode) snapshot.get("request")).put("status", "IN_CUSTODY")
                    .put("admittedAt", FROM.toString()).put("durationDays", 30);
            data.history("DETENTION_REQUEST", id, FROM, snapshot);
        }

        report("upcoming-releases").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(12)).andExpect(jsonPath("$.items.length()").value(10))
                .andExpect(jsonPath("$.items[0].requestId").value(first));
    }

    @Test
    void computesCustodyDaysAsTwentyFourHoursAcrossDst() throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        data.clearHistory("DETENTION_REQUEST", id);
        ((ObjectNode) snapshot.get("request")).put("status", "IN_CUSTODY").put("durationDays", 1)
                .put("admittedAt", "2000-03-25T12:00:00Z");
        data.history("DETENTION_REQUEST", id, Instant.parse("2000-03-25T12:00:00Z"), snapshot);
        String token = "Bearer " + fixtures.signInAndGetToken("admin", "admin");

        mockMvc.perform(get(URL + "/overdue-at-to").param("from", "2000-03-25T00:00:00Z")
                .param("to", "2000-03-26T11:30:00Z").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        mockMvc.perform(get(URL + "/upcoming-releases").param("from", "2000-03-25T00:00:00Z")
                .param("to", "2000-03-26T11:30:00Z").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].plannedReleaseAt").value("2000-03-26T12:00:00Z"));
    }

    private long request() throws Exception {
        return data.create("detention-requests", Map.of("incidentId", data.incident(), "subjectName", "Субъект",
                "subjectDescription", "Описание", "durationDays", 30));
    }

    private void released(int seconds) throws Exception {
        long id = request();
        var snapshot = data.snapshot("DETENTION_REQUEST", id);
        var value = (ObjectNode) snapshot.get("request");
        data.clearHistory("DETENTION_REQUEST", id);
        data.history("DETENTION_REQUEST", id, FROM.minus(2, ChronoUnit.DAYS), snapshot);
        value.put("status", "ACCEPTED");
        data.history("DETENTION_REQUEST", id, FROM.minusSeconds(3600), snapshot);
        value.put("status", "IN_CUSTODY").put("admittedAt", FROM.minusSeconds(seconds).toString());
        data.history("DETENTION_REQUEST", id, FROM.minusSeconds(1), snapshot);
        value.put("status", "RELEASED").put("releasedAt", FROM.toString());
        data.history("DETENTION_REQUEST", id, FROM, snapshot);
        value.put("subjectDescription", "Последующее уточнение");
        data.history("DETENTION_REQUEST", id, FROM.plusSeconds(1), snapshot);
    }

    private ResultActions report(String metric) throws Exception {
        String token = fixtures.signInAndGetToken("admin", "admin");
        return mockMvc.perform(get(URL + "/" + metric).param("from", FROM.toString()).param("to", TO.toString())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }
}
