package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredProcessor;
import gov.mib.aims.backend.services.incident.StatusChangeCommentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Ставит задачу на уведомление аналитиков о повторном анализе после commit.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAnalystsReanalysisPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        NotifyAnalystsReanalysisRequiredPayload payload = new NotifyAnalystsReanalysisRequiredPayload(
                context.getId(),
                StatusChangeCommentHolder.getCommentExcerpt()
        );
        enqueueAfterCommit(payload);
    }

    private void enqueueAfterCommit(NotifyAnalystsReanalysisRequiredPayload payload) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    dbQueueService.produceTask(NotifyAnalystsReanalysisRequiredProcessor.class, payload);
                }
            });
        } else {
            dbQueueService.produceTask(NotifyAnalystsReanalysisRequiredProcessor.class, payload);
        }
    }
}
