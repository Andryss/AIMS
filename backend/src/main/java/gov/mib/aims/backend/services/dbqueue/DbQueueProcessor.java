package gov.mib.aims.backend.services.dbqueue;

import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

/**
 * Процессор задач db-queue.
 *
 * @param <P> тип payload
 */
public interface DbQueueProcessor<P extends QueuePayload> {

    /**
     * Выполняет задачу из очереди.
     *
     * @param payload данные задачи
     * @return результат выполнения
     */
    TaskExecutionResult execute(P payload);
}
