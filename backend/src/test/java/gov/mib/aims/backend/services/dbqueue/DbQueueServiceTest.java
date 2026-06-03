package gov.mib.aims.backend.services.dbqueue;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.services.dbqueue.processor.DummyTaskPayload;
import gov.mib.aims.backend.services.dbqueue.processor.DummyTaskProcessor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты постановки задач в db-queue.
 */
class DbQueueServiceTest extends BaseDbTest {

    @Autowired
    private DbQueueService dbQueueService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void produceTaskInsertsRowIntoQueueTasks() {
        dbQueueService.produceTask(DummyTaskProcessor.class, new DummyTaskPayload("enqueue-test"));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM queue_tasks WHERE queue_name = ? AND payload LIKE ?",
                Integer.class,
                DummyTaskPayload.QUEUE_NAME,
                "%enqueue-test%"
        );

        assertThat(count).isEqualTo(1);
    }
}
