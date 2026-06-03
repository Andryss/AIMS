package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ставит задачу в db-queue на уведомление аналитиков после commit.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAnalystsPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        NotifyAnalystsIncidentReadyPayload payload = new NotifyAnalystsIncidentReadyPayload(context.getId());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dbQueueService.produceTask(NotifyAnalystsIncidentReadyProcessor.class, payload);
                }
            });
        } else {
            dbQueueService.produceTask(NotifyAnalystsIncidentReadyProcessor.class, payload);
        }
    }
}
