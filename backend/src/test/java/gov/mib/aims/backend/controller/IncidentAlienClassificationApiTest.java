package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.model.AlienPrediction;
import gov.mib.aims.backend.exception.Errors;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import gov.mib.aims.backend.services.classification.AlienClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Предложение вида по вложению инцидента без сохранения результата.
 */
class IncidentAlienClassificationApiTest extends BaseApiTest {

    @MockitoBean
    private AlienClassifier classifier;

    @ParameterizedTest
    @ValueSource(strings = {"analyst", "admin"})
    void suggestsKnownSpeciesWithoutChangingIncidentOrHistory(String login) throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        jdbcTemplate.update("update incident set status='READY_FOR_ANALYSIS' where id=?", incidentId);
        Long alienId = jdbcTemplate.queryForObject("insert into alien (name,description,threat_level,classification_key)"
                + " values ('Ашта','Описание',9,'starfield_ashta') returning id", Long.class);
        when(classifier.predict(anyList())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new AlienPrediction("starfield_ashta", 0.9, true);
        });
        var before = jdbcTemplate.queryForMap("select * from incident where id=?", incidentId);
        Integer history = jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class);

        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token(login)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("MATCH"))
                .andExpect(jsonPath("$.alien.id").value(alienId))
                .andExpect(jsonPath("$.confidence").value(90.0))
                .andExpect(jsonPath("$.alien.classificationKey").doesNotExist());

        assertThat(jdbcTemplate.queryForMap("select * from incident where id=?", incidentId)).isEqualTo(before);
        assertThat(jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class)).isEqualTo(history);
    }

    @ParameterizedTest
    @ValueSource(strings = {"operator", "agent", "management"})
    void rejectsUsersWithoutClassificationPermission(String login) throws Exception {
        mockMvc.perform(post(url(1)).header(HttpHeaders.AUTHORIZATION, token(login)))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsBestScoreWithoutSpeciesWhenNoMatch(boolean belowThreshold) throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        jdbcTemplate.update("update incident set status='READY_FOR_ANALYSIS' where id=?", incidentId);
        when(classifier.predict(anyList())).thenReturn(new AlienPrediction("missing_key", 0.15, !belowThreshold));

        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token("analyst")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.confidence").value(15.0)).andExpect(jsonPath("$.alien").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-state", "no-images", "unsupported", "missing-incident", "missing-file", "unavailable"})
    void distinguishesInvalidInputFromTechnicalFailure(String variant) throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        jdbcTemplate.update("update incident set status='READY_FOR_ANALYSIS' where id=?", incidentId);
        Long fileId = jdbcTemplate.queryForObject(
                "select (attachment_file_ids->>0)::bigint from incident where id=?", Long.class, incidentId);
        int expected;
        if (variant.equals("wrong-state")) {
            jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", incidentId);
            expected = 400;
        } else if (variant.equals("no-images")) {
            jdbcTemplate.update("update incident set attachment_file_ids='[]' where id=?", incidentId);
            expected = 400;
        } else if (variant.equals("unsupported")) {
            jdbcTemplate.update("update stored_file set content_type='image/gif' where id=?", fileId);
            expected = 400;
        } else if (variant.equals("missing-incident")) {
            incidentId = Long.MAX_VALUE;
            expected = 404;
        } else if (variant.equals("missing-file")) {
            jdbcTemplate.update("delete from stored_file where id=?", fileId);
            expected = 400;
        } else {
            when(classifier.predict(anyList())).thenThrow(Errors.classifierUnavailable());
            expected = 503;
        }
        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token("analyst")))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"image/png", "image/jpeg"})
    void usesAllSupportedAttachmentsWithoutARequestBody(String contentType) throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        var ids = java.util.stream.IntStream.range(0, 3).mapToObj(index -> jdbcTemplate.queryForObject(
                "insert into stored_file (storage_id,file_name,content_type,file_size) values (?,?,?,1) returning id",
                Long.class, "photo-" + index, "photo-" + index, contentType)).toList();
        jdbcTemplate.update("update incident set status='READY_FOR_ANALYSIS',attachment_file_ids=?::jsonb where id=?",
                objectMapper.writeValueAsString(ids), incidentId);
        when(classifier.predict(anyList())).thenAnswer(invocation -> {
            java.util.List<?> images = invocation.getArgument(0);
            assertThat(images).hasSize(3);
            return new AlienPrediction("missing_key", 0.1, false);
        });
        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token("analyst")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.confidence").value(10.0));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(url(1)))
                .andExpect(status().isUnauthorized());
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, login.equals("management") ? "admin" : login);
    }

    private String url(long id) {
        return "/api/v1/incidents/" + id + "/alien-classification";
    }
}
