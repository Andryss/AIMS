package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
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
        String operatorToken = signInAndGetToken("agent", "secret");
        long fileId = uploadFile(operatorToken);

        CreateIncidentRequest createRequest = new CreateIncidentRequest()
                .eventType(1)
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

        String analystToken = signInAndGetToken("analyst", "secret");
        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void createWithoutTokenReturns401() throws Exception {
        CreateIncidentRequest request = minimalCreateRequest(1L);
        mockMvc.perform(post(INCIDENTS_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void analystCannotCreateIncidentReturns403() throws Exception {
        String operatorToken = signInAndGetToken("agent", "secret");
        long fileId = uploadFile(operatorToken);
        String analystToken = signInAndGetToken("analyst", "secret");

        mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalCreateRequest(fileId))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.access_denied"));
    }

    @Test
    void analystCannotChangeStatusReturns403() throws Exception {
        String operatorToken = signInAndGetToken("agent", "secret");
        long incidentId = createIncident(operatorToken, uploadFile(operatorToken));
        String analystToken = signInAndGetToken("analyst", "secret");

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.access_denied"));
    }

    @Test
    void getUnknownIncidentReturns404() throws Exception {
        String token = signInAndGetToken("agent", "secret");
        mockMvc.perform(get(INCIDENTS_URL + "/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("incident.not_found"));
    }

    @Test
    void createWithInvalidEventTypeReturns400() throws Exception {
        String token = signInAndGetToken("agent", "secret");
        long fileId = uploadFile(token);
        CreateIncidentRequest request = minimalCreateRequest(fileId).eventType(999);

        mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("incident.invalid_event_type"));
    }

    @Test
    void changeStatusWithInvalidTransitionReturns400() throws Exception {
        String token = signInAndGetToken("agent", "secret");
        long incidentId = createIncident(token, uploadFile(token));

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

    private long createIncident(String token, long fileId) throws Exception {
        MvcResult result = mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalCreateRequest(fileId))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private CreateIncidentRequest minimalCreateRequest(long fileId) {
        return new CreateIncidentRequest()
                .eventType(1)
                .location("Test location")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .description("Test description")
                .attachmentFileIds(List.of(fileId));
    }

    private long uploadFile(String token) throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "evidence.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                "photo".getBytes()
        );
        MvcResult uploadResult = mockMvc.perform(multipart(FILES_URL)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andReturn();
        return objectMapper.readTree(uploadResult.getResponse().getContentAsString()).get("id").asLong();
    }

    private String signInAndGetToken(String login, String password) throws Exception {
        SignInRequest request = new SignInRequest().login(login).password(password);
        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
