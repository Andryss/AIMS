package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.model.BatchUsersRequest;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Users API (UC3).
 */
class UsersApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String USERS_SEARCH_URL = "/api/v1/users/search";
    private static final String USERS_BATCH_URL = "/api/v1/users/batch";

    @Autowired
    private AppUserRepository appUserRepository;

    @Test
    void searchWithoutRoleReturns400() throws Exception {
        String agentToken = fixtures.signInAndGetToken("agent", "agent");

        mockMvc.perform(get(USERS_SEARCH_URL + "?q=ag")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation.error"));
    }

    @Test
    void agentSearchesUsersWithAgentRoleFilter() throws Exception {
        String agentToken = fixtures.signInAndGetToken("agent", "agent");

        mockMvc.perform(get(USERS_SEARCH_URL + "?q=ag&role=AGENT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].login").exists())
                .andExpect(jsonPath("$.items[0].id").exists());
    }

    @Test
    void searchWithShortQueryReturns400() throws Exception {
        String agentToken = fixtures.signInAndGetToken("agent", "agent");

        mockMvc.perform(get(USERS_SEARCH_URL + "?q=a&role=AGENT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("validation.error"));
    }

    @Test
    void operatorCanSearchUsersWithUserReadPermission() throws Exception {
        String operatorToken = fixtures.signInAndGetToken("operator", "operator");

        mockMvc.perform(get(USERS_SEARCH_URL + "?q=ag&role=AGENT")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + operatorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void batchUsersReturnsRequestedOrderAndOmitsMissing() throws Exception {
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        long agentId = appUserRepository.findByLogin("agent").orElseThrow().getId();
        long agent2Id = appUserRepository.findByLogin("agent2").orElseThrow().getId();

        BatchUsersRequest request = new BatchUsersRequest()
                .ids(java.util.List.of(agent2Id, 999999L, agentId));

        mockMvc.perform(post(USERS_BATCH_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].id").value(agent2Id))
                .andExpect(jsonPath("$.items[0].login").value("agent2"))
                .andExpect(jsonPath("$.items[1].id").value(agentId))
                .andExpect(jsonPath("$.items[1].login").value("agent"));
    }

    @Test
    void batchEmptyIdsReturnsEmptyList() throws Exception {
        String agentToken = fixtures.signInAndGetToken("agent", "agent");
        BatchUsersRequest request = new BatchUsersRequest().ids(java.util.List.of());

        mockMvc.perform(post(USERS_BATCH_URL)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + agentToken)
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(0)));
    }

}
