package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.SignInRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Files API.
 */
class FilesApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String FILES_URL = "/api/v1/files";

    @Test
    void uploadAndDownloadRoundtrip() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        byte[] payload = "alien sighting photo".getBytes();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sighting.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                payload
        );

        MvcResult uploadResult = mockMvc.perform(multipart(FILES_URL)
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", not(emptyOrNullString())))
                .andExpect(jsonPath("$.fileName").value("sighting.jpg"))
                .andExpect(jsonPath("$.contentType").value(MediaType.IMAGE_JPEG_VALUE))
                .andExpect(jsonPath("$.fileSize").value(payload.length))
                .andReturn();

        long fileId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("id")
                .asLong();

        mockMvc.perform(get(FILES_URL + "/" + fileId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().bytes(payload));
    }

    @Test
    void uploadWithoutTokenReturns401() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "data".getBytes()
        );
        mockMvc.perform(multipart(FILES_URL).file(file))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void downloadUnknownIdReturns404() throws Exception {
        String token = fixtures.signInAndGetToken("operator", "operator");
        mockMvc.perform(get(FILES_URL + "/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("file.not_found"));
    }

}
