package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.BaseDbTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты Dummy-процессора db-queue.
 */
class DummyTaskProcessorTest extends BaseDbTest {

    @Autowired
    private DummyTaskProcessor processor;

    @Test
    void executeFinishesSuccessfully() {
        TaskExecutionResult result = processor.execute(new DummyTaskPayload("hello"));

        assertThat(result).isEqualTo(TaskExecutionResult.finish());
    }
}
