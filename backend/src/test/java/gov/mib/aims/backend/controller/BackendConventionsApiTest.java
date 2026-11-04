package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.CreateIncidentCommentRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IngestMonitoringEventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Общие ограничения входных данных для создания и изменения инцидента.
 */
class BackendConventionsApiTest extends BaseApiTest {

    @ParameterizedTest
    @ValueSource(strings = {"location", "description"})
    void rejectsWhitespaceOnlyIncidentFieldsOnCreate(String field) throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(token);
        var request = new CreateIncidentRequest().eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Nevada").description("Anomaly").detectedAt(OffsetDateTime.parse("2025-05-01T00:00:00Z"))
                .attachmentFileIds(List.of(fileId));
        if ("location".equals(field)) {
            request.location(" \t\n ");
        } else {
            request.description(" \t\n ");
        }

        mockMvc.perform(post("/api/v1/incidents").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("select count(*) from incident", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class)).isZero();
    }

    @Test
    void rejectsWhitespaceOnlyComment() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long id = fixtures.createIncident(token, fixtures.uploadFile(token));

        mockMvc.perform(post("/api/v1/incidents/" + id + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateIncidentCommentRequest().text(" \t "))))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("select count(*) from incident_comment", Integer.class)).isZero();
    }

    @Test
    void rejectsWhitespaceOnlyExternalMonitoringIdBeforeEnqueue() throws Exception {
        var request = new IngestMonitoringEventRequest().externalEventId(" \t ")
                .sourceSystem("EXTERNAL_MONITORING_V1").eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Nevada").description("Anomaly")
                .detectedAt(OffsetDateTime.parse("2025-05-01T00:00:00Z"));

        mockMvc.perform(post("/api/v1/integration/monitoring/events")
                        .header("X-Integration-Api-Key", "test-monitoring-api-key").contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        assertThat(jdbcTemplate.queryForObject("select count(*) from monitoring_alert", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from queue_tasks", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"incidents", "aliens", "equipment", "equipment-movements", "detention-requests",
            "tenders", "notifications", "monitoring/alerts"})
    void appliesSharedPagingContract(String resource) throws Exception {
        String login = "monitoring/alerts".equals(resource) ? "operator" : "admin";
        String token = fixtures.signInAndGetToken(login, login);
        String url = "/api/v1/" + resource;

        mockMvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20));
        mockMvc.perform(get(url).param("size", "0").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ordersSameTimestampHistoryByIdAcrossPageBoundaries() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long id = fixtures.createIncident(token, fixtures.uploadFile(token));
        long userId = appUserRepository.findByLogin("operator").orElseThrow().getId();
        String snapshot = jdbcTemplate.queryForObject(
                "select snapshot::text from entity_history where entity_type='INCIDENT' and entity_id=?",
                String.class, id);
        jdbcTemplate.update("delete from entity_history where entity_type='INCIDENT' and entity_id=?", id);
        String sql = """
                insert into entity_history(id,entity_type,entity_id,snapshot,changed_by_user_id,changed_at)
                values (?,'INCIDENT',?,cast(? as jsonb),?,timestamptz '2025-06-01T00:00:00Z')
                """;
        jdbcTemplate.update(sql, 20L, id, snapshot, userId);
        jdbcTemplate.update(sql, 10L, id, snapshot, userId);

        String url = "/api/v1/incidents/" + id + "/history";
        mockMvc.perform(get(url).param("page", "0").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].id").value(10));
        mockMvc.perform(get(url).param("page", "1").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(20));
    }
}
