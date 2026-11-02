package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API поэкземплярного складского учета.
 */
class EquipmentApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/equipment";
    private static final String VALID = """
            {"type":"NEURALYZER","model":"  Model A  ","serialNumber":"  SN-001  "}
            """;

    @ParameterizedTest
    @ValueSource(strings = {"admin", "storekeeper"})
    void writerRegistersUpdatesAndAuditsEquipment(String login) throws Exception {
        String token = fixtures.signInAndGetToken(login, login);
        MvcResult result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.model").value("Model A"))
                .andExpect(jsonPath("$.serialNumber").value("SN-001"))
                .andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.holderUserId").doesNotExist())
                .andExpect(jsonPath("$.createdAt").isNotEmpty()).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
        Object createdAt = jdbcTemplate.queryForObject("select created_at from equipment_item where id = ?",
                Object.class, id);
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("""
                                {"type":"NEUTRALIZER","model":"Model B","serialNumber":"SN-002",
                                 "status":"ISSUED","holderUserId":1}
                                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("AVAILABLE"))
                .andExpect(jsonPath("$.holderUserId").doesNotExist());
        assertThat(jdbcTemplate.queryForObject("select created_at from equipment_item where id = ?",
                Object.class, id)).isEqualTo(createdAt);
        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.model").value("Model B"));
        mockMvc.perform(get(URL + "/" + id + "/history").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.items[0].snapshot.event").value("REGISTERED"))
                .andExpect(jsonPath("$.items[0].snapshot.equipment.model").value("Model A"));
    }

    @Test
    void duplicateSerialNumberIgnoresCaseAndWhitespace() throws Exception {
        String token = fixtures.signInAndGetToken("admin", "admin");
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isCreated());
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID.replace("SN-001", "sn-001")))
                .andExpect(status().isConflict());
        assertThat(jdbcTemplate.queryForObject("select count(*) from equipment_item", Integer.class)).isEqualTo(1);
    }

    @Test
    void listUsesDefaultsFiltersAndLiteralSearch() throws Exception {
        String token = fixtures.signInAndGetToken("admin", "admin");
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID.replace("SN-001", "100%_!")))
                .andExpect(status().isCreated());
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(URL).param("q", " %_! ").param("type", "NEURALYZER")
                        .param("status", "AVAILABLE").param("size", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
        mockMvc.perform(get(URL).param("type", "NEUTRALIZER")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "analyst", "agent"})
    void readerCannotWriteEquipment(String login) throws Exception {
        String token = fixtures.signInAndGetToken(login, login);
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
    }

    @Test
    void cleanerCannotReadEquipmentAndAnonymousIsUnauthorized() throws Exception {
        mockMvc.perform(get(URL)).andExpect(status().isUnauthorized());
        String token = fixtures.signInAndGetToken("cleaner", "cleaner");
        mockMvc.perform(get(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}", "{\"type\":\"UNKNOWN\",\"model\":\"A\",\"serialNumber\":\"S\"}",
            "{\"type\":\"NEURALYZER\",\"model\":\"  \",\"serialNumber\":\"S\"}",
            "{\"type\":\"NEURALYZER\",\"model\":\"A\",\"serialNumber\":\"  \"}"
    })
    void invalidEquipmentIsRejected(String body) throws Exception {
        String token = fixtures.signInAndGetToken("admin", "admin");
        mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void missingEquipmentAndInvalidPagingAreRejected() throws Exception {
        String token = fixtures.signInAndGetToken("admin", "admin");
        mockMvc.perform(get(URL + "/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(URL).param("page", "-1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }
}
