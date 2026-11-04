package gov.mib.aims.backend.services.transaction;

import gov.mib.aims.backend.BaseDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет откат неудачной попытки и сериализуемую транзакцию повторной команды.
 */
@Import(RetryableSerializableTest.Config.class)
class RetryableSerializableTest extends BaseDbTest {

    @Autowired
    private RetriedCommand command;

    @Test
    void retriesInFreshSerializableTransactionAfterRollingBackFirstWrite() {
        String original = jdbcTemplate.queryForObject("select name from alien order by id limit 1", String.class);

        Outcome result = command.execute();

        assertThat(result.previousName()).isEqualTo(original);
        assertThat(result.attempt()).isEqualTo(2);
        assertThat(result.isolation()).isEqualTo("serializable");
        assertThat(jdbcTemplate.queryForObject("select name from alien order by id limit 1", String.class))
                .isEqualTo("Attempt 2");
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Config {

        @Bean
        RetriedCommand retriedCommand(JdbcTemplate jdbcTemplate) {
            return new RetriedCommand(jdbcTemplate);
        }
    }

    public static class RetriedCommand {

        private final JdbcTemplate jdbcTemplate;
        private final AtomicInteger attempts = new AtomicInteger();

        public RetriedCommand(JdbcTemplate jdbcTemplate) {
            this.jdbcTemplate = jdbcTemplate;
        }

        /** Выполняет настоящую запись и имитирует конфликт первой попытки. */
        @RetryableSerializable
        public Outcome execute() {
            int attempt = attempts.incrementAndGet();
            String previous = jdbcTemplate.queryForObject("select name from alien order by id limit 1", String.class);
            String isolation = jdbcTemplate.queryForObject("show transaction_isolation", String.class);
            jdbcTemplate.update("update alien set name=? where id=(select min(id) from alien)", "Attempt " + attempt);
            if (attempt == 1) {
                throw new CannotAcquireLockException("Simulated serialization conflict");
            }

            return new Outcome(previous, isolation, attempt);
        }
    }

    public record Outcome(String previousName, String isolation, int attempt) {
    }
}
