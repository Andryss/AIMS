package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Map;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Заявки на выдачу и возврат оборудования.
 */
class EquipmentMovementsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/equipment-movements";
    private String authorToken;
    private String warehouseToken;
    private long incidentId;
    private long holderId;
    private long equipmentId;

    @BeforeEach
    void prepareWarehouseAndIncident() throws Exception {
        authorToken = fixtures.signInAndGetToken("operator", "operator");
        warehouseToken = fixtures.signInAndGetToken("storekeeper", "storekeeper");
        incidentId = fixtures.createIncident(authorToken, fixtures.uploadFile(authorToken));
        holderId = appUserRepository.findByLogin("agent").orElseThrow().getId();
        equipmentId = register("SN-001");
    }

    @Test
    void createIssueLeavesEquipmentOnShelfAndRecordsHistory() throws Exception {
        long id = createIssue(equipmentId);
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.responsibleUserId").value(holderId))
                .andExpect(jsonPath("$.incidentId").value(incidentId))
                .andExpect(jsonPath("$.equipment[0].serialNumber").value("SN-001"))
                .andExpect(jsonPath("$.completedAt").doesNotExist());
        assertThat(jdbcTemplate.queryForObject("select status from equipment_item where id = ?",
                String.class, equipmentId)).isEqualTo("AVAILABLE");
        mockMvc.perform(get(URL + "/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].snapshot.movement.status").value("PENDING"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "storekeeper", "admin"})
    void pendingRequestCanBeEditedByAuthorOrStorekeeper(String login) throws Exception {
        long id = createIssue(equipmentId);
        String token = fixtures.signInAndGetToken(login, login);
        long other = register("SN-002");
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body("ISSUE", null, List.of(other))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.equipment[0].id").value(other));
        mockMvc.perform(get(URL + "/" + id + "/history").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].snapshot.movement.equipment[0].id").value(equipmentId));
    }

    @Test
    void unrelatedAuthorCannotEditAndIncidentCannotBeChanged() throws Exception {
        long id = createIssue(equipmentId);
        String agent = fixtures.signInAndGetToken("agent", "agent");
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + agent)
                        .contentType(APPLICATION_JSON).content(body("ISSUE", null, List.of(equipmentId))))
                .andExpect(status().isForbidden());
        long otherIncident = fixtures.createIncident(authorToken, fixtures.uploadFile(authorToken));
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(body("ISSUE", null, List.of(equipmentId))
                                .replace("\"incidentId\":" + incidentId, "\"incidentId\":" + otherIncident)))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"empty", "duplicate", "missing", "source", "operation", "incident", "holder"})
    void invalidRequestIsRejectedWithoutCreatingHistory(String invalid) throws Exception {
        String request = switch (invalid) {
            case "empty" -> body("ISSUE", null, List.of());
            case "duplicate" -> body("ISSUE", null, List.of(equipmentId, equipmentId));
            case "missing" -> body("ISSUE", null, List.of(999999L));
            case "source" -> body("RETURN", null, List.of(equipmentId));
            case "operation" -> body("UNKNOWN", null, List.of(equipmentId));
            case "incident" -> body("ISSUE", null, List.of(equipmentId))
                    .replace("\"incidentId\":" + incidentId, "\"incidentId\":999999");
            case "holder" -> body("ISSUE", null, List.of(equipmentId))
                    .replace("\"responsibleUserId\":" + holderId, "\"responsibleUserId\":999999");
            default -> throw new IllegalArgumentException(invalid);
        };
        int expected = List.of("missing", "incident", "holder").contains(invalid) ? 404 : 400;
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(request)).andExpect(status().is(expected));
        assertThat(jdbcTemplate.queryForObject("select count(*) from equipment_movement", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type = ?",
                Integer.class, "EQUIPMENT_MOVEMENT")).isZero();
    }

    @Test
    void movementListFiltersAndPages() throws Exception {
        long id = createIssue(equipmentId);
        mockMvc.perform(get(URL).param("incidentId", Long.toString(incidentId))
                        .param("equipmentId", Long.toString(equipmentId)).param("status", "PENDING")
                        .param("operation", "ISSUE").param("responsibleUserId", Long.toString(holderId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.page").value(0)).andExpect(jsonPath("$.size").value(20));
        mockMvc.perform(get(URL).param("status", "COMPLETED")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void paginationCountsOnlyMatchingMovements() throws Exception {
        createIssue(equipmentId);
        long newest = createIssue(equipmentId);
        createIssue(register("OTHER-001"));
        mockMvc.perform(get(URL).param("equipmentId", Long.toString(equipmentId)).param("size", "1")
                        .param("status", "PENDING").param("incidentId", Long.toString(incidentId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(2)).andExpect(jsonPath("$.items[0].id").value(newest));
        mockMvc.perform(get(URL).param("equipmentId", Long.toString(equipmentId))
                        .param("size", "1").param("page", "3")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

    @Test
    void movementHistoryPreservesEarlierItemDetails() throws Exception {
        long id = createIssue(equipmentId);
        mockMvc.perform(put("/api/v1/equipment/" + equipmentId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON).content("""
                                {"type":"NEURALYZER","model":"Renamed","serialNumber":"SN-RENAMED"}
                                """))
                .andExpect(status().isOk());
        mockMvc.perform(get(URL + "/" + id + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].snapshot.movement.equipment[0].serialNumber").value("SN-001"));
    }

    @Test
    void issueAndPartialReturnsUpdateHoldersAndHistory() throws Exception {
        long second = register("SN-002");
        long issue = createIssue(equipmentId, second);
        complete(issue);
        assertThat(jdbcTemplate.queryForObject("select holder_user_id from equipment_item where id = ?",
                Long.class, equipmentId)).isEqualTo(holderId);
        long firstReturn = createReturn(issue, equipmentId);
        complete(firstReturn);
        mockMvc.perform(get("/api/v1/equipment").param("holderUserId", Long.toString(holderId))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(second));
        complete(createReturn(issue, second));
        assertThat(jdbcTemplate.queryForObject("select count(*) from equipment_item where status = 'ISSUED'",
                Integer.class)).isZero();
        mockMvc.perform(get("/api/v1/equipment/" + equipmentId + "/history")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[1].snapshot.event").value("ISSUED"))
                .andExpect(jsonPath("$.items[1].snapshot.movementId").value(issue))
                .andExpect(jsonPath("$.items[2].snapshot.event").value("RETURNED"))
                .andExpect(jsonPath("$.items[2].snapshot.movementId").value(firstReturn));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CANCELLED", "REJECTED"})
    void cancellationAndRejectionRequireReasonAndDoNotMoveEquipment(String target) throws Exception {
        long id = createIssue(equipmentId);
        mockMvc.perform(patch(URL + "/" + id + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"" + target + "\",\"comment\":\"  \"}"))
                .andExpect(status().isBadRequest());
        change(id, target, warehouseToken, 200);
        assertThat(jdbcTemplate.queryForObject("select status from equipment_item where id = ?",
                String.class, equipmentId)).isEqualTo("AVAILABLE");
        mockMvc.perform(get(URL + "/" + id + "/history").param("page", "1").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].snapshot.transitionComment").value("Причина"));
    }

    @ParameterizedTest
    @CsvSource({
            "PENDING,PROCESSING,200", "PENDING,COMPLETED,400", "PENDING,PENDING,400",
            "PENDING,CANCELLED,200", "PENDING,REJECTED,200", "PROCESSING,COMPLETED,200",
            "PROCESSING,CANCELLED,200", "PROCESSING,REJECTED,200", "PROCESSING,PENDING,400",
            "PROCESSING,PROCESSING,400", "COMPLETED,COMPLETED,400", "COMPLETED,CANCELLED,400",
            "CANCELLED,PROCESSING,400", "REJECTED,PROCESSING,400"
    })
    void statusTransitionsFollowLifecycle(String from, String target, int expected) throws Exception {
        long id = createIssue(equipmentId);
        if (from.equals("PROCESSING") || from.equals("COMPLETED")) {
            change(id, "PROCESSING", warehouseToken, 200);
        }
        if (!from.equals("PENDING") && !from.equals("PROCESSING")) {
            change(id, from, warehouseToken, 200);
        }
        int before = jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class);
        change(id, target, warehouseToken, expected);
        if (expected == 400) {
            assertThat(jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class))
                    .isEqualTo(before);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "agent", "cleaner"})
    void onlyWarehouseCanProcessMovement(String login) throws Exception {
        long id = createIssue(equipmentId);
        change(id, "PROCESSING", fixtures.signInAndGetToken(login, login), 403);
    }

    @Test
    void authorCanCancelButAnotherEmployeeCannot() throws Exception {
        long id = createIssue(equipmentId);
        change(id, "CANCELLED", fixtures.signInAndGetToken("agent", "agent"), 403);
        change(id, "CANCELLED", authorToken, 200);
    }

    @Test
    void processingAndTerminalRequestsCannotBeEdited() throws Exception {
        long id = createIssue(equipmentId);
        change(id, "PROCESSING", warehouseToken, 200);
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON).content(body("ISSUE", null, List.of(equipmentId))))
                .andExpect(status().isBadRequest());
        change(id, "COMPLETED", warehouseToken, 200);
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(body("ISSUE", null, List.of(equipmentId))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void oneInvalidItemRollsBackWholeMovement() throws Exception {
        long second = register("SN-002");
        long combined = createIssue(equipmentId, second);
        long competing = createIssue(second);
        complete(competing);
        change(combined, "PROCESSING", warehouseToken, 200);
        int before = jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class);
        change(combined, "COMPLETED", warehouseToken, 409);
        assertThat(jdbcTemplate.queryForObject("select status from equipment_movement where id = ?",
                String.class, combined)).isEqualTo("PROCESSING");
        assertThat(jdbcTemplate.queryForObject("select status from equipment_item where id = ?",
                String.class, equipmentId)).isEqualTo("AVAILABLE");
        assertThat(jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class)).isEqualTo(before);
    }

    @Test
    void returnCannotCloseAnOlderIssueAfterReissue() throws Exception {
        long issue = createIssue(equipmentId);
        complete(issue);
        long staleReturn = createReturn(issue, equipmentId);
        complete(createReturn(issue, equipmentId));
        long nextIssue = createIssue(equipmentId);
        complete(nextIssue);
        change(staleReturn, "PROCESSING", warehouseToken, 200);
        change(staleReturn, "COMPLETED", warehouseToken, 409);
        assertThat(jdbcTemplate.queryForObject("select current_issue_movement_id from equipment_item where id = ?",
                Long.class, equipmentId)).isEqualTo(nextIssue);
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(body("RETURN", issue, List.of(equipmentId))))
                .andExpect(status().isConflict());
    }

    @Test
    void returnMustMatchCompletedIssueAndHolder() throws Exception {
        long issue = createIssue(equipmentId);
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(body("RETURN", issue, List.of(equipmentId))))
                .andExpect(status().isBadRequest());
        complete(issue);
        long otherHolder = appUserRepository.findByLogin("analyst").orElseThrow().getId();
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(body("RETURN", issue, List.of(equipmentId))
                                .replace("\"responsibleUserId\":" + holderId,
                                        "\"responsibleUserId\":" + otherHolder)))
                .andExpect(status().isBadRequest());
    }

    private void change(long id, String target, String token, int expected) throws Exception {
        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(
                                Map.of("status", target, "comment", "  Причина  "))))
                .andExpect(status().is(expected));
    }

    private void complete(long id) throws Exception {
        change(id, "PROCESSING", warehouseToken, 200);
        change(id, "COMPLETED", warehouseToken, 200);
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.completedAt").isNotEmpty());
    }

    private long createReturn(long issueId, long itemId) throws Exception {
        return id(mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON).content(body("RETURN", issueId, List.of(itemId))))
                .andExpect(status().isCreated()).andReturn());
    }

    private long register(String serial) throws Exception {
        return id(mockMvc.perform(post("/api/v1/equipment")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + warehouseToken)
                        .contentType(APPLICATION_JSON).content(objectMapper.writeValueAsString(Map.of(
                                "type", "NEURALYZER", "model", "Model A", "serialNumber", serial))))
                .andExpect(status().isCreated()).andReturn());
    }

    private long createIssue(long... itemIds) throws Exception {
        return id(mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + authorToken)
                        .contentType(APPLICATION_JSON)
                        .content(body("ISSUE", null, Arrays.stream(itemIds).boxed().toList())))
                .andExpect(status().isCreated()).andReturn());
    }

    private String body(String operation, Long sourceId, List<Long> items) {
        return "{\"operation\":\"" + operation + "\",\"incidentId\":" + incidentId
                + ",\"responsibleUserId\":" + holderId + ",\"sourceIssueMovementId\":" + sourceId
                + ",\"equipmentIds\":" + objectMapper.writeValueAsString(items) + "}";
    }

    private long id(MvcResult result) throws Exception {
        return json(result).get("id").asLong();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
