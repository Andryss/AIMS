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
 * Выбор победителя, переходы тендера и неизменность истории при отказах.
 */
class TenderWorkflowApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/tenders";
    private static final String TENDER = """
            {"equipmentType":"NEURALYZER","quantity":2,"conditions":"Delivery"}
            """;
    private static final String OFFER = """
            {"name":"Orbit","proposal":"Delivery in five days"}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"admin", "tender_manager"})
    void selectsWinnerAndRecordsOneCompleteSnapshot(String login) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long first = add(tender, admin);
        long winner = add(tender, admin);
        Object createdAt = jdbcTemplate.queryForObject("select created_at from tender_participant where id=?",
                Object.class, winner);
        String manager = token(login);

        select(tender, winner, manager).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_DELIVERY"))
                .andExpect(jsonPath("$.winnerParticipantId").value(winner))
                .andExpect(jsonPath("$.completedAt").doesNotExist());

        mockMvc.perform(get(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.winnerParticipantId").value(winner));
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].winnerParticipantId").value(winner));
        mockMvc.perform(get(participants(tender)).header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(first))
                .andExpect(jsonPath("$.items[0].status").value("PARTICIPATING"))
                .andExpect(jsonPath("$.items[1].id").value(winner))
                .andExpect(jsonPath("$.items[1].status").value("WINNER"));
        mockMvc.perform(get(URL + "/" + tender + "/history").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.items[3].snapshot.tender.status").value("COLLECTING_OFFERS"))
                .andExpect(jsonPath("$.items[3].snapshot.tender.winnerParticipantId").doesNotExist())
                .andExpect(jsonPath("$.items[3].snapshot.participants[1].status").value("PARTICIPATING"))
                .andExpect(jsonPath("$.items[4].snapshot.tender.status").value("AWAITING_DELIVERY"))
                .andExpect(jsonPath("$.items[4].snapshot.tender.winnerParticipantId").value(winner))
                .andExpect(jsonPath("$.items[4].snapshot.participants[0].status").value("PARTICIPATING"))
                .andExpect(jsonPath("$.items[4].snapshot.participants[1].status").value("WINNER"))
                .andExpect(jsonPath("$.items[4].changedByUserId")
                        .value(appUserRepository.findByLogin(login).orElseThrow().getId()));
        assertThat(jdbcTemplate.queryForObject("select created_at from tender_participant where id=?",
                Object.class, winner)).isEqualTo(createdAt);
        assertThat(jdbcTemplate.queryForObject("select updated_at from tender_participant where id=?",
                Object.class, winner)).isEqualTo(jdbcTemplate.queryForObject(
                        "select updated_at from tender where id=?", Object.class, tender));
        assertThat(winnerCount(tender)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"storekeeper", "operator", "analyst", "agent", "cleaner"})
    void onlyManagePermissionAllowsWinnerSelection(String login) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long participant = add(tender, admin);

        select(tender, participant, token(login)).andExpect(status().isForbidden());

        assertThat(historyCount(tender)).isEqualTo(3);
        assertThat(winnerCount(tender)).isZero();
        assertStatus(tender, "COLLECTING_OFFERS");
    }

    @Test
    void managerWithoutManagePermissionCannotSelectWinner() throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long participant = add(tender, admin);
        jdbcTemplate.update("""
                delete from role_permission where role_id = (select id from role where name='TENDER_MANAGER')
                and permission_id = (select id from permission where code='TENDER_MANAGE')
                """);

        select(tender, participant, token("tender_manager")).andExpect(status().isForbidden());

        assertThat(historyCount(tender)).isEqualTo(3);
        assertThat(winnerCount(tender)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEED", "AWAITING_DELIVERY", "COMPLETED", "CANCELLED"})
    void winnerSelectionRequiresCollection(String source) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long participant = add(tender, admin);
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);

        select(tender, participant, admin).andExpect(status().isConflict());

        assertThat(historyCount(tender)).isEqualTo(3);
        assertThat(winnerCount(tender)).isZero();
        assertStatus(tender, source);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void cannotRepeatSelectionOrReplaceWinner(boolean sameParticipant) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long winner = add(tender, admin);
        long other = add(tender, admin);
        select(tender, winner, admin).andExpect(status().isOk());

        select(tender, sameParticipant ? winner : other, admin).andExpect(status().isConflict());

        assertThat(historyCount(tender)).isEqualTo(5);
        assertThat(winnerCount(tender)).isEqualTo(1);
        mockMvc.perform(get(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.winnerParticipantId").value(winner));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEED", "COLLECTING_OFFERS", "AWAITING_DELIVERY", "COMPLETED", "CANCELLED"})
    void rejectsMissingAndForeignParticipantsWithoutChanges(String source) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        add(tender, admin);
        long other = collecting(admin);
        long foreign = add(other, admin);
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);

        select(tender, foreign, admin).andExpect(status().isNotFound());
        select(tender, 999999, admin).andExpect(status().isNotFound());
        select(999999, foreign, admin).andExpect(status().isNotFound());

        assertThat(historyCount(tender)).isEqualTo(3);
        assertThat(historyCount(other)).isEqualTo(3);
        assertThat(winnerCount(tender)).isZero();
        assertThat(winnerCount(other)).isZero();
        assertStatus(tender, source);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"participantId\":null}", "{\"participantId\":0}",
            "{\"participantId\":-1}", "{\"participantId\":\"invalid\"}"})
    void validatesWinnerRequest(String body) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        add(tender, admin);

        mockMvc.perform(post(URL + "/" + tender + "/winner").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());

        assertThat(historyCount(tender)).isEqualTo(3);
        assertThat(winnerCount(tender)).isZero();
    }

    @Test
    void validatesWinnerPathAndRequiresAuthentication() throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long participant = add(tender, admin);

        select(0, participant, admin).andExpect(status().isBadRequest());
        mockMvc.perform(post(URL + "/" + tender + "/winner").contentType(APPLICATION_JSON)
                .content("{\"participantId\":" + participant + "}")).andExpect(status().isUnauthorized());

        assertThat(historyCount(tender)).isEqualTo(3);
    }

    @ParameterizedTest
    @CsvSource({"NEED,storekeeper,storekeeper,CANCELLED,200", "NEED,admin,storekeeper,CANCELLED,403",
            "NEED,storekeeper,admin,COLLECTING_OFFERS,200", "NEED,storekeeper,tender_manager,COLLECTING_OFFERS,200",
            "NEED,storekeeper,storekeeper,COLLECTING_OFFERS,403", "NEED,storekeeper,admin,CANCELLED,200",
            "NEED,storekeeper,tender_manager,CANCELLED,200", "COLLECTING_OFFERS,storekeeper,admin,CANCELLED,200",
            "COLLECTING_OFFERS,storekeeper,tender_manager,CANCELLED,200",
            "COLLECTING_OFFERS,storekeeper,storekeeper,CANCELLED,403",
            "AWAITING_DELIVERY,storekeeper,admin,CANCELLED,200",
            "AWAITING_DELIVERY,storekeeper,tender_manager,CANCELLED,200",
            "AWAITING_DELIVERY,storekeeper,storekeeper,CANCELLED,403"})
    void checksTransitionsAndCancellationOwnership(String source, String author, String actor,
                                                   String target, int expected) throws Exception {
        long tender = create(token(author));
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);
        String actingToken = token(actor);

        change(tender, actingToken, target, "  No longer needed  ").andExpect(status().is(expected));

        assertThat(historyCount(tender)).isEqualTo(expected == 200 ? 2 : 1);
        assertStatus(tender, expected == 200 ? target : source);
        if (expected == 200 && "CANCELLED".equals(target)) {
            mockMvc.perform(get(URL + "/" + tender + "/history").header(HttpHeaders.AUTHORIZATION, actingToken))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.items[1].snapshot.transitionComment")
                            .value("No longer needed"));
        }
    }

    @ParameterizedTest
    @CsvSource(value = {"NEED,NULL", "NEED,''", "NEED,'   '", "COLLECTING_OFFERS,NULL",
            "COLLECTING_OFFERS,''", "COLLECTING_OFFERS,'   '", "AWAITING_DELIVERY,NULL",
            "AWAITING_DELIVERY,''", "AWAITING_DELIVERY,'   '"}, nullValues = "NULL")
    void cancellationRequiresReason(String source, String reason) throws Exception {
        String admin = token("admin");
        long tender = create(admin);
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);

        change(tender, admin, "CANCELLED", reason).andExpect(status().isBadRequest());

        assertThat(historyCount(tender)).isEqualTo(1);
        assertStatus(tender, source);
    }

    @ParameterizedTest
    @CsvSource({"NEED,0000", "NEED,0008", "NEED,001f", "COLLECTING_OFFERS,0000",
            "COLLECTING_OFFERS,0008", "COLLECTING_OFFERS,001f", "AWAITING_DELIVERY,0000",
            "AWAITING_DELIVERY,0008", "AWAITING_DELIVERY,001f"})
    void cancellationRejectsReasonBlankAfterNormalization(String source, String codePoint) throws Exception {
        String admin = token("admin");
        long tender = create(admin);
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);
        String reason = Character.toString(Integer.parseInt(codePoint, 16));
        String body = objectMapper.writeValueAsString(Map.of("status", "CANCELLED", "comment", reason));

        mockMvc.perform(patch(URL + "/" + tender + "/status").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());

        assertThat(historyCount(tender)).isEqualTo(1);
        assertStatus(tender, source);
    }

    @ParameterizedTest
    @CsvSource({"NEED,NEED", "NEED,AWAITING_DELIVERY", "NEED,COMPLETED",
            "COLLECTING_OFFERS,NEED", "COLLECTING_OFFERS,COLLECTING_OFFERS",
            "COLLECTING_OFFERS,AWAITING_DELIVERY", "COLLECTING_OFFERS,COMPLETED",
            "AWAITING_DELIVERY,NEED", "AWAITING_DELIVERY,COLLECTING_OFFERS",
            "AWAITING_DELIVERY,AWAITING_DELIVERY", "AWAITING_DELIVERY,COMPLETED",
            "COMPLETED,NEED", "COMPLETED,COLLECTING_OFFERS", "COMPLETED,AWAITING_DELIVERY",
            "COMPLETED,COMPLETED", "COMPLETED,CANCELLED", "CANCELLED,NEED", "CANCELLED,COLLECTING_OFFERS",
            "CANCELLED,AWAITING_DELIVERY", "CANCELLED,COMPLETED", "CANCELLED,CANCELLED"})
    void rejectsDirectRepeatedAndBackwardTransitions(String source, String target) throws Exception {
        String admin = token("admin");
        long tender = create(admin);
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);

        change(tender, admin, target, "Reason").andExpect(status().isBadRequest());

        assertThat(historyCount(tender)).isEqualTo(1);
        assertStatus(tender, source);
    }

    @ParameterizedTest
    @ValueSource(strings = {"AWAITING_DELIVERY", "CANCELLED", "COMPLETED"})
    void freezesTenderAndProposalsAfterSelectionAndKeepsReadingAvailable(String target) throws Exception {
        String admin = token("admin");
        long tender = collecting(admin);
        long participant = add(tender, admin);
        select(tender, participant, admin).andExpect(status().isOk());
        if ("CANCELLED".equals(target)) {
            change(tender, admin, target, "Reason").andExpect(status().isOk());
        } else if ("COMPLETED".equals(target)) {
            jdbcTemplate.update("update tender set status='COMPLETED' where id=?", tender);
        }
        int history = historyCount(tender);

        mockMvc.perform(put(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(TENDER)).andExpect(status().isConflict());
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isConflict());
        mockMvc.perform(put(participants(tender) + "/" + participant).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isConflict());
        select(tender, participant, admin).andExpect(status().isConflict());

        String reader = token("storekeeper");
        mockMvc.perform(get(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.winnerParticipantId").value(participant));
        mockMvc.perform(get(participants(tender)).header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].status").value("WINNER"));
        mockMvc.perform(get(URL + "/" + tender + "/history").header(HttpHeaders.AUTHORIZATION, reader))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(history));
        assertThat(historyCount(tender)).isEqualTo(history);
        assertThat(winnerCount(tender)).isEqualTo(1);
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, "tender_manager".equals(login) ? "admin" : login);
    }

    private long create(String token) throws Exception {
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(TENDER)).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long collecting(String token) throws Exception {
        long tender = create(token);
        change(tender, token, "COLLECTING_OFFERS", null).andExpect(status().isOk());
        return tender;
    }

    private long add(long tender, String token) throws Exception {
        var result = mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private ResultActions select(long tender, long participant, String token) throws Exception {
        return mockMvc.perform(post(URL + "/" + tender + "/winner").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content("{\"participantId\":" + participant + "}"));
    }

    private ResultActions change(long tender, String token, String target, String reason) throws Exception {
        String body = "{\"status\":\"" + target + "\"" + (reason == null ? "" : ",\"comment\":\"" + reason + "\"") + "}";
        return mockMvc.perform(patch(URL + "/" + tender + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body));
    }

    private String participants(long tender) {
        return URL + "/" + tender + "/participants";
    }

    private int historyCount(long tender) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type='TENDER'"
                + " and entity_id=?", Integer.class, tender);
    }

    private int winnerCount(long tender) {
        return jdbcTemplate.queryForObject("select count(*) from tender_participant where tender_id=?"
                + " and status='WINNER'", Integer.class, tender);
    }

    private void assertStatus(long tender, String expected) {
        assertThat(jdbcTemplate.queryForObject("select status from tender where id=?", String.class, tender))
                .isEqualTo(expected);
    }
}
