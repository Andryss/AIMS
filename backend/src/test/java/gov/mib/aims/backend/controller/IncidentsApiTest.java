package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Incidents API (UC1).
 */
class IncidentsApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String FILES_URL = "/api/v1/files";
    private static final String INCIDENTS_URL = "/api/v1/incidents";
    private static final String UNREAD_COUNT_URL = "/api/v1/notifications/unread-count";

    @Autowired
    private NotifyAnalystsIncidentReadyProcessor notifyAnalystsProcessor;

    @Test
    void operatorCreatesIncidentAndAnalystReceivesNotificationAfterStatusChange() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(operatorToken);

        CreateIncidentRequest createRequest = new CreateIncidentRequest()
                .eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Area 51 perimeter")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 12, 0, 0, 0, ZoneOffset.UTC))
                .description("Bright object hovering")
                .attachmentFileIds(List.of(fileId));

        MvcResult createResult = mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.attachmentFileIds[0]").value(fileId))
                .andExpect(jsonPath("$.createdAt").value("2025-06-01T12:00:00Z"))
                .andExpect(jsonPath("$.updatedAt").value("2025-06-01T12:00:00Z"))
                .andReturn();

        long incidentId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get(INCIDENTS_URL + "/" + incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(incidentId))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_ANALYSIS"))
                .andExpect(jsonPath("$.updatedAt").value("2025-06-01T12:00:00Z"));

        assertQueueTasksCount(
                NotifyAnalystsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );

        notifyAnalystsProcessor.execute(new NotifyAnalystsIncidentReadyPayload(incidentId));

        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void listIncidentsReturnsPaginatedResults() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(operatorToken);
        fixtures.createIncident(operatorToken, fileId);

        mockMvc.perform(get(INCIDENTS_URL + "?page=0&size=10")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].status").value("DRAFT"));
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        CreateIncidentRequest request = fixtures.minimalCreateRequest(1L);
        mockMvc.perform(post(INCIDENTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void analystCannotCreateIncidentReturns403() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(operatorToken);
        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");

        mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fixtures.minimalCreateRequest(fileId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.access_denied"));
    }

    @Test
    void analystCannotSubmitDraftToAnalysisReturns403() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long incidentId = fixtures.createIncident(operatorToken, fixtures.uploadFile(operatorToken));
        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.insufficient_role"));
    }

    @Test
    void getUnknownIncidentReturns404() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        mockMvc.perform(get(INCIDENTS_URL + "/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("incident.not_found"));
    }

    @Test
    void createWithInvalidEventTypeReturns400() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(token);
        String body = """
                {
                  "eventType": "UNKNOWN_EVENT",
                  "location": "Test location",
                  "detectedAt": "2025-06-01T10:00:00Z",
                  "description": "Test description",
                  "attachmentFileIds": [%d]
                }
                """.formatted(fileId);

        mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void operatorResubmitsAfterClarificationReturnsToReadyForAnalysis() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long incidentId = fixtures.createIncident(operatorToken, fixtures.uploadFile(operatorToken));

        ChangeIncidentStatusRequest toAnalysis = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAnalysis)))
                .andExpect(status().isOk());

        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");
        ChangeIncidentStatusRequest clarification = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.CLARIFICATION_REQUIRED)
                .comment("Уточните место");
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(clarification)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLARIFICATION_REQUIRED"));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAnalysis)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_ANALYSIS"));
    }

    @Test
    void changeStatusWithInvalidTransitionReturns400() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        long incidentId = fixtures.createIncident(token, fixtures.uploadFile(token));

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("incident.invalid_status_transition"));
    }

}
