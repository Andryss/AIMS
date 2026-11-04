package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.mib.aims.backend.config.TestChronoConfig;
import gov.mib.aims.backend.model.IncidentStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Уточнение основных сведений инцидента и добавление вложений оператором.
 */
class IncidentUpdateApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/incidents";

    @ParameterizedTest
    @CsvSource({"operator,DRAFT", "operator,CLARIFICATION_REQUIRED", "admin,DRAFT", "admin,CLARIFICATION_REQUIRED"})
    void updatesEditableStatesAndRecordsOneSnapshot(String login, String state) throws Exception {
        String author = token("admin");
        long id = fixtures.createIncident(author, fixtures.uploadFile(author));
        jdbcTemplate.update("update incident set status=?,updated_at=timestamp '2025-05-01' where id=?", state, id);
        String editor = token(login);
        long userId = appUserRepository.findByLogin(login).orElseThrow().getId();

        update(editor, id, request()).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(state))
                .andExpect(jsonPath("$.eventType").value("ATTACK_ON_PEOPLE"))
                .andExpect(jsonPath("$.location").value("Уточнённое место"))
                .andExpect(jsonPath("$.description").value("Уточнённое описание"))
                .andExpect(jsonPath("$.victimCount").value(2))
                .andExpect(jsonPath("$.detectedAt").value("2025-05-01T12:34:56.123456Z"))
                .andExpect(jsonPath("$.updatedAt").value(TestChronoConfig.FIXED_INSTANT.toString()));
        mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[1].changedByUserId").value(userId))
                .andExpect(jsonPath("$.items[1].snapshot.description").value("Уточнённое описание"))
                .andExpect(jsonPath("$.items[1].snapshot.victimCount").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"analyst", "agent", "storekeeper", "tender_manager", "management", "prison_guard", "cleaner"})
    void rejectsOtherRoles(String login) throws Exception {
        long id = incident();
        var before = row(id);

        update(token(login), id, request()).andExpect(status().isForbidden());
        assertThat(row(id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @EnumSource(value = IncidentStatus.class, names = {"DRAFT", "CLARIFICATION_REQUIRED"}, mode = EnumSource.Mode.EXCLUDE)
    void rejectsStatesOwnedByOtherStages(IncidentStatus state) throws Exception {
        long id = incident();
        jdbcTemplate.update("update incident set status=? where id=?", state.name(), id);
        var before = row(id);

        update(token("operator"), id, request()).andExpect(status().isConflict());
        assertThat(row(id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "0", "2", "2147483647"})
    void savesNullableVictimCount(String count) throws Exception {
        long id = incident();
        var body = request();
        body.set("victimCount", objectMapper.readTree(count));

        update(token("operator"), id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.victimCount").value("null".equals(count) ? null : Integer.valueOf(count)));
    }

    @ParameterizedTest
    @CsvSource({"victimCount,-1", "victimCount,2147483648", "victimCount,true", "eventType,\"INVALID\"",
            "location,\"\"", "description,\"\"", "location,\"   \"", "description,\"   \"",
            "additionalAttachmentFileIds,[null]"})
    void rejectsInvalidFields(String field, String value) throws Exception {
        long id = incident();
        var before = row(id);
        var body = request();
        body.set(field, objectMapper.readTree(value));

        update(token("operator"), id, body).andExpect(status().isBadRequest());
        assertThat(row(id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"eventType", "location", "detectedAt", "description"})
    void requiresOrdinaryFields(String field) throws Exception {
        long id = incident();
        var body = request();
        body.remove(field);

        update(token("operator"), id, body).andExpect(status().isBadRequest());
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"MISSING", "null", "[]"})
    void acceptsNoAdditionalFiles(String value) throws Exception {
        long id = incident();
        var body = request();
        if ("MISSING".equals(value)) {
            body.remove("additionalAttachmentFileIds");
        } else {
            body.set("additionalAttachmentFileIds", objectMapper.readTree(value));
        }

        update(token("operator"), id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentFileIds.length()").value(1));
    }

    @Test
    void appendsUniqueFilesAndPreservesFilesAddedAfterTheFormWasOpened() throws Exception {
        String editor = token("operator");
        long first = fixtures.uploadFile(editor);
        long id = fixtures.createIncident(editor, first);
        long second = fixtures.uploadFile(editor);
        long third = fixtures.uploadFile(editor);
        var body = request();
        body.putArray("additionalAttachmentFileIds").add(second).add(second).add(first);
        update(editor, id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentFileIds").value(contains((int) first, (int) second)));
        body.putArray("additionalAttachmentFileIds").add(third);

        update(editor, id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentFileIds").value(contains((int) first, (int) second, (int) third)));
        assertThat(historyCount(id)).isEqualTo(3);
        body.putArray("additionalAttachmentFileIds").add(third);
        update(editor, id, body).andExpect(status().isOk());
        assertThat(historyCount(id)).isEqualTo(3);
    }

    @ParameterizedTest
    @ValueSource(ints = {9, 10})
    void checksTheTotalAttachmentLimit(int addedCount) throws Exception {
        long id = incident();
        String editor = token("operator");
        var body = request();
        var ids = body.putArray("additionalAttachmentFileIds");
        for (int index = 0; index < addedCount; index++) {
            ids.add(fixtures.uploadFile(editor));
        }

        update(editor, id, body).andExpect(status().is(addedCount == 9 ? 200 : 400));
        assertThat(historyCount(id)).isEqualTo(addedCount == 9 ? 2 : 1);
    }

    @Test
    void handlesLegacyDuplicateFilesWithoutConsumingUniqueCapacityOrChangingNoOp() throws Exception {
        String editor = token("operator");
        long first = fixtures.uploadFile(editor);
        var create = fixtures.minimalCreateRequest(first).attachmentFileIds(java.util.Collections.nCopies(10, first));
        var response = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(create)))
                .andExpect(status().isOk()).andReturn();
        var original = objectMapper.readTree(response.getResponse().getContentAsString());
        long id = original.get("id").asLong();
        var body = request();
        body.set("eventType", original.get("eventType"));
        body.set("location", original.get("location"));
        body.set("description", original.get("description"));
        body.set("detectedAt", original.get("detectedAt"));
        body.putNull("victimCount");

        update(editor, id, body).andExpect(status().isOk());
        assertThat(historyCount(id)).isEqualTo(1);
        long second = fixtures.uploadFile(editor);
        body.putArray("additionalAttachmentFileIds").add(second);
        update(editor, id, body).andExpect(status().isOk())
                .andExpect(jsonPath("$.attachmentFileIds").value(contains((int) first, (int) second)));
        assertThat(historyCount(id)).isEqualTo(2);
    }

    @ParameterizedTest
    @CsvSource({"2025-05-01T12:34:56.123456789Z,2025-05-01T12:34:56.123456Z",
            "2025-05-01T12:34:56.999999999Z,2025-05-01T12:34:56.999999Z"})
    void normalizesStoragePrecisionBeforeComparingRepeatedUpdates(String input, String stored) throws Exception {
        long id = incident();
        String editor = token("operator");
        var body = request().put("detectedAt", input);

        update(editor, id, body).andExpect(status().isOk()).andExpect(jsonPath("$.detectedAt").value(stored));
        update(editor, id, body).andExpect(status().isOk()).andExpect(jsonPath("$.detectedAt").value(stored));
        assertThat(historyCount(id)).isEqualTo(2);
    }

    @Test
    void rejectsAnUnknownAttachmentWithoutSavingFields() throws Exception {
        long id = incident();
        var before = row(id);
        var body = request();
        body.putArray("additionalAttachmentFileIds").add(999999);

        update(token("operator"), id, body).andExpect(status().isNotFound());
        assertThat(row(id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(1);
    }

    @Test
    void keepsAnalystDecisionsAssignmentsAndCreationMetadata() throws Exception {
        long id = incident();
        Long alien = jdbcTemplate.queryForObject("select min(id) from alien", Long.class);
        Long agent = appUserRepository.findByLogin("agent").orElseThrow().getId();
        jdbcTemplate.update("update incident set alien_id=?,threat_level=8,responsible_user_id=?,"
                + "executor_user_ids=cast(? as jsonb),cleanup_status='PREPARATION' where id=?", alien, agent,
                "[" + agent + "]", id);
        Long alert = jdbcTemplate.queryForObject("""
                insert into monitoring_alert(external_event_id,source_system,status,event_type,location,
                    detected_at,description,raw_payload,incident_id)
                select 'edit-fixture','test','INCIDENT_CREATED',event_type,location,detected_at,description,
                    '{}'::jsonb,id from incident where id=? returning id
                """, Long.class, id);
        jdbcTemplate.update("update incident set monitoring_alert_id=? where id=?", alert, id);
        var before = row(id);

        update(token("operator"), id, request()).andExpect(status().isOk());
        assertThat(row(id)).containsEntry("alien_id", before.get("alien_id"))
                .containsEntry("threat_level", 8).containsEntry("responsible_user_id", agent)
                .containsEntry("executor_user_ids", before.get("executor_user_ids"))
                .containsEntry("cleanup_status", before.get("cleanup_status"))
                .containsEntry("created_by_user_id", before.get("created_by_user_id"))
                .containsEntry("created_at", before.get("created_at"))
                .containsEntry("monitoring_alert_id", before.get("monitoring_alert_id"));
    }

    @Test
    void skipsNoOpAndRefusesTheSameRequestAfterSendingToAnalysis() throws Exception {
        long id = incident();
        String editor = token("operator");
        update(editor, id, request()).andExpect(status().isOk());
        jdbcTemplate.update("update incident set updated_at=timestamp '2025-05-01' where id=?", id);
        var before = row(id);

        update(editor, id, request()).andExpect(status().isOk());
        assertThat(row(id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(2);
        mockMvc.perform(post(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + editor)
                .contentType(APPLICATION_JSON).content("{\"status\":\"READY_FOR_ANALYSIS\"}"))
                .andExpect(status().isOk());
        before = row(id);
        update(editor, id, request()).andExpect(status().isConflict());
        assertThat(row(id)).isEqualTo(before);
        assertThat(historyCount(id)).isEqualTo(3);
    }

    @Test
    void requiresAuthenticationAndReportsMissingIncident() throws Exception {
        mockMvc.perform(put(URL + "/999999").contentType(APPLICATION_JSON).content(request().toString()))
                .andExpect(status().isUnauthorized());
        update(token("operator"), 999999, request()).andExpect(status().isNotFound());
    }

    private ObjectNode request() {
        var body = (ObjectNode) objectMapper.readTree("{}");
        body.put("eventType", "ATTACK_ON_PEOPLE").put("location", " Уточнённое место ")
                .put("description", " Уточнённое описание ").put("detectedAt", "2025-05-01T12:34:56.123456Z")
                .put("victimCount", 2).putArray("additionalAttachmentFileIds");
        return body;
    }

    private long incident() throws Exception {
        String editor = token("operator");
        return fixtures.createIncident(editor, fixtures.uploadFile(editor));
    }

    private String token(String login) throws Exception {
        String password = List.of("management", "tender_manager", "prison_guard").contains(login) ? "admin" : login;
        return fixtures.signInAndGetToken(login, password);
    }

    private ResultActions update(String token, long id, ObjectNode body) throws Exception {
        return mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(APPLICATION_JSON).content(body.toString()));
    }

    private java.util.Map<String, Object> row(long id) {
        return jdbcTemplate.queryForMap("select * from incident where id=?", id);
    }

    private int historyCount(long id) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type='INCIDENT'"
                + " and entity_id=?", Integer.class, id);
    }
}
