package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.SignInRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Aliens API (UC2).
 */
class AliensApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String ALIENS_URL = "/api/v1/aliens";

    @Test
    void searchAliensByPartialNameReturnsMatches() throws Exception {
        String token = signInAndGetToken("analyst", "analyst");
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", "слиз")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.items[0].name").exists())
                .andExpect(jsonPath("$.items[0].threatLevel").exists());
    }

    @Test
    void getAlienByIdReturnsRecord() throws Exception {
        String token = signInAndGetToken("analyst", "analyst");
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
        String token = signInAndGetToken("operator", "operator");
        mockMvc.perform(get(ALIENS_URL + "/search").param("q", "слиз")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String signInAndGetToken(String login, String password) throws Exception {
        SignInRequest request = new SignInRequest().login(login).password(password);
        MvcResult result = mockMvc.perform(post(SIGNIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
