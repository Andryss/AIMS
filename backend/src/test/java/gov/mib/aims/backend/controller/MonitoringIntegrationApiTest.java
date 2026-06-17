package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IngestMonitoringEventRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.security.IntegrationApiKeyFilter;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyProcessor;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorsMonitoringAlertPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorsMonitoringAlertProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты inbound API мониторинга и operator flow UC1.
 */
class MonitoringIntegrationApiTest extends BaseApiTest {

    private static final String INGEST_URL = "/api/v1/integration/monitoring/events";
    private static final String ALERTS_URL = "/api/v1/monitoring/alerts";
    private static final String INCIDENTS_URL = "/api/v1/incidents";
    private static final String UNREAD_COUNT_URL = "/api/v1/notifications/unread-count";

    @Value("${aims.integration.monitoring.api-key}")
    private String integrationApiKey;

    @Autowired
    private NotifyOperatorsMonitoringAlertProcessor notifyOperatorsProcessor;

    @Autowired
    private NotifyAnalystsIncidentReadyProcessor notifyAnalystsProcessor;

    @Test
    void ingestCreatesAlertAndRejectsDuplicateExternalEventId() throws Exception {
        IngestMonitoringEventRequest request = sampleIngestRequest("evt-uc1-001");

        mockMvc.perform(post(INGEST_URL)
                        .header(IntegrationApiKeyFilter.API_KEY_HEADER, integrationApiKey)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.externalEventId", is("evt-uc1-001")))
                .andExpect(jsonPath("$.status", is("NEW")));

        mockMvc.perform(post(INGEST_URL)
                        .header(IntegrationApiKeyFilter.API_KEY_HEADER, integrationApiKey)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("monitoring_alert.duplicate_external_event")));
    }

    @Test
    void ingestRejectsInvalidApiKey() throws Exception {
        IngestMonitoringEventRequest request = sampleIngestRequest("evt-uc1-unauthorized");

        mockMvc.perform(post(INGEST_URL)
                        .header(IntegrationApiKeyFilter.API_KEY_HEADER, "wrong-key")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void nonOperatorCannotListMonitoringAlerts() throws Exception {
        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALERTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void operatorFlowFromAlertToAnalystNotification() throws Exception {
        IngestMonitoringEventRequest ingestRequest = sampleIngestRequest("evt-uc1-flow-001");
        MvcResult ingestResult = mockMvc.perform(post(INGEST_URL)
                        .header(IntegrationApiKeyFilter.API_KEY_HEADER, integrationApiKey)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(ingestRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        long alertId = objectMapper.readTree(ingestResult.getResponse().getContentAsString()).get("id").asLong();

        assertQueueTasksCount(
                NotifyOperatorsMonitoringAlertPayload.QUEUE_NAME,
                String.valueOf(alertId),
                1);
        notifyOperatorsProcessor.execute(new NotifyOperatorsMonitoringAlertPayload(alertId));

        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        mockMvc.perform(get(ALERTS_URL)
                        .param("status", "NEW")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id == " + alertId + ")]", hasSize(1)));

        mockMvc.perform(get(UNREAD_COUNT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        long fileId = fixtures.uploadFile(operatorToken);
        CreateIncidentRequest createRequest = new CreateIncidentRequest()
                .eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Area 51 perimeter")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .description("Unidentified object near the fence")
                .attachmentFileIds(List.of(fileId))
                .monitoringAlertId(alertId);

        MvcResult createResult = mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monitoringAlertId", is((int) alertId)))
                .andReturn();
        long incidentId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(get(ALERTS_URL + "/" + alertId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INCIDENT_CREATED")))
                .andExpect(jsonPath("$.incidentId", is((int) incidentId)));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.READY_FOR_ANALYSIS))))
                .andExpect(status().isOk());

        assertQueueTasksCount(
                NotifyAnalystsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1);
        notifyAnalystsProcessor.execute(new NotifyAnalystsIncidentReadyPayload(incidentId));

        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(UNREAD_COUNT_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    private IngestMonitoringEventRequest sampleIngestRequest(String externalEventId) {
        return new IngestMonitoringEventRequest()
                .externalEventId(externalEventId)
                .sourceSystem("EXTERNAL_MONITORING_V1")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .location("Nevada desert sector 7")
                .eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .description("Thermal anomaly detected by external sensors")
                .mediaUrls(List.of(URI.create("https://example.com/evidence/photo1.jpg")));
    }
}
