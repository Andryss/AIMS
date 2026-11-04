package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.model.AlienPrediction;
import gov.mib.aims.backend.exception.Errors;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import gov.mib.aims.backend.services.AlienClassifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        Long fileId = jdbcTemplate.queryForObject(
                "select (attachment_file_ids->>0)::bigint from incident where id=?", Long.class, incidentId);
        Long alienId = jdbcTemplate.queryForObject("insert into alien (name,description,threat_level,classification_key)"
                + " values ('Ашта','Описание',9,'starfield_ashta') returning id", Long.class);
        when(classifier.predict(any())).thenAnswer(invocation -> {
            assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
            return new AlienPrediction("starfield_ashta", 0.9, true);
        });
        var before = jdbcTemplate.queryForMap("select * from incident where id=?", incidentId);
        Integer history = jdbcTemplate.queryForObject("select count(*) from entity_history", Integer.class);

        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token(login))
                        .contentType(APPLICATION_JSON).content("{\"fileId\":" + fileId + "}"))
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
        mockMvc.perform(post(url(1)).header(HttpHeaders.AUTHORIZATION, token(login))
                        .contentType(APPLICATION_JSON).content("{\"fileId\":1}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void returnsBestScoreWithoutSpeciesWhenNoMatch(boolean belowThreshold) throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        jdbcTemplate.update("update incident set status='READY_FOR_ANALYSIS' where id=?", incidentId);
        Long fileId = jdbcTemplate.queryForObject(
                "select (attachment_file_ids->>0)::bigint from incident where id=?", Long.class, incidentId);
        when(classifier.predict(any())).thenReturn(new AlienPrediction("missing_key", 0.15, !belowThreshold));

        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token("analyst"))
                        .contentType(APPLICATION_JSON).content("{\"fileId\":" + fileId + "}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("NO_MATCH"))
                .andExpect(jsonPath("$.confidence").value(15.0)).andExpect(jsonPath("$.alien").doesNotExist());
    }

    @ParameterizedTest
    @ValueSource(strings = {"wrong-state", "unattached", "missing-incident", "missing-file", "unavailable"})
    void distinguishesInvalidInputFromTechnicalFailure(String variant) throws Exception {
        long incidentId = fixtures.createPreparedIncident();
        jdbcTemplate.update("update incident set status='READY_FOR_ANALYSIS' where id=?", incidentId);
        Long fileId = jdbcTemplate.queryForObject(
                "select (attachment_file_ids->>0)::bigint from incident where id=?", Long.class, incidentId);
        int expected;
        if (variant.equals("wrong-state")) {
            jdbcTemplate.update("update incident set status='EXECUTION_COMPLETED' where id=?", incidentId);
            expected = 400;
        } else if (variant.equals("unattached")) {
            fileId = Long.MAX_VALUE;
            expected = 404;
        } else if (variant.equals("missing-incident")) {
            incidentId = Long.MAX_VALUE;
            expected = 404;
        } else if (variant.equals("missing-file")) {
            jdbcTemplate.update("delete from stored_file where id=?", fileId);
            when(classifier.predict(any())).thenAnswer(invocation -> {
                java.util.function.Supplier<?> image = invocation.getArgument(0);
                image.get();
                return null;
            });
            expected = 404;
        } else {
            when(classifier.predict(any())).thenThrow(Errors.classifierUnavailable());
            expected = 503;
        }
        mockMvc.perform(post(url(incidentId)).header(HttpHeaders.AUTHORIZATION, token("analyst"))
                        .contentType(APPLICATION_JSON).content("{\"fileId\":" + fileId + "}"))
                .andExpect(status().is(expected));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"fileId\":null}", "{\"fileId\":0}", "{\"fileId\":-1}"})
    void validatesFileIdentifier(String body) throws Exception {
        mockMvc.perform(post(url(1)).header(HttpHeaders.AUTHORIZATION, token("analyst"))
                        .contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post(url(1)).contentType(APPLICATION_JSON).content("{\"fileId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    private String token(String login) throws Exception {
        return "Bearer " + fixtures.signInAndGetToken(login, login.equals("management") ? "admin" : login);
    }

    private String url(long id) {
        return "/api/v1/incidents/" + id + "/alien-classification";
    }
}
