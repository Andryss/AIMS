package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Данные заявок на заключение, права доступа, поиск и независимая история.
 */
class DetentionRequestsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/detention-requests";

    @ParameterizedTest
    @ValueSource(strings = {"agent", "admin"})
    void createsUpdatesAndAuditsRequest(String login) throws Exception {
        String actor = token(login);
        long incident = incident();
        long id = create(actor, incident);
        Object createdAt = jdbcTemplate.queryForObject("select created_at from detention_request where id=?",
                Object.class, id);

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.subjectName").value("Субъект %_!"))
                .andExpect(jsonPath("$.subjectDescription").value("Задержан у вокзала"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.createdByUserId").value(userId(login)))
                .andExpect(jsonPath("$.admittedAt").doesNotExist())
                .andExpect(jsonPath("$.plannedReleaseAt").doesNotExist())
                .andExpect(jsonPath("$.releasedAt").doesNotExist())
                .andExpect(jsonPath("$.reviewedByUserId").doesNotExist())
                .andExpect(jsonPath("$.custodyStatus").doesNotExist());

        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, actor)
                .contentType(APPLICATION_JSON).content(body(incident).replace("30", "15")
                        .replace("Задержан у вокзала", "Уточнённые данные")
                        .replace("}", ",\"comment\":\"  Новый срок  \"}")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.durationDays").value(15));

        assertThat(jdbcTemplate.queryForObject("select created_at from detention_request where id=?",
                Object.class, id)).isEqualTo(createdAt);
        mockMvc.perform(get(URL + "/" + id + "/history").param("size", "1")
                .header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].snapshot.request.durationDays").value(30));
        mockMvc.perform(get(URL + "/" + id + "/history").param("size", "1").param("page", "1")
                .header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].snapshot.request.durationDays").value(15))
                .andExpect(jsonPath("$.items[0].snapshot.transitionComment").value("Новый срок"));
    }

    @Test
    void searchesLiteralSubjectDataAndCombinesFilters() throws Exception {
        String actor = token("agent");
        long firstIncident = incident();
        long secondIncident = incident();
        long first = create(actor, firstIncident);
        long second = create(actor, firstIncident);
        create(actor, secondIncident);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.items[0].id").isNumber()).andExpect(jsonPath("$.totalElements").value(3));
        mockMvc.perform(get(URL).param("q", " %_! ").param("status", "NEW")
                .param("incidentId", Long.toString(firstIncident)).param("size", "1")
                .header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(second))
                .andExpect(jsonPath("$.totalElements").value(2));
        mockMvc.perform(get(URL).param("q", "ВОКЗАЛА").param("incidentId", Long.toString(firstIncident))
                .param("page", "1").param("size", "1").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(first));
        mockMvc.perform(get(URL).param("status", "RELEASED").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void preservesOwnSubjectAndSpeciesWhenIncidentChanges() throws Exception {
        String actor = token("agent");
        long incident = incident();
        Long alien = jdbcTemplate.queryForObject("select min(id) from alien", Long.class);
        jdbcTemplate.update("update incident set alien_id=? where id=?", alien, incident);
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, actor).contentType(APPLICATION_JSON)
                .content(body(incident).replace("}", ",\"alienId\":" + alien + "}")))
                .andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        jdbcTemplate.update("update incident set alien_id=null where id=?", incident);

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alienId").value(alien));
        mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].snapshot.request.alienId").value(alien));
        create(actor, incident);
    }

    @Test
    void restrictsEditingAndRequiresReasonForChangedDuration() throws Exception {
        String author = token("agent");
        long incident = incident();
        long id = create(author, incident);

        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, token("agent2"))
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isForbidden());
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, author)
                .contentType(APPLICATION_JSON).content(body(incident).replace("30", "15")))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, author)
                .contentType(APPLICATION_JSON).content(body(incident())))
                .andExpect(status().isBadRequest());

        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "storekeeper", "tender_manager", "cleaner"})
    void otherRolesCannotAccessRequests(String login) throws Exception {
        String actor = token(login);
        long incident = incident();
        long id = create(token("agent"), incident);

        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, actor)).andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, actor)
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isForbidden());
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, actor)
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"durationDays\":0}", "{\"subjectName\":\" \"}",
            "{\"subjectDescription\":\" \"}", "{\"durationDays\":-1}", "{\"incidentId\":0}",
            "{\"alienId\":-1}", "{\"durationDays\":null}"})
    void rejectsInvalidFields(String overrides) throws Exception {
        var data = objectMapper.readTree(body(incident()));
        var fields = objectMapper.readTree(overrides).fields();
        fields.forEachRemaining(entry -> ((com.fasterxml.jackson.databind.node.ObjectNode) data)
                .set(entry.getKey(), entry.getValue()));
        if ("{}".equals(overrides)) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) data).remove("subjectName");
        }

        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token("agent"))
                .contentType(APPLICATION_JSON).content(data.toString())).andExpect(status().isBadRequest());
    }

    @Test
    void checksReferencesAndCompletedIncident() throws Exception {
        String actor = token("agent");
        long incident = incident();
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, actor).contentType(APPLICATION_JSON)
                .content(body(999999))).andExpect(status().isNotFound());
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, actor).contentType(APPLICATION_JSON)
                .content(body(incident).replace("}", ",\"alienId\":999999}"))).andExpect(status().isNotFound());
        jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", incident);

        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, actor)
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isConflict());
    }

    @ParameterizedTest
    @ValueSource(strings = {"page=-1", "size=0", "size=101", "status=UNKNOWN", "incidentId=0"})
    void validatesListParameters(String parameter) throws Exception {
        String[] parts = parameter.split("=");

        mockMvc.perform(get(URL).param(parts[0], parts[1]).header(HttpHeaders.AUTHORIZATION, token("agent")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validatesIdentifiersAndAuthentication() throws Exception {
        String actor = token("agent");
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(get(URL + "/0").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(URL + "/999999").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL + "/999999/history").header(HttpHeaders.AUTHORIZATION, actor))
                .andExpect(status().isNotFound());
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, "tender_manager".equals(login) ? "admin" : login);
    }

    private long userId(String login) {
        return appUserRepository.findByLogin(login).orElseThrow().getId();
    }

    private long incident() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        return fixtures.createIncident(operator, fixtures.uploadFile(operator));
    }

    private long create(String token, long incident) throws Exception {
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body(incident))).andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String body(long incident) throws Exception {
        return objectMapper.writeValueAsString(Map.of("incidentId", incident, "subjectName", "  Субъект %_!  ",
                "subjectDescription", "  Задержан у вокзала  ", "durationDays", 30));
    }

    private int historyCount(long id) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history"
                + " where entity_type='DETENTION_REQUEST' and entity_id=?", Integer.class, id);
    }
}
