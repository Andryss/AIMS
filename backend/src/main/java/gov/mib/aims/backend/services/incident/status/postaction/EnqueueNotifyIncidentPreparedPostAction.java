package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ставит задачу на уведомление ответственного и исполнителей после commit.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyIncidentPreparedPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        NotifyIncidentPreparedPayload payload = new NotifyIncidentPreparedPayload(context.getId());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dbQueueService.produceTask(NotifyIncidentPreparedProcessor.class, payload);
                }
            });
        } else {
            dbQueueService.produceTask(NotifyIncidentPreparedProcessor.class, payload);
        }
    }
}
