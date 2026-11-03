package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.services.FileStorage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Фотографии базы знаний и внутренний ключ классификации.
 */
class AlienPhotosApiTest extends BaseApiTest {

    private static final String ALIENS_URL = "/api/v1/aliens";
    private static final byte[] PHOTO = {1, 2, 3, 4};

    @Autowired
    private FileStorage fileStorage;

    @ParameterizedTest
    @ValueSource(strings = {"analyst", "admin"})
    void preservesPhotoOrderAndInternalKeyWithoutExposingItInResponses(String login) throws Exception {
        String token = fixtures.signInAndGetToken(login, login);
        long first = upload(token);
        long second = upload(token);
        long id = create(token, "[" + second + "," + first + "]");
        jdbcTemplate.update("update alien set classification_key='starfield_ashta' where id=?", id);

        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body(null)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoFileIds", contains((int) second, (int) first)))
                .andExpect(jsonPath("$.classificationKey").doesNotHaveJsonPath());
        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body("null")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoFileIds", hasSize(2)));
        mockMvc.perform(get(ALIENS_URL).param("q", "Фотовид")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].photoFileIds", hasSize(2)))
                .andExpect(jsonPath("$.items[0].classificationKey").doesNotHaveJsonPath());
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", "Фотовид")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].classificationKey").doesNotHaveJsonPath());
        mockMvc.perform(get(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.classificationKey").doesNotHaveJsonPath());
        assertThat(jdbcTemplate.queryForObject("select classification_key from alien where id=?",
                String.class, id)).isEqualTo("starfield_ashta");

        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body("[]")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoFileIds", hasSize(0)));
        mockMvc.perform(get("/api/v1/files/" + first).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(content().bytes(PHOTO));
    }

    @Test
    void createsWithoutPhotosAndDoesNotAcceptClassificationKeyFromClient() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        String request = body(null).replace("\"threatLevel\":4", "\"threatLevel\":4,"
                + "\"classificationKey\":\"starfield_ashta\"");
        mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(request))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.photoFileIds", hasSize(0)))
                .andExpect(jsonPath("$.classificationKey").doesNotHaveJsonPath());
        assertThat(jdbcTemplate.queryForObject("select classification_key from alien where name='Фотовид'",
                String.class)).isNull();
    }

    @Test
    void editingFieldsAndKeepingPhotoIdsDoesNotReadOriginalFilesAgain() throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        long file = upload(token);
        long id = create(token, "[" + file + "]");
        String storageId = jdbcTemplate.queryForObject("select storage_id from stored_file where id=?",
                String.class, file);
        fileStorage.delete(storageId);

        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body("[" + file + "]")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoFileIds", contains((int) file)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"duplicate", "unknown", "too-many", "null-item", "negative"})
    void rejectsInvalidPhotoReferencesWithoutChangingExistingEntry(String variant) throws Exception {
        String token = fixtures.signInAndGetToken("analyst", "analyst");
        long file = upload(token);
        long id = create(token, "[" + file + "]");
        String ids = switch (variant) {
            case "duplicate" -> "[" + file + "," + file + "]";
            case "unknown" -> "[999999]";
            case "too-many" -> "[" + (file + ",").repeat(20) + file + "]";
            case "null-item" -> "[null]";
            default -> "[-1]";
        };
        mockMvc.perform(put(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body(ids)))
                .andExpect(status().is("unknown".equals(variant) ? 404 : 400));
        mockMvc.perform(get(ALIENS_URL + "/" + id).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.photoFileIds", contains((int) file)));
    }

    private long create(String token, String ids) throws Exception {
        MvcResult result = mockMvc.perform(post(ALIENS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(APPLICATION_JSON).content(body(ids)))
                .andExpect(status().isCreated()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long upload(String token) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/v1/files")
                        .file(new MockMultipartFile("file", "photo.jpg", "image/jpeg", PHOTO))
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private String body(String ids) {
        return "{\"name\":\"Фотовид\",\"description\":\"Фотографии и наблюдения\",\"threatLevel\":4"
                + (ids == null ? "" : ",\"photoFileIds\":" + ids) + "}";
    }
}
