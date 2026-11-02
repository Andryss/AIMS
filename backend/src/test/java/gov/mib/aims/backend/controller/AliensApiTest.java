package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.stream.Stream;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Aliens API (UC2).
 */
class AliensApiTest extends BaseApiTest {

    private static final String ALIENS_URL = "/api/v1/aliens";
    private static final String VALID = """
            {"name":"  Новый вид  ","description":"  Описание вида  ","threatLevel":5}
            """;

    @Test
    void searchAliensByPartialNameReturnsMatches() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", "слиз")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].name").exists())
                .andExpect(jsonPath("$.items[0].threatLevel").exists());
    }

    @Test
    void getAlienByIdReturnsRecord() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        MvcResult searchResult = mockMvc.perform(get(ALIENS_URL + "/search").param("q", "Слизень")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        long alienId = objectMapper.readTree(searchResult.getResponse().getContentAsString())
                .get("items").get(0).get("id").asLong();

        mockMvc.perform(get(ALIENS_URL + "/" + alienId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(alienId))
                .andExpect(jsonPath("$.name").value("Слизень"))
                .andExpect(jsonPath("$.threatLevel").value(3));
    }

    @Test
    void searchWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", "слиз"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void searchWithoutAlienReadPermissionReturns403() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", "слиз")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"analyst", "admin"})
    void writerCreatesUpdatesAndFindsAlien(String login) throws Exception {
        String token = fixtures.signInAndGetToken(login, login);
        mockMvc.perform(get("/api/v1/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.permissions", hasItem("ALIEN_WRITE")));
        MvcResult created = mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Новый вид"))
                .andExpect(jsonPath("$.description").value("Описание вида"))
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        LocalDateTime createdAt = jdbcTemplate.queryForObject(
                "select created_at from alien where id = ?", LocalDateTime.class, id);
        assertThat(createdAt).isNotNull();
        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Изменённый вид\",\"description\":\"Редкие признаки\",\"threatLevel\":10}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        assertThat(jdbcTemplate.queryForObject(
                "select created_at from alien where id = ?", LocalDateTime.class, id)).isEqualTo(createdAt);
        mockMvc.perform(get(ALIENS_URL).param("q", "  РЕДКИЕ  ")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Изменённый вид"));
        mockMvc.perform(get(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.threatLevel").value(10));
    }

    @Test
    void listSupportsPaginationAndEmptyQuery() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALIENS_URL).param("q", "  ").param("size", "1").param("page", "1")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.totalElements").value(2)).andExpect(jsonPath("$.page").value(1));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "?page=&size="})
    void listUsesOpenApiPaginationDefaults(String query) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALIENS_URL + query).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20)).andExpect(jsonPath("$.totalElements").value(2));
    }

    @ParameterizedTest
    @ValueSource(strings = {"%_", "\\тест"})
    void bothSearchEndpointsTreatWildcardsLiterally(String query) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"name\":\"Вид 100%_\\\\тест\",\"description\":\"Проба\",\"threatLevel\":1}"))
                .andExpect(status().isCreated());
        mockMvc.perform(get(ALIENS_URL).param("q", query).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", query)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
    }

    @Test
    void listReturnsEmptyResultForUnknownAlien() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALIENS_URL).param("q", "нет такого вида")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "agent", "cleaner"})
    void readerCannotWriteAlien(String login) throws Exception {
        String token = fixtures.signInAndGetToken(login, login);
        mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
        mockMvc.perform(put(ALIENS_URL + "/1").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"agent", "cleaner"})
    void readerCanListAliens(String login) throws Exception {
        String token = fixtures.signInAndGetToken(login, login);
        mockMvc.perform(get(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void operatorCannotListAliens() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        mockMvc.perform(get(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{\"name\":\"  \",\"description\":\"ok\",\"threatLevel\":3}",
        "{\"name\":\"ok\",\"description\":\"  \",\"threatLevel\":3}",
        "{\"name\":\"ok\",\"description\":\"ok\",\"threatLevel\":0}",
        "{\"name\":\"ok\",\"description\":\"ok\",\"threatLevel\":11}",
        "{}"
    })
    void invalidBodiesAreRejectedForCreateAndUpdate(String body) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        long id = fixtures.findAlienId("Слизень", token);
        mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        mockMvc.perform(get(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.name").value("Слизень"));
    }

    @ParameterizedTest(name = "Reject oversized {0}")
    @MethodSource("oversizedBodies")
    void rejectsOversizedFields(String field, String body) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> oversizedBodies() {
        return Stream.of(
                Arguments.of("name", "{\"name\":\"" + "x".repeat(201)
                        + "\",\"description\":\"ok\",\"threatLevel\":1}"),
                Arguments.of("description", "{\"name\":\"ok\",\"description\":\""
                        + "x".repeat(4001) + "\",\"threatLevel\":1}")
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"?page=-1", "?size=0", "?size=101"})
    void rejectsInvalidPagination(String query) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALIENS_URL + query).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unknownAlienReturns404AndAnonymousRequestsReturn401() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(put(ALIENS_URL + "/999999").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isNotFound());
        mockMvc.perform(get(ALIENS_URL)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(ALIENS_URL).contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put(ALIENS_URL + "/1").contentType(APPLICATION_JSON).content(VALID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void updatingLinkedAlienPreservesIncidentReference() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        String operator = fixtures.signInAndGetToken("operator", "operator");
        long incident = fixtures.createIncident(operator, fixtures.uploadFile(operator));
        long alien = fixtures.findAlienId("Слизень", token);
        mockMvc.perform(post("/api/v1/incidents/" + incident + "/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operator)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"READY_FOR_ANALYSIS\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/incidents/" + incident + "/alien")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"alienId\":" + alien + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(put(ALIENS_URL + "/" + alien).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(VALID)).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/incidents/" + incident)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.alienId").value(alien));
        mockMvc.perform(get(ALIENS_URL + "/" + alien).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(jsonPath("$.name").value("Новый вид"));
    }

}
