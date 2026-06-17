package gov.mib.aims.backend.support;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Общие хелперы для MockMvc API-тестов: sign-in, upload, создание инцидента и подготовленного инцидента.
 */
public final class ApiTestFixtures {

    public static final String SIGNIN_URL = "/api/v1/auth/signin";
    public static final String FILES_URL = "/api/v1/files";
    public static final String INCIDENTS_URL = "/api/v1/incidents";
    public static final String ALIENS_URL = "/api/v1/aliens";
    public static final MediaType APPLICATION_JSON = MediaType.APPLICATION_JSON;

    private final MockMvc mockMvc;
    private final ObjectMapperWrapper objectMapper;
    private final AppUserRepository appUserRepository;

    public ApiTestFixtures(
            MockMvc mockMvc,
            ObjectMapperWrapper objectMapper,
            AppUserRepository appUserRepository
    ) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.appUserRepository = appUserRepository;
    }

    /**
     * Выполняет sign-in и возвращает JWT access token.
     */
    public String signInAndGetToken(String login, String password) throws Exception {
        SignInRequest request = new SignInRequest().login(login).password(password);
        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }

    /**
     * Загружает тестовый файл и возвращает его id.
     */
    public long uploadFile(String token) throws Exception {
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

    /**
     * Создаёт инцидент в статусе DRAFT.
     */
    public long createIncident(String token, long fileId) throws Exception {
        MvcResult result = mockMvc.perform(post(INCIDENTS_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(minimalCreateRequest(fileId))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    /**
     * Проводит инцидент по цепочке до PREPARED_FOR_EXECUTION.
     */
    public long createPreparedIncident() throws Exception {
        String operatorToken = signInAndGetToken("operator", "operator");
        String analystToken = signInAndGetToken("analyst", "analyst");
        String agentToken = signInAndGetToken("agent", "agent");
        long fileId = uploadFile(operatorToken);
        long incidentId = createIncident(operatorToken, fileId);
        long agentId = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long agent2Id = appUserRepository.findByLogin("agent2").orElseThrow().getId();

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.READY_FOR_ANALYSIS))))
                .andExpect(status().isOk());

        long alienId = findAlienId("Слизень", analystToken);
        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/alien")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content("{\"alienId\": " + alienId + "}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.READY_FOR_EXECUTION))))
                .andExpect(status().isOk());

        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/responsible")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SetIncidentResponsibleRequest().userId(agentId))))
                .andExpect(status().isOk());

        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/executors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new SetIncidentExecutorsRequest().userIds(List.of(agent2Id)))))
                .andExpect(status().isOk());

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.PREPARATION_FOR_EXECUTION))))
                .andExpect(status().isOk());

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.PREPARED_FOR_EXECUTION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARED_FOR_EXECUTION"));

        return incidentId;
    }

    /**
     * Ищет id пришельца по имени через search API.
     */
    public long findAlienId(String name, String analystToken) throws Exception {
        MvcResult result = mockMvc.perform(get(ALIENS_URL + "/search?q=" + name)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("items").get(0).get("id").asLong();
    }

    /**
     * Минимальный запрос на создание инцидента для тестов.
     */
    public CreateIncidentRequest minimalCreateRequest(long fileId) {
        return new CreateIncidentRequest()
                .eventType(IncidentEventTypeApi.UNIDENTIFIED_SIGHTING)
                .location("Test location")
                .detectedAt(OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC))
                .description("Test description")
                .attachmentFileIds(List.of(fileId));
    }
}
