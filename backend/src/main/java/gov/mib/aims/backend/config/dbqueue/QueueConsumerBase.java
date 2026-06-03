package gov.mib.aims.backend.config.dbqueue;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import ru.yoomoney.tech.dbqueue.api.QueueConsumer;
import ru.yoomoney.tech.dbqueue.api.Task;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer;
import ru.yoomoney.tech.dbqueue.settings.QueueConfig;

/**
 * Потребитель задач db-queue.
 */
@RequiredArgsConstructor
public class QueueConsumerBase<P extends QueuePayload> implements QueueConsumer<P> {

    @Getter
    private final QueueConfig queueConfig;
    @Getter
    private final TaskPayloadTransformer<P> payloadTransformer;
    private final DbQueueProcessor<P> processor;

    @Override
    @SuppressWarnings("NullableProblems")
    public TaskExecutionResult execute(Task<P> task) {
        return processor.execute(task.getPayloadOrThrow());
    }
}
