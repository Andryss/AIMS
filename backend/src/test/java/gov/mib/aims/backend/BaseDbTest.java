package gov.mib.aims.backend;

import io.zonky.test.db.AutoConfigureEmbeddedDatabase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Базовый класс для тестов с встроенной PostgreSQL.
 */
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureEmbeddedDatabase(
        refresh = AutoConfigureEmbeddedDatabase.RefreshMode.AFTER_EACH_TEST_METHOD,
        type = AutoConfigureEmbeddedDatabase.DatabaseType.POSTGRES,
        provider = AutoConfigureEmbeddedDatabase.DatabaseProvider.ZONKY
)
public abstract class BaseDbTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    /**
     * Проверяет число задач в {@code queue_tasks} по имени очереди и фрагменту payload.
     *
     * @param queueName имя очереди
     * @param payloadContains подстрока в JSON payload
     * @param expected ожидаемое количество
     */
    protected void assertQueueTasksCount(String queueName, String payloadContains, int expected) {
        assertThat(countQueueTasks(queueName, payloadContains)).isEqualTo(expected);
    }

    /**
     * Считает задачи в {@code queue_tasks} по имени очереди и фрагменту payload.
     *
     * @param queueName имя очереди
     * @param payloadContains подстрока в JSON payload
     * @return количество строк
     */
    protected int countQueueTasks(String queueName, String payloadContains) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue_tasks WHERE queue_name = ? AND payload LIKE ?",
                Integer.class,
                queueName,
                "%" + payloadContains + "%"
        );
        return count == null ? 0 : count;
    }
}
