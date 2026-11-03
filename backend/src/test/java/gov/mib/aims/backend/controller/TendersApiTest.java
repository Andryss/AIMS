package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Создание тендеров, начальные переходы и права доступа.
 */
class TendersApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/tenders";
    private static final String VALID = """
            {"equipmentType":"NEURALYZER","quantity":2,"conditions":"  Delivery 100%_!  "}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"admin", "storekeeper", "tender_manager"})
    void createsUpdatesAndAuditsTender(String login) throws Exception {
        String token = token(login);
        long id = create(token, VALID);
        String createdAt = jdbcTemplate.queryForObject("select created_at::text from tender where id = ?",
                String.class, id);
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NEED"))
                .andExpect(jsonPath("$.conditions").value("Delivery 100%_!"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.winnerParticipantId").doesNotExist())
                .andExpect(jsonPath("$.createdByUserId").value(appUserRepository.findByLogin(login).orElseThrow().getId()));
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(VALID.replace("NEURALYZER", "NEUTRALIZER")
                                .replace("Delivery 100%_!", "New conditions")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.equipmentType").value("NEUTRALIZER"));
        assertThat(jdbcTemplate.queryForObject("select created_at::text from tender where id = ?", String.class, id))
                .isEqualTo(createdAt);
        mockMvc.perform(get(URL + "/" + id + "/history").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].snapshot.tender.conditions").value("Delivery 100%_!"))
                .andExpect(jsonPath("$.items[0].snapshot.participants", hasSize(0)));
        mockMvc.perform(get(URL + "/" + id + "/history").param("size", "1").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].snapshot.tender.conditions")
                        .value("New conditions"));
    }

    @Test
    void searchesLiteralConditionsAndPaginatesWithStableFilters() throws Exception {
        String token = token("admin");
        create(token, VALID);
        create(token, VALID.replace("NEURALYZER", "NEUTRALIZER").replace("Delivery 100%_!", "Second"));
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get(URL).param("q", " %_! ").param("equipmentType", "NEURALYZER")
                        .param("status", "NEED").param("size", "1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(URL).param("size", "1").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2));
        create(token, "{\"equipmentType\":\"NEURALYZER\",\"quantity\":1}");
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "agent", "cleaner"})
    void otherRolesCannotReadOrCreateTenders(String login) throws Exception {
        String token = token(login);
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isForbidden());
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"equipmentType\":\"UNKNOWN\",\"quantity\":1}",
            "{\"equipmentType\":\"NEURALYZER\",\"quantity\":0}",
            "{\"equipmentType\":\"NEURALYZER\",\"quantity\":-1}"})
    void rejectsInvalidTender(String body) throws Exception {
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token("admin"))
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "tender_manager"})
    void managerStartsCollectionAndEditsTender(String login) throws Exception {
        String author = token("storekeeper");
        long id = create(author, VALID);
        String manager = token(login);
        change(id, manager, "COLLECTING_OFFERS", null, 200);
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, manager)
                .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isOk());
        change(id, manager, "CANCELLED", "  Purchase no longer needed  ", 200);
        mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, manager))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[3].snapshot.transitionComment")
                        .value("Purchase no longer needed"));
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, manager)
                .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isConflict());
        change(id, manager, "COLLECTING_OFFERS", null, 400);
    }

    @Test
    void storekeeperCanOnlyEditAndCancelOwnNeed() throws Exception {
        String storekeeper = token("storekeeper");
        long own = create(storekeeper, VALID);
        long other = create(token("admin"), VALID);
        mockMvc.perform(put(URL + "/" + other).header(HttpHeaders.AUTHORIZATION, storekeeper)
                .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
        change(other, storekeeper, "CANCELLED", "Reason", 403);
        change(own, storekeeper, "COLLECTING_OFFERS", null, 403);
        change(own, storekeeper, "CANCELLED", "  ", 400);
        change(own, storekeeper, "CANCELLED", "Reason", 200);
        assertThat(historyCount(own)).isEqualTo(2);
        long collecting = create(storekeeper, VALID);
        change(collecting, token("admin"), "COLLECTING_OFFERS", null, 200);
        mockMvc.perform(put(URL + "/" + collecting).header(HttpHeaders.AUTHORIZATION, storekeeper)
                .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
        change(collecting, storekeeper, "CANCELLED", "Reason", 403);
    }

    @ParameterizedTest
    @CsvSource({"NEED", "AWAITING_DELIVERY", "COMPLETED"})
    void directOrRepeatedTransitionsDoNotChangeHistory(String target) throws Exception {
        String token = token("admin");
        long id = create(token, VALID);
        change(id, token, target, null, 400);
        assertThat(historyCount(id)).isEqualTo(1);
        change(id, token, "COLLECTING_OFFERS", null, 200);
        change(id, token, "COLLECTING_OFFERS", null, 400);
        assertThat(historyCount(id)).isEqualTo(2);
    }

    @Test
    void validatesReferencesIdsAndPaging() throws Exception {
        String token = token("admin");
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(VALID.replace("\"quantity\":2", "\"quantity\":2,\"incidentId\":999999")))
                .andExpect(status().isNotFound());
        long id = create(token, VALID);
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token).contentType(APPLICATION_JSON)
                        .content(VALID.replace("\"quantity\":2", "\"quantity\":2,\"incidentId\":999999")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL + "/999999").header(HttpHeaders.AUTHORIZATION, token)).andExpect(status().isNotFound());
        mockMvc.perform(get(URL).param("page", "-1").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL).param("size", "101").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/0/history").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isBadRequest());
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, "tender_manager".equals(login) ? "admin" : login);
    }

    @Test
    void tenderRoleWithoutManagePermissionCannotStartCollection() throws Exception {
        String author = token("storekeeper");
        long id = create(author, VALID);
        jdbcTemplate.update("""
                delete from role_permission where role_id = (select id from role where name='TENDER_MANAGER')
                and permission_id = (select id from permission where code='TENDER_MANAGE')
                """);
        change(id, token("tender_manager"), "COLLECTING_OFFERS", null, 403);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @Test
    void acceptsClosedIncidentAsImmutableProcurementReason() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        long incident = fixtures.createIncident(operator, fixtures.uploadFile(operator));
        jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", incident);
        String token = token("admin");
        long id = create(token, VALID.replace("\"quantity\":2", "\"quantity\":2,\"incidentId\":" + incident));
        mockMvc.perform(get(URL).param("incidentId", String.valueOf(incident))
                        .header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(id));
        mockMvc.perform(get(URL).param("incidentId", "999999").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isBadRequest());
        assertThat(historyCount(id)).isEqualTo(1);
    }

    private long create(String token, String body) throws Exception {
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private void change(long id, String token, String target, String reason, int expected) throws Exception {
        String body = "{\"status\":\"" + target + "\"" + (reason == null ? "" : ",\"comment\":\"" + reason + "\"") + "}";
        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().is(expected));
    }

    private int historyCount(long id) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type='TENDER'"
                + " and entity_id=?", Integer.class, id);
    }
}
