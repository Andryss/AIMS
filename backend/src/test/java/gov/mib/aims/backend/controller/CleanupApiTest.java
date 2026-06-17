package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeCleanupStatusRequest;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CleanupStatusApi;
import gov.mib.aims.backend.generated.model.CreateCleanupReportRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedProcessor;
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
 * Интеграционные тесты UC4: отчёт и статус очистки.
 */
class CleanupApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String FILES_URL = "/api/v1/files";
    private static final String INCIDENTS_URL = "/api/v1/incidents";
    private static final String ALIENS_URL = "/api/v1/aliens";
    private static final String UNREAD_COUNT_URL = "/api/v1/notifications/unread-count";

    @Autowired
    private NotifyCleanupCompletedProcessor notifyCleanupCompletedProcessor;

    @Test
    void responsibleAgentMovesToExecutingAndExecutionCompleted() throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        String agentToken = fixtures.signInAndGetToken("agent", "agent");

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.EXECUTING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTING"));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.EXECUTION_COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTION_COMPLETED"));
    }

    @Test
    void nonResponsibleAgentCannotMoveToExecuting() throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        String agent2Token = fixtures.signInAndGetToken("agent2", "agent2");

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agent2Token)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.EXECUTING))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.insufficient_role"));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.EXECUTING))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EXECUTING"));
    }

    @Test
    void getCleanupReportReturns404WhenMissing() throws Exception {
        long incidentId = createExecutingIncident();
        String cleanerToken = fixtures.signInAndGetToken("cleaner", "cleaner");

        mockMvc.perform(get(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("cleanup_report.not_found"));
    }

    @Test
    void cleanerCreatesAndReadsCleanupReport() throws Exception {
        long incidentId = createExecutingIncident();
        String cleanerToken = fixtures.signInAndGetToken("cleaner", "cleaner");
        long fileId = fixtures.uploadFile(cleanerToken);

        CreateCleanupReportRequest createRequest = new CreateCleanupReportRequest()
                .description("Площадка очищена")
                .attachmentFileIds(List.of(fileId));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Площадка очищена"))
                .andExpect(jsonPath("$.attachmentFileIds[0]").value(fileId))
                .andExpect(jsonPath("$.incidentId").value(incidentId));

        mockMvc.perform(get(INCIDENTS_URL + "/" + incidentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanupReportId", not(emptyOrNullString())));

        mockMvc.perform(get(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Площадка очищена"));
    }

    @Test
    void duplicateCleanupReportReturns409() throws Exception {
        long incidentId = createExecutingIncident();
        String cleanerToken = fixtures.signInAndGetToken("cleaner", "cleaner");
        long fileId = fixtures.uploadFile(cleanerToken);

        CreateCleanupReportRequest createRequest = new CreateCleanupReportRequest()
                .description("Первый отчёт")
                .attachmentFileIds(List.of(fileId));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("cleanup_report.already_exists"));
    }

    @Test
    void operatorCannotCreateCleanupReportReturns403() throws Exception {
        long incidentId = createExecutingIncident();
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(operatorToken);

        CreateCleanupReportRequest createRequest = new CreateCleanupReportRequest()
                .description("Отчёт")
                .attachmentFileIds(List.of(fileId));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.access_denied"));
    }

    @Test
    void cleanupStatusWorkflowAndNotification() throws Exception {
        long incidentId = createExecutingIncident();
        String cleanerToken = fixtures.signInAndGetToken("cleaner", "cleaner");
        long fileId = fixtures.uploadFile(cleanerToken);

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeCleanupStatusRequest().status(CleanupStatusApi.PREPARATION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanupStatus").value("PREPARATION"));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeCleanupStatusRequest().status(CleanupStatusApi.EXECUTION))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanupStatus").value("EXECUTION"));

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeCleanupStatusRequest().status(CleanupStatusApi.COMPLETED))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation.error"));

        CreateCleanupReportRequest createRequest = new CreateCleanupReportRequest()
                .description("Готово")
                .attachmentFileIds(List.of(fileId));
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-report")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated());

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/cleanup-status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + cleanerToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeCleanupStatusRequest().status(CleanupStatusApi.COMPLETED))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cleanupStatus").value("COMPLETED"));

        assertQueueTasksCount(
                NotifyCleanupCompletedPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );
        notifyCleanupCompletedProcessor.execute(new NotifyCleanupCompletedPayload(incidentId));

        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
    }

    private long createExecutingIncident() throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new ChangeIncidentStatusRequest().status(IncidentStatusApi.EXECUTING))))
                .andExpect(status().isOk());
        return incidentId;
    }

}
