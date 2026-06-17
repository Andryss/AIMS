package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.IncidentStatusApi;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyPayload;
import gov.mib.aims.backend.support.ApiTestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Проверяет, что задачи db-queue не попадают в БД при неуспешной смене статуса (rollback транзакции).
 */
class DbQueueTransactionalEnqueueApiTest extends BaseApiTest {

    private static final String INCIDENTS_URL = ApiTestFixtures.INCIDENTS_URL;

    @Test
    void invalidStatusTransitionDoesNotEnqueueTask() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(operatorToken);
        long incidentId = fixtures.createIncident(operatorToken, fileId);

        ChangeIncidentStatusRequest toAnalysis = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAnalysis)))
                .andExpect(status().isOk());

        assertQueueTasksCount(
                NotifyAnalystsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );

        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAnalysis)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("incident.invalid_status_transition"));

        assertQueueTasksCount(
                NotifyAnalystsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                1
        );
    }

    @Test
    void insufficientRoleDoesNotEnqueueTask() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");
        long fileId = fixtures.uploadFile(operatorToken);
        long incidentId = fixtures.createIncident(operatorToken, fileId);

        assertQueueTasksCount(
                NotifyAnalystsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                0
        );

        String analystToken = fixtures.signInAndGetToken("analyst", "analyst");
        ChangeIncidentStatusRequest toAnalysis = new ChangeIncidentStatusRequest()
                .status(IncidentStatusApi.READY_FOR_ANALYSIS);
        mockMvc.perform(post(INCIDENTS_URL + "/" + incidentId + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + analystToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(toAnalysis)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("auth.insufficient_role"));

        assertQueueTasksCount(
                NotifyAnalystsIncidentReadyPayload.QUEUE_NAME,
                String.valueOf(incidentId),
                0
        );
    }
}
