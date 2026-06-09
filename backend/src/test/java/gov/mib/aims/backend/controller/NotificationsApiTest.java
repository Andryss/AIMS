package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.generated.model.SignInRequest;
import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.services.NotificationService;
import gov.mib.aims.backend.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционные тесты Notifications API.
 */
class NotificationsApiTest extends BaseApiTest {

    private static final String SIGNIN_URL = "/api/v1/auth/signin";
    private static final String NOTIFICATIONS_URL = "/api/v1/notifications";
    private static final String UNREAD_COUNT_URL = "/api/v1/notifications/unread-count";

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private AppUserRepository appUserRepository;

    private Long operatorUserId;
    private Long otherUserId;
    private Long notificationId;

    @BeforeEach
    void setUp() {
        operatorUserId = appUserRepository.findByLogin("operator").orElseThrow().getId();
        otherUserId = appUserRepository.save(AppUserEntity.builder()
                .login("other")
                .passwordHash("$2a$10$LArEHlxdvPz42xMgLOLMLu2H9ZtkqH0Oge920nnSSL8Bowo4KJIKa")
                .build()).getId();
        var record = notificationService.send(
                operatorUserId,
                "Incident ready for analysis",
                List.of(EntityRef.format(EntityType.INCIDENT, 612L))
        );
        notificationId = record.id();
    }

    @Test
    void listUnreadCountAndMarkRead() throws Exception {
        String token = signInAndGetToken("operator", "operator");

        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(1));

        mockMvc.perform(get(NOTIFICATIONS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].message").value("Incident ready for analysis"))
                .andExpect(jsonPath("$.items[0].relatedEntities[0]").value("INCIDENT:612"))
                .andExpect(jsonPath("$.items[0].read").value(false));

        mockMvc.perform(patch(NOTIFICATIONS_URL + "/" + notificationId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get(UNREAD_COUNT_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));

        mockMvc.perform(get(NOTIFICATIONS_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].read").value(true));
    }

    @Test
    void listWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(NOTIFICATIONS_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void markReadForeignNotificationReturns404() throws Exception {
        Long foreignNotificationId = notificationService.send(
                otherUserId,
                "For other user",
                List.of(EntityRef.format(EntityType.INCIDENT, 1L))
        ).id();

        String token = signInAndGetToken("operator", "operator");
        mockMvc.perform(patch(NOTIFICATIONS_URL + "/" + foreignNotificationId + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("notification.not_found"));
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
