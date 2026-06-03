package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueSettings;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

/**
 * Тестовый процессор db-queue (проверка wiring).
 */
@Component
@DbQueueSettings(DummyTaskPayload.QUEUE_NAME)
public class DummyTaskProcessor implements DbQueueProcessor<DummyTaskPayload> {

    @Override
    public TaskExecutionResult execute(DummyTaskPayload payload) {
        return TaskExecutionResult.finish();
    }
}
