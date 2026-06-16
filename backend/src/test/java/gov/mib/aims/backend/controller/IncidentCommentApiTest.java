package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentCommentRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.repository.AppUserRepository;
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
 * Интеграционные тесты комментариев и истории инцидента.
 */
class IncidentCommentApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String FILES_URL = "/api/v1/files";
    private static final String INCIDENTS_URL = "/api/v1/incidents";

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void analystCreatesAndListsCommentsOldestFirst() throws Exception {
        String operatorToken = signInAndGetToken("operator", "operator");
        String analystToken = signInAndGetToken("analyst", "analyst");
        long incidentId = createDraftIncident(operatorToken);
        long analystUserId = appUserRepository.findByLogin("analyst").orElseThrow().getId();

        CreateIncidentCommentRequest first = new CreateIncidentCommentRequest().text("Первый комментарий");
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUserId").value(analystUserId))
                .andExpect(jsonPath("$.text").value("Первый комментарий"));

        CreateIncidentCommentRequest second = new CreateIncidentCommentRequest().text("Второй комментарий");
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(second)))
                .andExpect(status().isCreated());

        mockMvc.perform(get(INCIDENTS_URL + "/" + incidentId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].text").value("Первый комментарий"))
                .andExpect(jsonPath("$.items[1].text").value("Второй комментарий"))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void agentCanCreateComment() throws Exception {
        String operatorToken = signInAndGetToken("operator", "operator");
        String agentToken = signInAndGetToken("agent", "agent");
        long incidentId = createDraftIncident(operatorToken);
        long agentUserId = appUserRepository.findByLogin("agent").orElseThrow().getId();

        CreateIncidentCommentRequest request = new CreateIncidentCommentRequest().text("Agent note");
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.authorUserId").value(agentUserId));
    }

    @Test
    void createCommentForMissingIncidentReturns404() throws Exception {
        String analystToken = signInAndGetToken("analyst", "analyst");
        CreateIncidentCommentRequest request = new CreateIncidentCommentRequest().text("Ghost");
        mockMvc.perform(post(INCIDENTS_URL + "/999999/comments")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("incident.not_found"));
    }

    @Test
    void historyReturnsChronologicalEntriesWithUserId() throws Exception {
        String operatorToken = signInAndGetToken("operator", "operator");
        long operatorUserId = appUserRepository.findByLogin("operator").orElseThrow().getId();
        long incidentId = createDraftIncident(operatorToken);

        ChangeIncidentStatusRequest statusRequest = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusRequest)))
                .andExpect(status().isOk());

        mockMvc.perform(get(INCIDENTS_URL + "/" + incidentId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].changedByUserId").value(operatorUserId))
                .andExpect(jsonPath("$.items[0].snapshot.status").value("DRAFT"))
                .andExpect(jsonPath("$.items[1].changedByUserId").value(operatorUserId))
                .andExpect(jsonPath("$.items[1].snapshot.status").value("READY_FOR_ANALYSIS"));
    }

    private long createDraftIncident(String operatorToken) throws Exception {
        long fileId = uploadFile(operatorToken);
        CreateIncidentRequest request = new CreateIncidentRequest()
                .eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Test location")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .description("Test description")
                .attachmentFileIds(List.of(fileId));
        MvcResult result = mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
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
