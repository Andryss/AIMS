package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Рассмотрение и отмена заявок на заключение с проверкой полномочий.
 */
class DetentionRequestWorkflowApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/detention-requests";

    @ParameterizedTest
    @CsvSource({"NEW,prison_guard,ACCEPTED,200", "NEW,admin,ACCEPTED,200", "NEW,agent,ACCEPTED,403",
            "NEW,prison_guard,REJECTED,200", "NEW,admin,REJECTED,200", "NEW,agent,REJECTED,403",
            "NEW,agent,CANCELLED,200", "NEW,agent2,CANCELLED,403", "NEW,prison_guard,CANCELLED,200",
            "ACCEPTED,prison_guard,CANCELLED,200", "ACCEPTED,admin,CANCELLED,200",
            "ACCEPTED,agent,CANCELLED,403", "ACCEPTED,agent2,CANCELLED,403"})
    void checksTransitionsAndOwnership(String source, String actor, String target, int expected) throws Exception {
        long id = create();
        jdbcTemplate.update("update detention_request set status=? where id=?", source, id);

        change(id, actor, target, "  Причина  ").andExpect(status().is(expected));

        assertThat(historyCount(id)).isEqualTo(expected == 200 ? 2 : 1);
        assertThat(jdbcTemplate.queryForObject("select status from detention_request where id=?", String.class, id))
                .isEqualTo(expected == 200 ? target : source);
        if (expected == 200) {
            mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, token(actor)))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items[1].snapshot.request.status").value(target))
                    .andExpect(jsonPath("$.items[1].snapshot.transitionComment").value("Причина"))
                    .andExpect(jsonPath("$.items[1].changedByUserId").value(userId(actor)));
        }
    }

    @Test
    void acceptsThenCancelsBeforeArrivalPreservingReviewer() throws Exception {
        long id = create();

        change(id, "prison_guard", "ACCEPTED", null).andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedByUserId").value(userId("prison_guard")))
                .andExpect(jsonPath("$.admittedAt").doesNotExist());
        change(id, "admin", "CANCELLED", "Нет необходимости").andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewedByUserId").value(userId("prison_guard")))
                .andExpect(jsonPath("$.admittedAt").doesNotExist())
                .andExpect(jsonPath("$.releasedAt").doesNotExist());

        assertThat(historyCount(id)).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource(value = {"NEW,REJECTED,NULL", "NEW,REJECTED,''", "NEW,REJECTED,'   '",
            "NEW,CANCELLED,NULL", "NEW,CANCELLED,''", "NEW,CANCELLED,'   '",
            "ACCEPTED,CANCELLED,NULL", "ACCEPTED,CANCELLED,''", "ACCEPTED,CANCELLED,'   '"}, nullValues = "NULL")
    void terminationRequiresReason(String source, String target, String reason) throws Exception {
        long id = create();
        jdbcTemplate.update("update detention_request set status=? where id=?", source, id);

        change(id, "prison_guard", target, reason).andExpect(status().isBadRequest());

        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"NEW,NEW", "NEW,RELEASED", "NEW,IN_CUSTODY", "ACCEPTED,NEW", "ACCEPTED,ACCEPTED",
            "ACCEPTED,REJECTED", "ACCEPTED,RELEASED", "REJECTED,NEW", "REJECTED,ACCEPTED",
            "REJECTED,REJECTED", "REJECTED,CANCELLED", "CANCELLED,NEW", "CANCELLED,ACCEPTED",
            "CANCELLED,REJECTED", "CANCELLED,CANCELLED", "RELEASED,NEW", "RELEASED,ACCEPTED",
            "RELEASED,IN_CUSTODY", "RELEASED,RELEASED", "RELEASED,CANCELLED"})
    void rejectsDirectRepeatedAndBackwardTransitions(String source, String target) throws Exception {
        long id = create();
        jdbcTemplate.update("update detention_request set status=? where id=?", source, id);

        change(id, "prison_guard", target, "Причина").andExpect(status().isBadRequest());

        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "storekeeper", "tender_manager", "cleaner"})
    void unrelatedRolesCannotProcessRequests(String actor) throws Exception {
        long id = create();

        change(id, actor, "ACCEPTED", null).andExpect(status().isForbidden());

        assertThat(historyCount(id)).isEqualTo(1);
    }

    @Test
    void guardCanReadAndEditButCannotCreateRequests() throws Exception {
        long id = create();
        String guard = token("prison_guard");
        Long incident = jdbcTemplate.queryForObject("select incident_id from detention_request where id=?",
                Long.class, id);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, guard)).andExpect(status().isOk());
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, guard)
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isOk());
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, guard)
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isForbidden());
    }

    @Test
    void processingPermissionIsRequiredEvenForGuard() throws Exception {
        long id = create();
        jdbcTemplate.update("""
                delete from role_permission where role_id = (select id from role where name='PRISON_GUARD')
                and permission_id = (select id from permission where code='DETENTION_PROCESS')
                """);

        change(id, "prison_guard", "ACCEPTED", null).andExpect(status().isForbidden());

        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"status\":null}", "{\"status\":\"UNKNOWN\"}"})
    void validatesStatusPayload(String body) throws Exception {
        long id = create();

        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, token("prison_guard"))
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());

        assertThat(historyCount(id)).isEqualTo(1);
    }

    private String token(String login) throws Exception {
        String password = "prison_guard".equals(login) || "tender_manager".equals(login) ? "admin" : login;
        return "Bearer " + fixtures.signInAndGetToken(login, password);
    }

    private long userId(String login) {
        return appUserRepository.findByLogin(login).orElseThrow().getId();
    }

    private long create() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        long incident = fixtures.createIncident(operator, fixtures.uploadFile(operator));
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token("agent"))
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String body(long incident) throws Exception {
        return objectMapper.writeValueAsString(Map.of("incidentId", incident, "subjectName", "Субъект",
                "subjectDescription", "Описание", "durationDays", 30));
    }

    private ResultActions change(long id, String actor, String target, String reason) throws Exception {
        var body = new java.util.HashMap<String, Object>();
        body.put("status", target);
        body.put("comment", reason);
        return mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, token(actor))
                .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(body)));
    }

    private int historyCount(long id) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history"
                + " where entity_type='DETENTION_REQUEST' and entity_id=?", Integer.class, id);
    }
}
