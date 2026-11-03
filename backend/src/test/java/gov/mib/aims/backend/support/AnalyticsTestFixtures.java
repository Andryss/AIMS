package gov.mib.aims.backend.support;

import com.fasterxml.jackson.databind.node.ObjectNode;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * Реальные сущности API и управляемые исторические снимки для аналитики.
 */
public class AnalyticsTestFixtures {

    private final JdbcTemplate jdbc;
    private final ObjectMapperWrapper mapper;
    private final ApiTestFixtures fixtures;
    private final MockMvc mvc;

    public AnalyticsTestFixtures(JdbcTemplate jdbc, ObjectMapperWrapper mapper, ApiTestFixtures fixtures, MockMvc mvc) {
        this.jdbc = jdbc;
        this.mapper = mapper;
        this.fixtures = fixtures;
        this.mvc = mvc;
    }

    /**
     * Создаёт инцидент обычным API.
     */
    public long incident() throws Exception {
        String operator = fixtures.signInAndGetToken("operator", "operator");
        return fixtures.createIncident(operator, fixtures.uploadFile(operator));
    }

    /**
     * Создаёт сущность доменным API от администратора.
     */
    public long create(String path, Map<String, Object> body) throws Exception {
        String token = fixtures.signInAndGetToken("admin", "admin");
        var result = mvc.perform(post("/api/v1/" + path).header("Authorization", "Bearer " + token)
                .contentType(APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isCreated()).andReturn();
        return mapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    /**
     * Возвращает независимую копию первого снимка, сохраняя настоящий формат аудита.
     */
    public ObjectNode snapshot(String type, long id) {
        String value = jdbc.queryForObject("select snapshot::text from entity_history"
                + " where entity_type=? and entity_id=? order by id limit 1", String.class, type, id);
        return (ObjectNode) mapper.readTree(value);
    }

    /**
     * Удаляет только тестовую историю сущности перед размещением временных фикстур.
     */
    public void clearHistory(String type, long id) {
        jdbc.update("delete from entity_history where entity_type=? and entity_id=?", type, id);
    }

    /**
     * Добавляет снимок в заданный момент изолированной тестовой БД.
     */
    public void history(String type, long id, Instant at, ObjectNode snapshot) {
        jdbc.update("""
                insert into entity_history(entity_type,entity_id,snapshot,changed_by_user_id,changed_at)
                values (?,?,cast(? as jsonb),(select id from app_user where login='admin'),?)
                """, type, id, snapshot.toString(), Timestamp.from(at));
    }
}
