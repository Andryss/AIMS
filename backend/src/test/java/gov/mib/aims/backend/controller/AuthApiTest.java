package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.SignInRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Auth API.
 */
class AuthApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String ME_URL = "/api/v1/auth/me";

    @Test
    void signInWithValidCredentialsReturnsToken() throws Exception {
        SignInRequest request = new SignInRequest().login("agent").password("secret");
        mockMvc.perform(post(SIGNIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(emptyOrNullString())));
    }

    @Test
    void signInWithWrongPasswordReturns401() throws Exception {
        SignInRequest request = new SignInRequest().login("agent").password("wrong");
        mockMvc.perform(post(SIGNIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("auth.invalid_credentials"));
    }

    @Test
    void signInWithUnknownUserReturns401() throws Exception {
        SignInRequest request = new SignInRequest().login("unknown").password("secret");
        mockMvc.perform(post(SIGNIN_URL)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getAuthMeWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void getAuthMeWithValidTokenReturnsLoginRolesAndPermissions() throws Exception {
        String token = signInAndGetToken("agent", "secret");
        mockMvc.perform(get(ME_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.login").value("agent"))
                .andExpect(jsonPath("$.roles", hasSize(1)))
                .andExpect(jsonPath("$.roles[0]").value("OPERATOR"))
                .andExpect(jsonPath("$.permissions", hasSize(2)))
                .andExpect(jsonPath("$.permissions", containsInAnyOrder("INCIDENT_READ", "INCIDENT_WRITE")));
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
