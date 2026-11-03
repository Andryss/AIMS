package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.services.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Пакетное чтение метаданных файлов без загрузки содержимого.
 */
class FileMetadataApiTest extends BaseApiTest {

    private static final String BATCH_URL = "/api/v1/files/batch";

    @Autowired
    private FileStorage fileStorage;

    @ParameterizedTest
    @CsvSource({"operator,operator", "analyst,analyst", "admin,admin", "management,admin"})
    void readsMetadataAndOriginalWithFileReadPermission(String login, String password) throws Exception {
        String analyst = fixtures.signInAndGetToken("analyst", "analyst");
        long id = upload(analyst, "sample.png", "image/png", new byte[]{1, 2, 3});
        String token = fixtures.signInAndGetToken(login, password);

        mockMvc.perform(post(BATCH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"ids\":[" + id + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id").value(id))
                .andExpect(jsonPath("$.items[0].fileName").value("sample.png"))
                .andExpect(jsonPath("$.items[0].contentType").value("image/png"))
                .andExpect(jsonPath("$.items[0].fileSize").value(3))
                .andExpect(jsonPath("$.items[0].createdAt").exists());
        mockMvc.perform(get("/api/v1/files/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().bytes(new byte[]{1, 2, 3}));
    }

    @Test
    void preservesRequestedOrderSkipsUnknownIdsAndDoesNotReadContents() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        long first = upload(token, "report.pdf", "application/pdf", new byte[]{4, 5});
        long second = upload(token, "picture.jpg", "image/jpeg", new byte[]{6, 7, 8});
        String storageId = jdbcTemplate.queryForObject("select storage_id from stored_file where id=?",
                String.class, first);
        fileStorage.delete(storageId);

        mockMvc.perform(post(BATCH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON)
                        .content("{\"ids\":[" + second + ",999999," + first + "]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[*].id", contains((int) second, (int) first)))
                .andExpect(jsonPath("$.items[1].contentType").value("application/pdf"));
        mockMvc.perform(post(BATCH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"ids\":[]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(0)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"ids\":null}", "{\"ids\":[null]}", "{\"ids\":[0]}"})
    void rejectsInvalidIds(String body) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(post(BATCH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMoreThanOneHundredIds() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        mockMvc.perform(post(BATCH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"ids\":[" + "1,".repeat(100) + "1]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deniesAnonymousAndRoleWithoutFileRead() throws Exception {
        mockMvc.perform(post(BATCH_URL).contentType(APPLICATION_JSON).content("{\"ids\":[]}"))
                .andExpect(status().isUnauthorized());
        String token = fixtures.signInAndGetToken("storekeeper", "storekeeper");
        mockMvc.perform(post(BATCH_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content("{\"ids\":[]}"))
                .andExpect(status().isForbidden());
    }

    private long upload(String token, String name, String contentType, byte[] bytes) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", name, contentType, bytes))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
