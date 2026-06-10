package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyProcessor;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyProcessor;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredProcessor;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты привязки инопланетянина и UC2-статусов.
 */
class IncidentAlienApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String FILES_URL = "/api/v1/files";
    private static final String INCIDENTS_URL = "/api/v1/incidents";
    private static final String ALIENS_URL = "/api/v1/aliens";
    private static final String UNREAD_COUNT_URL = "/api/v1/notifications/unread-count";

    @Autowired
    private NotifyAnalystsIncidentReadyProcessor notifyAnalystsProcessor;

    @Autowired
    private NotifyAgentsIncidentReadyProcessor notifyAgentsProcessor;

    @Autowired
    private NotifyOperatorClarificationRequiredProcessor notifyOperatorClarificationProcessor;

    @Test
    void analystLinksAlienOperatorForbidden() throws Exception {
        long incidentId = createReadyForAnalysisIncident();
        long alienId = findAlienId("Слизень");
        String analystToken = signInAndGetToken("analyst", "analyst");
        String operatorToken = signInAndGetToken("operator", "operator");

        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/alien")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alienId\": " + alienId + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alienId").value(alienId));

        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/alien")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alienId\": " + alienId + "}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.access_denied"));
    }

    @Test
    void operatorCannotChangeToAnalysisStatusReturns400() throws Exception {
        String operatorToken = signInAndGetToken("operator", "operator");
        long incidentId = createReadyForAnalysisIncident();

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_EXECUTION);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("incident.invalid_status_transition"));
    }

    @Test
    void readyForExecutionWithoutAlienReturns400() throws Exception {
        long incidentId = createReadyForAnalysisIncident();
        String analystToken = signInAndGetToken("analyst", "analyst");

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_EXECUTION);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("incident.invalid_status_transition"));
    }

    @Test
    void analystMovesToReadyForExecutionAgentReceivesNotification() throws Exception {
        long incidentId = createReadyForAnalysisIncident();
        long alienId = findAlienId("Слизень");
        String analystToken = signInAndGetToken("analyst", "analyst");

        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/alien")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alienId\": " + alienId + "}"))
                .andExpect(status().isOk());

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_EXECUTION);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY_FOR_EXECUTION"));

        assertQueueTasksCount(
                NotifyAgentsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );

        notifyAgentsProcessor.execute(new NotifyAgentsIncidentReadyPayload(incidentId));

        String agentToken = signInAndGetToken("agent", "agent");
        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void clarificationRequiredNotifiesIncidentCreator() throws Exception {
        long incidentId = createReadyForAnalysisIncident();
        String analystToken = signInAndGetToken("analyst", "analyst");
        String operatorToken = signInAndGetToken("operator", "operator");

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.CLARIFICATION_REQUIRED);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLARIFICATION_REQUIRED"));

        assertQueueTasksCount(
                NotifyOperatorClarificationRequiredPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );

        notifyOperatorClarificationProcessor.execute(
                new NotifyOperatorClarificationRequiredPayload(incidentId)
        );

        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    private long createReadyForAnalysisIncident() throws Exception {
        String operatorToken = signInAndGetToken("operator", "operator");
        long fileId = uploadFile(operatorToken);
        long incidentId = createIncident(operatorToken, fileId);

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        notifyAnalystsProcessor.execute(new NotifyAnalystsIncidentReadyPayload(incidentId));
        return incidentId;
    }

    private long findAlienId(String name) throws Exception {
        String analystToken = signInAndGetToken("analyst", "analyst");
        MvcResult result = mockMvc.perform(get(ALIENS_URL + "/search").param("q", name)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("items").get(0).get("id").asLong();
    }

    private long createIncident(String token, long fileId) throws Exception {
        CreateIncidentRequest request = new CreateIncidentRequest()
                .eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Test location")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .description("Test description")
                .attachmentFileIds(List.of(fileId));
        MvcResult result = mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
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
