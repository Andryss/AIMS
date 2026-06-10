package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ставит задачу на уведомление создателя инцидента после commit.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyOperatorClarificationPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        NotifyOperatorClarificationRequiredPayload payload =
                new NotifyOperatorClarificationRequiredPayload(context.getId());
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dbQueueService.produceTask(NotifyOperatorClarificationRequiredProcessor.class, payload);
                }
            });
        } else {
            dbQueueService.produceTask(NotifyOperatorClarificationRequiredProcessor.class, payload);
        }
    }
}
