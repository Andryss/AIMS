package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Независимость ресурсов, доступ и масштаб отдельных графиков.
 */
class AnalyticsResourcesApiTest extends BaseApiTest {

    private static final String BASE = "/api/v1/";
    private static final String FROM = "2025-05-01T00:00:00Z";
    private static final String TO = "2025-06-01T00:00:00Z";

    @ParameterizedTest
    @CsvSource({
            "incidents/statistics/created-count,count",
            "incidents/statistics/completed-count,count",
            "incidents/statistics/total-at-to,count",
            "incidents/statistics/completed-at-to,count",
            "incidents/statistics/open-at-to,count",
            "incidents/statistics/completion-duration,percentiles",
            "incidents/statistics/trend,buckets",
            "incidents/statistics/by-status,groups",
            "incidents/statistics/by-event-type,groups",
            "incidents/statistics/by-alien,groups",
            "incidents/statistics/by-location,groups",
            "incidents/statistics/stale,items",
            "equipment/statistics/received-count,count",
            "equipment/statistics/issued-count,count",
            "equipment/statistics/returned-count,count",
            "equipment/statistics/total-at-to,count",
            "equipment/statistics/available-at-to,count",
            "equipment/statistics/issued-at-to,count",
            "equipment/statistics/trend,buckets",
            "equipment/statistics/inventory-by-type,items",
            "equipment/statistics/open-requests,items",
            "equipment/statistics/expected-deliveries-by-type,items",
            "equipment/statistics/outstanding-by-incident,items",
            "detention-requests/statistics/created-count,count",
            "detention-requests/statistics/admitted-count,count",
            "detention-requests/statistics/released-count,count",
            "detention-requests/statistics/awaiting-at-to,count",
            "detention-requests/statistics/in-custody-at-to,count",
            "detention-requests/statistics/overdue-at-to,count",
            "detention-requests/statistics/custody-duration,percentiles",
            "detention-requests/statistics/trend,buckets",
            "detention-requests/statistics/by-status,groups",
            "detention-requests/statistics/by-alien,groups",
            "detention-requests/statistics/by-assigned-term,groups",
            "detention-requests/statistics/by-remaining-time,groups",
            "detention-requests/statistics/upcoming-releases,items"})
    void exposesEachWidgetIndependentlyWithItsOwnAccessCheck(String path, String field) throws Exception {
        String management = "Bearer " + fixtures.signInAndGetToken("management", "admin");
        String operator = "Bearer " + fixtures.signInAndGetToken("operator", "operator");

        mockMvc.perform(get(BASE + path).param("from", FROM).param("to", TO)
                .header(HttpHeaders.AUTHORIZATION, management)).andExpect(status().isOk())
                .andExpect(jsonPath("$." + field).exists()).andExpect(jsonPath("$.meta.from").value(FROM));
        mockMvc.perform(get(BASE + path).param("from", FROM).param("to", TO)
                .header(HttpHeaders.AUTHORIZATION, operator)).andExpect(status().isForbidden());
        mockMvc.perform(get(BASE + path).param("from", FROM).param("to", TO)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(BASE + path).param("from", TO).param("to", FROM)
                .header(HttpHeaders.AUTHORIZATION, management)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"incidents", "equipment", "detentions"})
    void aggregateResourcesAreNotExposed(String domain) throws Exception {
        String admin = "Bearer " + fixtures.signInAndGetToken("admin", "admin");

        mockMvc.perform(get("/api/v1/analytics/" + domain).param("from", FROM).param("to", TO)
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @CsvSource({
            "2025-05-01T00:30:00Z,2025-05-03T00:30:00Z,HOUR,1",
            "2025-05-01T00:30:00Z,2025-05-03T00:31:00Z,DAY,1",
            "2025-03-01T00:00:00Z,2025-05-30T00:00:00Z,DAY,1",
            "2025-03-01T00:00:00Z,2025-05-30T00:00:01Z,MONTH,1",
            "1900-01-15T00:00:00Z,2025-06-01T00:00:00Z,MONTH,13"})
    void limitsAndClipsUtcBuckets(String from, String to, String unit, int step) throws Exception {
        String admin = "Bearer " + fixtures.signInAndGetToken("admin", "admin");

        mockMvc.perform(get(BASE + "incidents/statistics/trend").param("from", from).param("to", to)
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.bucketUnit").value(unit)).andExpect(jsonPath("$.meta.bucketStep").value(step))
                .andExpect(jsonPath("$.buckets.length()").value(lessThanOrEqualTo(120)))
                .andExpect(jsonPath("$.buckets[0].from").value(from))
                .andExpect(jsonPath("$.buckets[-1].to").value(to))
                .andExpect(jsonPath("$.buckets[0].values.created").value(0));
    }
    @Test
    void monthlyBucketsStayInUtcAcrossHistoricalTimezoneChanges() throws Exception {
        String admin = "Bearer " + fixtures.signInAndGetToken("admin", "admin");

        mockMvc.perform(get(BASE + "incidents/statistics/trend").param("from", "2000-01-01T00:00:00Z").param("to", TO)
                .header(HttpHeaders.AUTHORIZATION, admin)).andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.bucketStep").value(3))
                .andExpect(jsonPath("$.buckets[2].from").value("2000-07-01T00:00:00Z"));
    }

}
