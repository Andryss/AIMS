package gov.mib.aims.backend.services.cleanup.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedProcessor;
import gov.mib.aims.backend.services.incident.status.postaction.StatusTransitionPostAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ставит задачу на уведомление ответственного после завершения очистки.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyCleanupCompletedPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        NotifyCleanupCompletedPayload payload = new NotifyCleanupCompletedPayload(context.getId());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dbQueueService.produceTask(NotifyCleanupCompletedProcessor.class, payload);
                }
            });
        } else {
            dbQueueService.produceTask(NotifyCleanupCompletedProcessor.class, payload);
        }
    }
}
