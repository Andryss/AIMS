package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyExecutorsAssignedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyExecutorsAssignedProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ставит задачу на уведомление новых исполнителей в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class ExecutorAssignmentNotifier {

    private final DbQueueService dbQueueService;

    /**
     * Уведомляет указанных исполнителей о назначении.
     *
     * @param incidentId id инцидента
     * @param newExecutorUserIds id новых исполнителей
     */
    public void notifyNewExecutors(long incidentId, List<Long> newExecutorUserIds) {
        if (newExecutorUserIds == null || newExecutorUserIds.isEmpty()) {
            return;
        }
        dbQueueService.produceTask(
                NotifyExecutorsAssignedProcessor.class,
                new NotifyExecutorsAssignedPayload(incidentId, newExecutorUserIds)
        );
    }
}
