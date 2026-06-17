package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyExecutorsAssignedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyExecutorsAssignedProcessor;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedProcessor;
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
 * Интеграционные тесты назначения ответственных и исполнителей (UC3).
 */
class IncidentAssignmentApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String FILES_URL = "/api/v1/files";
    private static final String INCIDENTS_URL = "/api/v1/incidents";
    private static final String ALIENS_URL = "/api/v1/aliens";
    private static final String UNREAD_COUNT_URL = "/api/v1/notifications/unread-count";

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NotifyExecutorsAssignedProcessor notifyExecutorsAssignedProcessor;

    @Autowired
    private NotifyIncidentPreparedProcessor notifyIncidentPreparedProcessor;

    @Test
    void agentAssignsResponsibleAndExecutors() throws Exception {
        long incidentId = createReadyForExecutionIncident();
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        long agentId = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long agent2Id = appUserRepository.findByLogin("agent2").orElseThrow().getId();

        SetIncidentResponsibleRequest responsibleRequest = new SetIncidentResponsibleRequest().userId(agentId);
        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/responsible")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(responsibleRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.responsibleUserId").value(agentId));

        SetIncidentExecutorsRequest executorsRequest = new SetIncidentExecutorsRequest()
                .userIds(List.of(agent2Id));
        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/executors")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(executorsRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.executorUserIds[0]").value(agent2Id));

        assertQueueTasksCount(
                NotifyExecutorsAssignedPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );
        notifyExecutorsAssignedProcessor.execute(
                new NotifyExecutorsAssignedPayload(incidentId, List.of(agent2Id))
        );

        String agent2Token = fixtures.signInAndGetToken("agent2", "agent2");
        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + agent2Token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void operatorCannotAssignReturns403() throws Exception {
        long incidentId = createReadyForExecutionIncident();
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long agentId = appUserRepository.findByLogin("agent").orElseThrow().getId();

        SetIncidentResponsibleRequest request = new SetIncidentResponsibleRequest().userId(agentId);
        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/responsible")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.access_denied"));
    }

    @Test
    void assignInDraftStatusReturns400() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        long fileId = fixtures.uploadFile(operatorToken);
        long incidentId = fixtures.createIncident(operatorToken, fileId);
        long agentId = appUserRepository.findByLogin("agent").orElseThrow().getId();

        SetIncidentResponsibleRequest request = new SetIncidentResponsibleRequest().userId(agentId);
        mockMvc.perform(put(INCIDENTS_URL + "/" + incidentId + "/responsible")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("incident.invalid_assignment"));
    }

    @Test
    void preparedStatusRequiresAssignment() throws Exception {
        long incidentId = createReadyForExecutionIncident();
        String agentToken = fixtures.signInAndGetToken("agent", "agent");

        ChangeIncidentStatusRequest preparation = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.PREPARATION_FOR_EXECUTION);
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(preparation)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PREPARATION_FOR_EXECUTION"));

        ChangeIncidentStatusRequest prepared = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.PREPARED_FOR_EXECUTION);
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(prepared)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation.error"));
    }

    @Test
    void agentCompletesAssignmentAndMovesToPrepared() throws Exception {
        long incidentId = createReadyForExecutionIncident();
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        long agentId = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long agent2Id = appUserRepository.findByLogin("agent2").orElseThrow().getId();

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

        assertQueueTasksCount(
                NotifyIncidentPreparedPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );
        notifyIncidentPreparedProcessor.execute(new NotifyIncidentPreparedPayload(incidentId));
    }

    private long createReadyForExecutionIncident() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");
        long fileId = fixtures.uploadFile(operatorToken);
        long incidentId = fixtures.createIncident(operatorToken, fileId);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.READY_FOR_ANALYSIS))))
                .andExpect(status().isOk());

        long alienId = fixtures.findAlienId("Слизень", analystToken);
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

        return incidentId;
    }

}
