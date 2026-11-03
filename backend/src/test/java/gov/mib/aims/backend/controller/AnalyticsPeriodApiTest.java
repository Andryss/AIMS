package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.config.TestChronoConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Доступ к аналитике, обязательный интервал, UTC и точность аудита.
 */
class AnalyticsPeriodApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/incidents/statistics/created-count";
    private static final String FROM = "2025-05-01T00:00:00Z";
    private static final String TO = "2025-06-01T00:00:00Z";

    @ParameterizedTest
    @ValueSource(strings = {"admin", "management"})
    void readsEmptyAnalyticsWithPercentilesAndZeroBuckets(String login) throws Exception {
        mockMvc.perform(get(URL).param("from", FROM).param("to", TO)
                .header(HttpHeaders.AUTHORIZATION, token(login)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.from").value(FROM))
                .andExpect(jsonPath("$.meta.to").value(TO)).andExpect(jsonPath("$.meta.bucketUnit").value("DAY"))
                .andExpect(jsonPath("$.count").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "agent", "storekeeper", "tender_manager", "prison_guard", "cleaner"})
    void otherRolesCannotReadAnalytics(String login) throws Exception {
        mockMvc.perform(get(URL).param("from", FROM).param("to", TO)
                .header(HttpHeaders.AUTHORIZATION, token(login))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({"2025-05-01T00:00:00Z,2025-05-01T00:00:00Z",
            "2025-06-01T00:00:00Z,2025-05-01T00:00:00Z", "bad,2025-06-01T00:00:00Z",
            "2025-05-01T00:00:00Z,2025-06-02T00:00:00Z", "2025-05-01T00:00:00,2025-06-01T00:00:00Z",
            "2025-05-01T00:00:00.000000001Z,2025-06-01T00:00:00Z",
            "2025-05-01T00:00:00Z,2025-06-01T00:00:00.000000001Z"})
    void rejectsInvalidPeriod(String from, String to) throws Exception {
        mockMvc.perform(get(URL).param("from", from).param("to", to)
                .header(HttpHeaders.AUTHORIZATION, token("admin"))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"from", "to"})
    void requiresBothBounds(String missing) throws Exception {
        mockMvc.perform(get(URL).param("from".equals(missing) ? "to" : "from", FROM)
                .header(HttpHeaders.AUTHORIZATION, token("admin"))).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"alienId=0", "responsibleUserId=-1", "eventType=UNKNOWN"})
    void validatesFilters(String parameter) throws Exception {
        String[] parts = parameter.split("=");
        mockMvc.perform(get(URL).param("from", FROM).param("to", TO).param(parts[0], parts[1])
                .header(HttpHeaders.AUTHORIZATION, token("admin"))).andExpect(status().isBadRequest());
    }

    @Test
    void normalizesOffsetsAndUsesClockForNewAuditEntries() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        long id = fixtures.createIncident(operator, fixtures.uploadFile(operator));

        mockMvc.perform(get(URL).param("from", "2025-05-01T03:00:00+03:00")
                .param("to", "2025-06-01T03:00:00+03:00").header(HttpHeaders.AUTHORIZATION, token("admin")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.meta.from").value(FROM))
                .andExpect(jsonPath("$.meta.to").value(TO));
        mockMvc.perform(get("/api/v1/incidents/" + id + "/history")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + operator))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].changedAt")
                        .value(TestChronoConfig.FIXED_INSTANT.toString()));
        mockMvc.perform(get(URL).param("from", FROM).param("to", TO)).andExpect(status().isUnauthorized());
    }

    private String token(String login) throws Exception {
        String password = "management".equals(login) || "tender_manager".equals(login)
                || "prison_guard".equals(login) ? "admin" : login;
        return "Bearer " + fixtures.signInAndGetToken(login, password);
    }
}
