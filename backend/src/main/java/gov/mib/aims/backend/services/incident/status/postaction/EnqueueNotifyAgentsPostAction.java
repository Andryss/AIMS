package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ставит задачу на уведомление оперативных агентов после commit.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAgentsPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        NotifyAgentsIncidentReadyPayload payload = new NotifyAgentsIncidentReadyPayload(context.getId());
        enqueueAfterCommit(payload);
    }

    private void enqueueAfterCommit(NotifyAgentsIncidentReadyPayload payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dbQueueService.produceTask(NotifyAgentsIncidentReadyProcessor.class, payload);
                }
            });
        } else {
            dbQueueService.produceTask(NotifyAgentsIncidentReadyProcessor.class, payload);
        }
    }
}
