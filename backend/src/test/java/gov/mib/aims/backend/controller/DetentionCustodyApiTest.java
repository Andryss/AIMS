package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.config.TestClockConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Фактические даты заключения, изменение срока и ручное освобождение.
 */
class DetentionCustodyApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/detention-requests";

    @ParameterizedTest
    @ValueSource(strings = {"prison_guard", "admin"})
    void admitsWithServerTimeAndReleasesWithReason(String actor) throws Exception {
        long id = accepted();
        String now = TestClockConfig.FIXED_INSTANT.toString();
        String planned = TestClockConfig.FIXED_INSTANT.plus(30, ChronoUnit.DAYS).toString();

        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, token(actor))
                .contentType(APPLICATION_JSON).content("""
                        {"status":"IN_CUSTODY","admittedAt":"2000-01-01T00:00:00Z",
                         "reviewedByUserId":999999,"releasedAt":"2000-02-01T00:00:00Z"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.admittedAt").value(now))
                .andExpect(jsonPath("$.plannedReleaseAt").value(planned))
                .andExpect(jsonPath("$.releasedAt").doesNotExist())
                .andExpect(jsonPath("$.reviewedByUserId").value(userId("prison_guard")));

        change(id, actor, "RELEASED", "  Основания отпали  ").andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RELEASED"))
                .andExpect(jsonPath("$.admittedAt").value(now)).andExpect(jsonPath("$.releasedAt").value(now))
                .andExpect(jsonPath("$.plannedReleaseAt").value(planned))
                .andExpect(jsonPath("$.reviewedByUserId").value(userId("prison_guard")));

        mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, token(actor)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(4))
                .andExpect(jsonPath("$.items[1].snapshot.request.admittedAt").doesNotExist())
                .andExpect(jsonPath("$.items[2].snapshot.request.status").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.items[2].snapshot.request.releasedAt").doesNotExist())
                .andExpect(jsonPath("$.items[3].snapshot.request.status").value("RELEASED"))
                .andExpect(jsonPath("$.items[3].snapshot.transitionComment").value("Основания отпали"))
                .andExpect(jsonPath("$.items[3].changedByUserId").value(userId(actor)));
    }

    @ParameterizedTest
    @CsvSource({"IN_CUSTODY", "CANCELLED", "ACCEPTED", "NEW", "REJECTED"})
    void cannotReadmitCancelOrReturnFromCustody(String target) throws Exception {
        long id = admitted();
        change(id, "prison_guard", target, "Причина").andExpect(status().isBadRequest());

        assertThat(historyCount(id)).isEqualTo(3);
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("agent")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.admittedAt").value(TestClockConfig.FIXED_INSTANT.toString()));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\u0000", "\u0008", "\u001f"})
    void earlyReleaseRequiresNonblankReason(String reason) throws Exception {
        long id = admitted();

        change(id, "prison_guard", "RELEASED", reason).andExpect(status().isBadRequest());

        assertThat(historyCount(id)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("select released_at from detention_request where id=?",
                Object.class, id)).isNull();
    }

    @ParameterizedTest
    @CsvSource({"29,400", "30,200", "31,200"})
    void checksReleaseDeadlineWithoutAutomaticTransition(int daysAgo, int expected) throws Exception {
        long id = admitted();
        Instant admission = TestClockConfig.FIXED_INSTANT.minus(daysAgo, ChronoUnit.DAYS);
        jdbcTemplate.update("update detention_request set admitted_at=? where id=?",
                java.sql.Timestamp.from(admission), id);

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("prison_guard")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.plannedReleaseAt").value(admission.plus(30, ChronoUnit.DAYS).toString()));
        change(id, "prison_guard", "RELEASED", null).andExpect(status().is(expected));

        assertThat(historyCount(id)).isEqualTo(expected == 200 ? 4 : 3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ACCEPTED", "IN_CUSTODY"})
    void guardEditsAcceptedRequestAndAuthorCannot(String source) throws Exception {
        long id = "IN_CUSTODY".equals(source) ? admitted() : accepted();
        long incident = incidentId(id);
        String body = body(incident, 15, "  Уточнён срок  ");

        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("agent"))
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("admin"))
                .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationDays").value(15))
                .andExpect(jsonPath("$.status").value(source))
                .andExpect(jsonPath("$.reviewedByUserId").value(userId("prison_guard")));
    }

    @Test
    void shorteningDurationKeepsAdmissionAndCanMakeRequestOverdue() throws Exception {
        long id = admitted();
        Instant admission = TestClockConfig.FIXED_INSTANT.minus(10, ChronoUnit.DAYS);
        jdbcTemplate.update("update detention_request set admitted_at=? where id=?",
                java.sql.Timestamp.from(admission), id);

        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("prison_guard"))
                .contentType(APPLICATION_JSON).content(body(incidentId(id), 5, "Пересмотр срока")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.admittedAt").value(admission.toString()))
                .andExpect(jsonPath("$.plannedReleaseAt").value(admission.plus(5, ChronoUnit.DAYS).toString()))
                .andExpect(jsonPath("$.releasedAt").doesNotExist());
        change(id, "prison_guard", "RELEASED", null).andExpect(status().isOk());
    }

    @Test
    void changingDurationRequiresReasonAndIgnoresClientDates() throws Exception {
        long id = admitted();
        long incident = incidentId(id);
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("prison_guard"))
                .contentType(APPLICATION_JSON).content(body(incident, 15, null)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("prison_guard"))
                .contentType(APPLICATION_JSON).content(body(incident, 30, null).replace("}", """
                        ,"admittedAt":"2000-01-01T00:00:00Z","createdByUserId":999999,
                        "status":"RELEASED","releasedAt":"2000-01-02T00:00:00Z"}
                        """)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("IN_CUSTODY"))
                .andExpect(jsonPath("$.createdByUserId").value(userId("agent")))
                .andExpect(jsonPath("$.admittedAt").value(TestClockConfig.FIXED_INSTANT.toString()))
                .andExpect(jsonPath("$.releasedAt").doesNotExist());

        assertThat(historyCount(id)).isEqualTo(4);
    }

    @Test
    void continuesExistingRequestAfterIncidentCompletion() throws Exception {
        long id = create();
        jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", incidentId(id));

        change(id, "prison_guard", "ACCEPTED", null).andExpect(status().isOk());
        change(id, "prison_guard", "IN_CUSTODY", null).andExpect(status().isOk());
        change(id, "prison_guard", "RELEASED", "Досрочное освобождение").andExpect(status().isOk());

        assertThat(historyCount(id)).isEqualTo(4);
    }

    @ParameterizedTest
    @ValueSource(strings = {"RELEASED", "REJECTED", "CANCELLED"})
    void terminalRequestsRemainReadableButCannotBeEdited(String source) throws Exception {
        long id = create();
        jdbcTemplate.update("update detention_request set status=? where id=?", source, id);

        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("prison_guard"))
                .contentType(APPLICATION_JSON).content(body(incidentId(id), 30, null)))
                .andExpect(status().isConflict());
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("agent2")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(source));

        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"ACCEPTED,IN_CUSTODY", "IN_CUSTODY,RELEASED"})
    void agentCannotManageActualCustody(String source, String target) throws Exception {
        long id = "IN_CUSTODY".equals(source) ? admitted() : accepted();
        int history = historyCount(id);

        change(id, "agent", target, "Причина").andExpect(status().isForbidden());

        assertThat(historyCount(id)).isEqualTo(history);
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, "prison_guard".equals(login) ? "admin" : login);
    }

    private long userId(String login) {
        return appUserRepository.findByLogin(login).orElseThrow().getId();
    }

    private long incidentId(long id) {
        return jdbcTemplate.queryForObject("select incident_id from detention_request where id=?", Long.class, id);
    }

    private long create() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        long incident = fixtures.createIncident(operator, fixtures.uploadFile(operator));
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token("agent"))
                .contentType(APPLICATION_JSON).content(body(incident, 30, null)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long accepted() throws Exception {
        long id = create();
        change(id, "prison_guard", "ACCEPTED", null).andExpect(status().isOk());
        return id;
    }

    private long admitted() throws Exception {
        long id = accepted();
        change(id, "prison_guard", "IN_CUSTODY", null).andExpect(status().isOk());
        return id;
    }

    private String body(long incident, int days, String reason) throws Exception {
        var data = new HashMap<String, Object>(Map.of("incidentId", incident, "subjectName", "Субъект",
                "subjectDescription", "Описание", "durationDays", days));
        data.put("comment", reason);
        return objectMapper.writeValueAsString(data);
    }

    private ResultActions change(long id, String actor, String target, String reason) throws Exception {
        var body = new HashMap<String, Object>();
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
