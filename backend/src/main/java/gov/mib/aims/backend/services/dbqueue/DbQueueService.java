package gov.mib.aims.backend.services.dbqueue;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import gov.mib.aims.backend.config.dbqueue.QueueProducerBase;
import org.springframework.stereotype.Service;
import ru.yoomoney.tech.dbqueue.api.EnqueueParams;

/**
 * Фасад для постановки задач в db-queue.
 */
@Service
public class DbQueueService {

    private final Map<Class<?>, QueueProducerBase<?>> producers;

    public DbQueueService(List<? extends QueueProducerBase<?>> producers) {
        this.producers = producers.stream()
                .collect(Collectors.toMap(QueueProducerBase::getProcessorClass, Function.identity()));
    }

    /**
     * Ставит задачу в очередь для обработки.
     *
     * @param processorClass класс процессора
     * @param payload данные задачи
     */
    @SuppressWarnings("unchecked")
    public <P extends QueuePayload> void produceTask(Class<? extends DbQueueProcessor<P>> processorClass, P payload) {
        QueueProducerBase<P> producer = (QueueProducerBase<P>) producers.get(processorClass);
        producer.enqueue(EnqueueParams.create(payload));
    }
}
