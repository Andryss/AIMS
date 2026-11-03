package gov.mib.aims.backend.controller;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.mib.aims.backend.support.AnalyticsTestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Исторические складские остатки, движения, открытые заявки и ожидаемые поставки.
 */
class EquipmentAnalyticsApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/equipment/statistics";
    private static final Instant FROM = Instant.parse("2025-05-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2025-05-02T00:00:00Z");
    private AnalyticsTestFixtures data;

    @BeforeEach
    void prepareFixtures() {
        data = new AnalyticsTestFixtures(jdbcTemplate, objectMapper, fixtures, mockMvc);
    }

    @Test
    void retainsIssueAtCutoffDespiteLaterReturnAndTypeChange() throws Exception {
        long equipment = equipment("NEURALYZER", "SN-1");
        long incident = data.incident();
        long owner = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long movement = data.create("equipment-movements", Map.of("operation", "ISSUE", "incidentId", incident,
                "responsibleUserId", owner, "equipmentIds", List.of(equipment)));
        var snapshot = data.snapshot("EQUIPMENT", equipment);
        data.clearHistory("EQUIPMENT", equipment);
        data.history("EQUIPMENT", equipment, FROM.minusSeconds(1), snapshot);
        snapshot.put("event", "ISSUED").put("movementId", movement);
        var item = (ObjectNode) snapshot.get("equipment");
        item.put("status", "ISSUED").put("holderUserId", owner).put("currentIssueMovementId", movement);
        data.history("EQUIPMENT", equipment, FROM, snapshot);
        snapshot.put("event", "RETURNED");
        item.put("status", "AVAILABLE").put("type", "NEUTRALIZER");
        item.remove("holderUserId");
        item.remove("currentIssueMovementId");
        data.history("EQUIPMENT", equipment, TO.plusSeconds(1), snapshot);
        var move = data.snapshot("EQUIPMENT_MOVEMENT", movement);
        data.clearHistory("EQUIPMENT_MOVEMENT", movement);
        ((ObjectNode) move.get("movement")).put("status", "COMPLETED");
        data.history("EQUIPMENT_MOVEMENT", movement, FROM, move);
        ((ObjectNode) move.get("movement")).put("responsibleUserId", 999999);
        data.history("EQUIPMENT_MOVEMENT", movement, TO.plusSeconds(1), move);

        report("received-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        report("issued-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("returned-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        report("issued-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("outstanding-by-incident").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].incidentId").value(incident));
        mockMvc.perform(get(URL + "/issued-count").param("from", FROM.toString()).param("to", TO.toString())
                .param("responsibleUserId", Long.toString(owner)).param("incidentId", Long.toString(incident))
                .header(HttpHeaders.AUTHORIZATION, token())).andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));
        mockMvc.perform(get(URL + "/issued-count").param("from", FROM.toString()).param("to", TO.toString())
                .param("responsibleUserId", "999999").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void editsDoNotCreateAdditionalReceiptsAndZeroActivityKeepsInventory() throws Exception {
        long equipment = equipment("NEURALYZER", "SN-1");
        var snapshot = data.snapshot("EQUIPMENT", equipment);
        data.clearHistory("EQUIPMENT", equipment);
        data.history("EQUIPMENT", equipment, FROM.minusSeconds(2), snapshot);
        snapshot.put("event", "UPDATED");
        ((ObjectNode) snapshot.get("equipment")).put("model", "Другая модель");
        data.history("EQUIPMENT", equipment, FROM, snapshot);

        report("received-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(0));
        report("available-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("inventory-by-type").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.equipmentType=='NEURALYZER')].total").value(hasItem(1)));
    }

    @Test
    void countsOneOpenRequestAcrossMultipleEquipmentTypes() throws Exception {
        long first = equipment("NEURALYZER", "SN-1");
        long second = equipment("NEUTRALIZER", "SN-2");
        var one = data.snapshot("EQUIPMENT", first);
        var two = data.snapshot("EQUIPMENT", second);
        data.clearHistory("EQUIPMENT", first);
        data.clearHistory("EQUIPMENT", second);
        data.history("EQUIPMENT", first, FROM.minusSeconds(1), one);
        data.history("EQUIPMENT", second, FROM.minusSeconds(1), two);
        long owner = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long movement = data.create("equipment-movements", Map.of("operation", "ISSUE",
                "incidentId", data.incident(), "responsibleUserId", owner, "equipmentIds", List.of(first, second)));
        var snapshot = data.snapshot("EQUIPMENT_MOVEMENT", movement);
        data.clearHistory("EQUIPMENT_MOVEMENT", movement);
        data.history("EQUIPMENT_MOVEMENT", movement, FROM, snapshot);
        ((ObjectNode) snapshot.get("movement")).put("status", "CANCELLED");
        data.history("EQUIPMENT_MOVEMENT", movement, TO, snapshot);

        report("open-requests").andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].requestCount").value(1));
        mockMvc.perform(get(URL + "/open-requests").param("from", FROM.toString()).param("to", TO.toString())
                .param("responsibleUserId", "999999").header(HttpHeaders.AUTHORIZATION, token()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(0));
        report("total-at-to").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2));
    }

    @Test
    void retainsExpectedDeliveryBeforeItsLaterCompletion() throws Exception {
        long tender = data.create("tenders", Map.of("equipmentType", "NEURALYZER", "quantity", 2));
        var snapshot = data.snapshot("TENDER", tender);
        data.clearHistory("TENDER", tender);
        ((ObjectNode) snapshot.get("tender")).put("status", "AWAITING_DELIVERY");
        data.history("TENDER", tender, FROM.minusSeconds(1), snapshot);
        ((ObjectNode) snapshot.get("tender")).put("status", "COMPLETED");
        data.history("TENDER", tender, TO, snapshot);

        report("expected-deliveries-by-type").andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].unitCount").value(2))
                .andExpect(jsonPath("$.items[0].tenderCount").value(1));
    }

    @Test
    void countsRepeatedCyclesAndDeduplicatesTheSameMovementEvent() throws Exception {
        long id = equipment("NEURALYZER", "SN-CYCLES");
        long owner = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long incident = data.incident();
        Map<String, Object> body = Map.of("operation", "ISSUE", "incidentId", incident,
                "responsibleUserId", owner, "equipmentIds", List.of(id));
        long first = data.create("equipment-movements", body);
        long returned = data.create("equipment-movements", body);
        long second = data.create("equipment-movements", body);
        var snapshot = data.snapshot("EQUIPMENT", id);
        data.clearHistory("EQUIPMENT", id);
        data.history("EQUIPMENT", id, FROM.minusSeconds(1), snapshot);
        snapshot.put("event", "ISSUED").put("movementId", first);
        ((ObjectNode) snapshot.get("equipment")).put("status", "ISSUED").put("currentIssueMovementId", first);
        data.history("EQUIPMENT", id, FROM, snapshot);
        data.history("EQUIPMENT", id, FROM.plusSeconds(1), snapshot);
        snapshot.put("event", "RETURNED").put("movementId", returned);
        ((ObjectNode) snapshot.get("equipment")).put("status", "AVAILABLE");
        data.history("EQUIPMENT", id, FROM.plusSeconds(2), snapshot);
        snapshot.put("event", "ISSUED").put("movementId", second);
        ((ObjectNode) snapshot.get("equipment")).put("status", "ISSUED").put("currentIssueMovementId", second);
        data.history("EQUIPMENT", id, FROM.plusSeconds(3), snapshot);
        snapshot.put("event", "RETURNED").put("movementId", 999999);
        data.history("EQUIPMENT", id, TO, snapshot);

        report("issued-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(2));
        report("returned-count").andExpect(status().isOk()).andExpect(jsonPath("$.count").value(1));
        report("trend").andExpect(status().isOk())
                .andExpect(jsonPath("$.buckets[0].values.issued").value(2))
                .andExpect(jsonPath("$.buckets[0].values.returned").value(1));
    }

    @Test
    void emptyInventoryHasNoInventedShare() throws Exception {
        report("inventory-by-type").andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].total").value(0))
                .andExpect(jsonPath("$.items[0].issuedShare").doesNotExist());
    }

    private long equipment(String type, String serial) throws Exception {
        return data.create("equipment", Map.of("type", type, "model", "Модель", "serialNumber", serial));
    }

    private ResultActions report(String metric) throws Exception {
        return mockMvc.perform(get(URL + "/" + metric).param("from", FROM.toString()).param("to", TO.toString())
                .header(HttpHeaders.AUTHORIZATION, token()));
    }

    private String token() throws Exception {
        return "Bearer " + fixtures.signInAndGetToken("admin", "admin");
    }
}
