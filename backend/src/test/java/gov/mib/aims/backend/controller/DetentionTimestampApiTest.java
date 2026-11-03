package gov.mib.aims.backend.controller;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Одинаковая точность дат в ответах, сохранённых заявках и истории.
 */
@Import(DetentionTimestampApiTest.PreciseChronoConfig.class)
class DetentionTimestampApiTest extends BaseApiTest {

    private static final String URL = "/api/v1/detention-requests";
    private static final Instant NOW = Instant.parse("2025-06-01T12:00:00.123456789Z");
    private static final String STORED_NOW = NOW.truncatedTo(ChronoUnit.MICROS).toString();

    @Test
    void preservesTimestampPrecisionAcrossReadsUpdatesAndHistory() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        long incident = fixtures.createIncident(operator, fixtures.uploadFile(operator));
        String agent = "Bearer " + fixtures.signInAndGetToken("agent", "agent");
        String guard = "Bearer " + fixtures.signInAndGetToken("prison_guard", "admin");
        String body = "{\"incidentId\":" + incident
                + ",\"subjectName\":\"Субъект\",\"subjectDescription\":\"Описание\",\"durationDays\":30}";
        var result = mockMvc.perform(post(URL).header(HttpHeaders.AUTHORIZATION, agent)
                .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.updatedAt").value(STORED_NOW)).andReturn();
        long id = objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, guard)
                .contentType(APPLICATION_JSON).content("{\"status\":\"ACCEPTED\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, guard)
                .contentType(APPLICATION_JSON).content("{\"status\":\"IN_CUSTODY\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.admittedAt").value(STORED_NOW))
                .andExpect(jsonPath("$.updatedAt").value(STORED_NOW));

        mockMvc.perform(get(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, guard))
                .andExpect(status().isOk()).andExpect(jsonPath("$.admittedAt").value(STORED_NOW));
        mockMvc.perform(put(URL + "/" + id).header(HttpHeaders.AUTHORIZATION, guard)
                .contentType(APPLICATION_JSON).content(body.replace("Описание", "Уточнение")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.admittedAt").value(STORED_NOW));
        mockMvc.perform(patch(URL + "/" + id + "/status").header(HttpHeaders.AUTHORIZATION, guard)
                .contentType(APPLICATION_JSON).content("{\"status\":\"RELEASED\",\"comment\":\"Основания отпали\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.releasedAt").value(STORED_NOW));

        mockMvc.perform(get(URL + "/" + id + "/history").header(HttpHeaders.AUTHORIZATION, guard))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[2].snapshot.request.admittedAt").value(STORED_NOW))
                .andExpect(jsonPath("$.items[3].snapshot.request.admittedAt").value(STORED_NOW))
                .andExpect(jsonPath("$.items[4].snapshot.request.releasedAt").value(STORED_NOW));
    }

    /**
     * Часы с точностью выше точности PostgreSQL для проверки сохранённых снимков.
     */
    @TestConfiguration
    static class PreciseChronoConfig {

        @Bean
        @Primary
        Clock preciseClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }
}
