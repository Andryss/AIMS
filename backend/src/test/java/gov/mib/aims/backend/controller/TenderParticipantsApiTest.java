package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Предложения участников и неизменяемые снимки тендера.
 */
class TenderParticipantsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/tenders";
    private static final String TENDER = """
            {"equipmentType":"NEURALYZER","quantity":2,"conditions":"Delivery"}
            """;
    private static final String OFFER = """
            {"name":"  Orbit  ","proposal":"  Delivery in ten days  "}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"admin", "tender_manager"})
    void managesParticipantsAndPreservesSnapshots(String login) throws Exception {
        String token = token(login);
        long tender = collecting(token);
        long first = add(tender, token, OFFER);
        long second = add(tender, token, OFFER);
        Object createdAt = jdbcTemplate.queryForObject("select created_at from tender_participant where id=?",
                Object.class, first);
        mockMvc.perform(get(participants(tender)).param("size", "1").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].id").value(second)).andExpect(jsonPath("$.items[0].name").value("Orbit"))
                .andExpect(jsonPath("$.items[0].proposal").value("Delivery in ten days"))
                .andExpect(jsonPath("$.items[0].status").value("PARTICIPATING"))
                .andExpect(jsonPath("$.items[0].createdByUserId")
                        .value(appUserRepository.findByLogin(login).orElseThrow().getId()));
        mockMvc.perform(put(participants(tender) + "/" + first).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(OFFER.replace("ten days", "five days")
                                .replace("\"name\"", "\"status\":\"WINNER\",\"name\"")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proposal").value("Delivery in five days"))
                .andExpect(jsonPath("$.status").value("PARTICIPATING"));
        assertThat(jdbcTemplate.queryForObject("select created_at from tender_participant where id=?",
                Object.class, first)).isEqualTo(createdAt);
        mockMvc.perform(put(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(TENDER.replace("Delivery", "Updated conditions")))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/" + tender + "/status").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"CANCELLED\",\"comment\":\"Reason\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL + "/" + tender + "/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(7))
                .andExpect(jsonPath("$.items[0].snapshot.participants", hasSize(0)))
                .andExpect(jsonPath("$.items[2].snapshot.participants[0].proposal").value("Delivery in ten days"))
                .andExpect(jsonPath("$.items[4].snapshot.participants[0].proposal").value("Delivery in five days"))
                .andExpect(jsonPath("$.items[5].snapshot.participants", hasSize(2)))
                .andExpect(jsonPath("$.items[6].snapshot.participants", hasSize(2)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEED", "AWAITING_DELIVERY", "COMPLETED", "CANCELLED"})
    void cannotModifyProposalsOutsideCollection(String state) throws Exception {
        String token = token("admin");
        long tender = collecting(token);
        long participant = add(tender, token, OFFER);
        jdbcTemplate.update("update tender set status=? where id=?", state, tender);
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isConflict());
        mockMvc.perform(put(participants(tender) + "/" + participant).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isConflict());
        assertThat(historyCount(tender)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"storekeeper", "operator", "analyst", "agent", "cleaner"})
    void onlyManagersCanWriteProposals(String login) throws Exception {
        String token = token(login);
        long tender = collecting(token("admin"));
        long participant = add(tender, token("admin"), OFFER);
        mockMvc.perform(get(participants(tender)).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().is("storekeeper".equals(login) ? 200 : 403));
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isForbidden());
        mockMvc.perform(put(participants(tender) + "/" + participant).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"name\":\"  \",\"proposal\":\"Valid\"}",
            "{\"name\":\"Orbit\",\"proposal\":\"  \"}"})
    void rejectsEmptyProposalFields(String body) throws Exception {
        String token = token("admin");
        long tender = collecting(token);
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(historyCount(tender)).isEqualTo(2);
    }

    @Test
    void rejectsOverlongNameAndProposal() throws Exception {
        String token = token("admin");
        long tender = collecting(token);
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(OFFER.replace("Orbit", "A".repeat(201))))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(OFFER.replace("Delivery in ten days", "A".repeat(4001))))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEED", "COLLECTING_OFFERS", "CANCELLED"})
    void rejectsForeignParticipantAndPreservesBothTenders(String state) throws Exception {
        String token = token("admin");
        long first = collecting(token);
        long second = collecting(token);
        long participant = add(first, token, OFFER);
        jdbcTemplate.update("update tender set status=? where id=?", state, second);
        mockMvc.perform(put(participants(second) + "/" + participant).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER.replace("ten days", "five days")))
                .andExpect(status().isNotFound());
        assertThat(historyCount(first)).isEqualTo(3);
        assertThat(historyCount(second)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select proposal from tender_participant where id=?",
                String.class, participant)).isEqualTo("Delivery in ten days");
    }

    @ParameterizedTest
    @CsvSource({"name,001f", "proposal,001f", "name,2003", "proposal,2003", "name,0000", "proposal,0000"})
    void rejectsFieldsBlankAfterNormalization(String field, String codePoint) throws Exception {
        String token = token("admin");
        long tender = collecting(token);
        long participant = add(tender, token, OFFER);
        String blank = Character.toString(Integer.parseInt(codePoint, 16));
        String body = objectMapper.writeValueAsString(Map.of(
                "name", "name".equals(field) ? blank : "Orbit",
                "proposal", "proposal".equals(field) ? blank : "Delivery"));
        mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(put(participants(tender) + "/" + participant).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        assertThat(historyCount(tender)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"equipmentType\":\"NEUTRALIZER\",\"quantity\":2}",
            "{\"equipmentType\":\"NEURALYZER\",\"quantity\":3}"})
    void freezesTypeAndQuantityAfterFirstOffer(String body) throws Exception {
        String token = token("admin");
        long tender = collecting(token);
        add(tender, token, OFFER);
        mockMvc.perform(put(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        assertThat(historyCount(tender)).isEqualTo(3);
    }

    @Test
    void derivesWinnerInCardAndPaginatedList() throws Exception {
        String token = token("admin");
        long tender = collecting(token);
        long winner = add(tender, token, OFFER);
        jdbcTemplate.update("update tender_participant set status='WINNER' where id=?", winner);
        jdbcTemplate.update("update tender set status='AWAITING_DELIVERY' where id=?", tender);
        collecting(token);
        mockMvc.perform(get(URL + "/" + tender).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.winnerParticipantId").value(winner));
        mockMvc.perform(get(URL).param("page", "1").param("size", "1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].winnerParticipantId").value(winner));
    }

    @Test
    void rejectsMissingTenderInvalidIdsAndPaging() throws Exception {
        String token = token("admin");
        mockMvc.perform(get(participants(999999)).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isNotFound());
        long tender = collecting(token);
        mockMvc.perform(get(participants(tender)).param("page", "-1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(participants(tender)).param("size", "101").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(participants(tender) + "/0").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(OFFER)).andExpect(status().isBadRequest());
        mockMvc.perform(get(participants(tender))).andExpect(status().isUnauthorized());
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, "tender_manager".equals(login) ? "admin" : login);
    }

    private long collecting(String token) throws Exception {
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(TENDER)).andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content("{\"status\":\"COLLECTING_OFFERS\"}"))
                .andExpect(status().isOk());
        return id;
    }

    private long add(long tender, String token, String body) throws Exception {
        var result = mockMvc.perform(post(participants(tender)).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String participants(long tender) {
        return URL + "/" + tender + "/participants";
    }

    private int historyCount(long tender) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type='TENDER'"
                + " and entity_id=?", Integer.class, tender);
    }
}
