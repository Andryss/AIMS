package gov.mib.aims.backend.config.dbqueue;

import lombok.Getter;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;
import ru.yoomoney.tech.dbqueue.api.EnqueueResult;
import ru.yoomoney.tech.dbqueue.api.QueueProducer;
import ru.yoomoney.tech.dbqueue.api.TaskPayloadTransformer;
import ru.yoomoney.tech.dbqueue.api.impl.ShardingQueueProducer;
import ru.yoomoney.tech.dbqueue.api.impl.SingleQueueShardRouter;
import ru.yoomoney.tech.dbqueue.config.QueueShard;
import ru.yoomoney.tech.dbqueue.settings.QueueConfig;

/**
 * Производитель задач db-queue.
 */
public class QueueProducerBase<P extends QueuePayload> implements QueueProducer<P> {

    @Getter
    private final QueueConfig queueConfig;
    @Getter
    private final TaskPayloadTransformer<P> payloadTransformer;
    @Getter
    private final Class<? extends DbQueueProcessor<?>> processorClass;
    private final ShardingQueueProducer<P, ?> queueProducer;

    @SuppressWarnings("unchecked")
    public QueueProducerBase(
            QueueConfig queueConfig,
            TaskPayloadTransformer<P> payloadTransformer,
            DbQueueProcessor<P> processor,
            QueueShard<?> queueShard
    ) {
        this.queueConfig = queueConfig;
        this.payloadTransformer = payloadTransformer;
        this.processorClass = (Class<? extends DbQueueProcessor<?>>) processor.getClass();
        this.queueProducer = new ShardingQueueProducer<>(
                queueConfig,
                payloadTransformer,
                new SingleQueueShardRouter<>(queueShard)
        );
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public EnqueueResult enqueue(EnqueueParams<P> enqueueParams) {
        return queueProducer.enqueue(enqueueParams);
    }
}
