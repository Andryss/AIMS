package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
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
 * Полная приёмка поставки, происхождение оборудования и откат при конфликтах.
 */
class TenderReceiptsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/tenders";
    private static final String RECEIPT = """
            {"equipment":[{"model":"  Model A  ","serialNumber":"  SN-A  ","type":"NEUTRALIZER"},
                          {"model":"Model B","serialNumber":"SN-B"}],"comment":"  Received in full  "}
            """;

    @ParameterizedTest
    @CsvSource({"admin,NEURALYZER", "storekeeper,NEURALYZER", "tender_manager,NEURALYZER",
            "storekeeper,NEUTRALIZER"})
    void receivesFullDeliveryWithEquipmentAndBothHistories(String login, String type) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, type);
        String receiver = token(login);

        var result = receive(tender, receiver, RECEIPT).andExpect(status().isOk())
                .andExpect(jsonPath("$.tender.status").value("COMPLETED"))
                .andExpect(jsonPath("$.tender.winnerParticipantId").isNumber())
                .andExpect(jsonPath("$.tender.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.equipment", hasSize(2)))
                .andExpect(jsonPath("$.equipment[0].model").value("Model A"))
                .andExpect(jsonPath("$.equipment[0].serialNumber").value("SN-A"))
                .andExpect(jsonPath("$.equipment[0].type").value(type))
                .andExpect(jsonPath("$.equipment[0].status").value("AVAILABLE"))
                .andExpect(jsonPath("$.equipment[0].sourceTenderId").value(tender))
                .andExpect(jsonPath("$.equipment[0].holderUserId").doesNotExist())
                .andExpect(jsonPath("$.equipment[0].currentIssueMovementId").doesNotExist())
                .andExpect(jsonPath("$.equipment[0].createdAt").isNotEmpty())
                .andExpect(jsonPath("$.equipment[1].sourceTenderId").value(tender))
                .andExpect(jsonPath("$.equipment[1].type").value(type)).andReturn();
        var response = objectMapper.readTree(result.getResponse().getContentAsString());
        long first = response.get("equipment").get(0).get("id").asLong();
        long second = response.get("equipment").get(1).get("id").asLong();
        assertThat(response.get("tender").get("updatedAt")).isEqualTo(response.get("tender").get("completedAt"));

        mockMvc.perform(get(URL + "/" + tender + "/history").header(HttpHeaders.AUTHORIZATION, receiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.items[3].snapshot.tender.status").value("AWAITING_DELIVERY"))
                .andExpect(jsonPath("$.items[3].snapshot.receivedEquipmentIds", hasSize(0)))
                .andExpect(jsonPath("$.items[4].snapshot.tender.status").value("COMPLETED"))
                .andExpect(jsonPath("$.items[4].snapshot.receivedEquipmentIds[0]").value(first))
                .andExpect(jsonPath("$.items[4].snapshot.receivedEquipmentIds[1]").value(second))
                .andExpect(jsonPath("$.items[4].snapshot.participants[0].status").value("WINNER"))
                .andExpect(jsonPath("$.items[4].snapshot.transitionComment").value("Received in full"))
                .andExpect(jsonPath("$.items[4].changedByUserId")
                        .value(appUserRepository.findByLogin(login).orElseThrow().getId()));
        mockMvc.perform(get("/api/v1/equipment/" + first + "/history")
                        .header(HttpHeaders.AUTHORIZATION, receiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].snapshot.event").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].snapshot.equipment.sourceTenderId").value(tender))
                .andExpect(jsonPath("$.items[0].changedByUserId")
                        .value(appUserRepository.findByLogin(login).orElseThrow().getId()));
        mockMvc.perform(get("/api/v1/equipment/" + second).header(HttpHeaders.AUTHORIZATION, receiver))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.sourceTenderId").value(tender));
        assertThat(equipmentCount()).isEqualTo(2);
        assertThat(equipmentHistoryCount()).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("select count(*) from equipment_movement", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NEED", "COLLECTING_OFFERS", "COMPLETED", "CANCELLED"})
    void receiptRequiresAwaitingDelivery(String source) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        jdbcTemplate.update("update tender set status=? where id=?", source, tender);

        receive(tender, admin, RECEIPT).andExpect(status().isConflict());

        assertThat(equipmentCount()).isZero();
        assertThat(equipmentHistoryCount()).isZero();
        assertThat(historyCount(tender)).isEqualTo(4);
    }

    @Test
    void receiptRequiresSelectedWinner() throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        jdbcTemplate.update("update tender_participant set status='PARTICIPATING' where tender_id=?", tender);

        receive(tender, admin, RECEIPT).andExpect(status().isConflict());

        assertUnreceived(tender, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "agent", "cleaner"})
    void receiptRequiresReceivePermission(String login) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");

        receive(tender, token(login), RECEIPT).andExpect(status().isForbidden());

        assertUnreceived(tender, 0, 0);
    }

    @Test
    void managePermissionDoesNotReplaceReceivePermission() throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        jdbcTemplate.update("""
                delete from role_permission where role_id = (select id from role where name='TENDER_MANAGER')
                and permission_id = (select id from permission where code='TENDER_RECEIVE')
                """);

        receive(tender, token("tender_manager"), RECEIPT).andExpect(status().isForbidden());

        assertUnreceived(tender, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"equipment\":null}", "{\"equipment\":[]}", "{\"equipment\":[null,null]}",
            "{\"equipment\":[{},{}]}",
            "{\"equipment\":[{\"model\":\" \",\"serialNumber\":\"A\"},{\"model\":\"B\",\"serialNumber\":\"B\"}]}",
            "{\"equipment\":[{\"model\":\"A\",\"serialNumber\":\" \"},{\"model\":\"B\",\"serialNumber\":\"B\"}]}"})
    void validatesReceiptBody(String body) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");

        receive(tender, admin, body).andExpect(status().isBadRequest());

        assertUnreceived(tender, 0, 0);
    }

    @ParameterizedTest
    @CsvSource({"model,201", "serialNumber,101", "comment,4001"})
    void validatesReceiptFieldLengths(String field, int length) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        String body = objectMapper.writeValueAsString(Map.of("equipment", List.of(
                Map.of("model", "model".equals(field) ? "A".repeat(length) : "Model A",
                        "serialNumber", "serialNumber".equals(field) ? "A".repeat(length) : "SN-A"),
                Map.of("model", "Model B", "serialNumber", "SN-B")),
                "comment", "comment".equals(field) ? "A".repeat(length) : "Received"));

        receive(tender, admin, body).andExpect(status().isBadRequest());

        assertUnreceived(tender, 0, 0);
    }

    @ParameterizedTest
    @CsvSource({"model,0008", "serialNumber,0008", "model,001f", "serialNumber,001f",
            "model,2003", "serialNumber,2003"})
    void rejectsEquipmentFieldsBlankAfterNormalization(String field, String codePoint) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        String blank = Character.toString(Integer.parseInt(codePoint, 16));
        String body = objectMapper.writeValueAsString(Map.of("equipment", List.of(
                Map.of("model", "model".equals(field) ? blank : "Model A",
                        "serialNumber", "serialNumber".equals(field) ? blank : "SN-A"),
                Map.of("model", "Model B", "serialNumber", "SN-B"))));

        receive(tender, admin, body).andExpect(status().isBadRequest());

        assertUnreceived(tender, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void rejectsUnderAndOverDelivery(int quantity) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        jdbcTemplate.update("update tender set quantity=? where id=?", quantity, tender);

        receive(tender, admin, RECEIPT).andExpect(status().isBadRequest());

        assertUnreceived(tender, 0, 0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SN-A", "sn-a", "  sn-a  "})
    void duplicateSerialWithinDeliveryRollsBackEverything(String duplicate) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");

        receive(tender, admin, RECEIPT.replace("SN-B", duplicate)).andExpect(status().isConflict());

        assertUnreceived(tender, 0, 0);
        receive(tender, admin, RECEIPT).andExpect(status().isOk());
        assertThat(equipmentCount()).isEqualTo(2);
        assertThat(historyCount(tender)).isEqualTo(5);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SN-A", "SN-B", "  sn-b  "})
    void existingSerialRollsBackNewEquipmentAndItsHistory(String duplicate) throws Exception {
        String admin = token("admin");
        long existing = fixtures.registerEquipment(admin.substring(7), duplicate);
        long tender = awaiting(admin, "NEURALYZER");

        receive(tender, admin, RECEIPT).andExpect(status().isConflict());

        assertUnreceived(tender, 1, 1);
        mockMvc.perform(get("/api/v1/equipment/" + existing).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.sourceTenderId").doesNotExist())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
        receive(tender, admin, RECEIPT.replace("SN-A", "NEW-A").replace("SN-B", "NEW-B"))
                .andExpect(status().isOk());
        assertThat(equipmentCount()).isEqualTo(3);
        assertThat(equipmentHistoryCount()).isEqualTo(3);
    }

    @Test
    void rejectsRepeatedReceiptWithoutAdditionalEquipmentOrHistory() throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        receive(tender, admin, RECEIPT).andExpect(status().isOk());
        Object completedAt = jdbcTemplate.queryForObject("select completed_at from tender where id=?",
                Object.class, tender);

        receive(tender, admin, RECEIPT).andExpect(status().isConflict());

        assertThat(equipmentCount()).isEqualTo(2);
        assertThat(equipmentHistoryCount()).isEqualTo(2);
        assertThat(historyCount(tender)).isEqualTo(5);
        assertThat(jdbcTemplate.queryForObject("select completed_at from tender where id=?",
                Object.class, tender)).isEqualTo(completedAt);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "999999"})
    void ordinaryRegistrationAndEditingCannotSetOrReplaceTenderSource(String source) throws Exception {
        String admin = token("admin");
        long tender = awaiting(admin, "NEURALYZER");
        var result = receive(tender, admin, RECEIPT).andExpect(status().isOk()).andReturn();
        long equipment = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("equipment").get(0).get("id").asLong();
        String body = "{\"type\":\"NEUTRALIZER\",\"model\":\"Updated\",\"serialNumber\":\"UPDATED\","
                + "\"sourceTenderId\":" + source + "}";

        mockMvc.perform(put("/api/v1/equipment/" + equipment).header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(body)).andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceTenderId").value(tender));
        mockMvc.perform(post("/api/v1/equipment").header(HttpHeaders.AUTHORIZATION, admin)
                .contentType(APPLICATION_JSON).content(body.replace("UPDATED", "DIRECT")))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sourceTenderId").doesNotExist());

        mockMvc.perform(get("/api/v1/equipment/" + equipment + "/history")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].snapshot.event").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].snapshot.equipment.sourceTenderId").value(tender))
                .andExpect(jsonPath("$.items[0].snapshot.equipment.model").value("Model A"))
                .andExpect(jsonPath("$.items[1].snapshot.event").value("UPDATED"))
                .andExpect(jsonPath("$.items[1].snapshot.equipment.sourceTenderId").value(tender));
        assertThat(historyCount(tender)).isEqualTo(5);
    }

    @Test
    void issueAndReturnPreserveTenderSourceAndReceiptSnapshot() throws Exception {
        String rawAdmin = fixtures.signInAndGetToken("admin", "admin");
        String admin = "Bearer " + rawAdmin;
        long tender = awaiting(admin, "NEURALYZER");
        var result = receive(tender, admin, RECEIPT).andExpect(status().isOk()).andReturn();
        long equipment = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("equipment").get(0).get("id").asLong();
        long incident = fixtures.createIncident(rawAdmin, fixtures.uploadFile(rawAdmin));
        long agent = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long issue = fixtures.createEquipmentMovement(rawAdmin, incident, agent,
                EquipmentMovementOperationApi.ISSUE, null, List.of(equipment));
        fixtures.changeEquipmentMovementStatus(rawAdmin, issue, EquipmentMovementStatusApi.PROCESSING);
        fixtures.changeEquipmentMovementStatus(rawAdmin, issue, EquipmentMovementStatusApi.COMPLETED);
        long returned = fixtures.createEquipmentMovement(rawAdmin, incident, agent,
                EquipmentMovementOperationApi.RETURN, issue, List.of(equipment));
        fixtures.changeEquipmentMovementStatus(rawAdmin, returned, EquipmentMovementStatusApi.PROCESSING);
        fixtures.changeEquipmentMovementStatus(rawAdmin, returned, EquipmentMovementStatusApi.COMPLETED);

        mockMvc.perform(get("/api/v1/equipment/" + equipment).header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.sourceTenderId").value(tender));
        mockMvc.perform(get("/api/v1/equipment/" + equipment + "/history")
                        .header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.items[0].snapshot.event").value("RECEIVED"))
                .andExpect(jsonPath("$.items[0].snapshot.equipment.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.items[1].snapshot.event").value("ISSUED"))
                .andExpect(jsonPath("$.items[1].snapshot.equipment.sourceTenderId").value(tender))
                .andExpect(jsonPath("$.items[2].snapshot.event").value("RETURNED"))
                .andExpect(jsonPath("$.items[2].snapshot.equipment.sourceTenderId").value(tender));
        assertThat(historyCount(tender)).isEqualTo(5);
    }

    @Test
    void validatesTenderReferencePathAndAuthentication() throws Exception {
        String admin = token("admin");
        receive(999999, admin, RECEIPT).andExpect(status().isNotFound());
        receive(0, admin, RECEIPT).andExpect(status().isBadRequest());
        mockMvc.perform(post(URL + "/1/receipt").contentType(APPLICATION_JSON).content(RECEIPT))
                .andExpect(status().isUnauthorized());
        assertThat(equipmentCount()).isZero();
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, "tender_manager".equals(login) ? "admin" : login);
    }

    private long awaiting(String token, String type) throws Exception {
        var created = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content("{\"equipmentType\":\"" + type + "\",\"quantity\":2}"))
                .andExpect(status().isCreated()).andReturn();
        long tender = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(patch(URL + "/" + tender + "/status").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content("{\"status\":\"COLLECTING_OFFERS\"}"))
                .andExpect(status().isOk());
        var offer = mockMvc.perform(post(URL + "/" + tender + "/participants")
                .header(HttpHeaders.AUTHORIZATION, token).contentType(APPLICATION_JSON)
                .content("{\"name\":\"Orbit\",\"proposal\":\"Delivery in five days\"}"))
                .andExpect(status().isCreated()).andReturn();
        long participant = objectMapper.readTree(offer.getResponse().getContentAsString()).get("id").asLong();
        mockMvc.perform(post(URL + "/" + tender + "/winner").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content("{\"participantId\":" + participant + "}"))
                .andExpect(status().isOk());
        return tender;
    }

    private ResultActions receive(long tender, String token, String body) throws Exception {
        return mockMvc.perform(post(URL + "/" + tender + "/receipt").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(APPLICATION_JSON).content(body));
    }

    private int equipmentCount() {
        return jdbcTemplate.queryForObject("select count(*) from equipment_item", Integer.class);
    }

    private int equipmentHistoryCount() {
        return jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type='EQUIPMENT'",
                Integer.class);
    }

    private int historyCount(long tender) {
        return jdbcTemplate.queryForObject("select count(*) from entity_history where entity_type='TENDER'"
                + " and entity_id=?", Integer.class, tender);
    }

    private void assertUnreceived(long tender, int equipment, int equipmentHistory) {
        assertThat(equipmentCount()).isEqualTo(equipment);
        assertThat(equipmentHistoryCount()).isEqualTo(equipmentHistory);
        assertThat(historyCount(tender)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("select status from tender where id=?", String.class, tender))
                .isEqualTo("AWAITING_DELIVERY");
        assertThat(jdbcTemplate.queryForObject("select completed_at from tender where id=?", Object.class, tender))
                .isNull();
    }
}
