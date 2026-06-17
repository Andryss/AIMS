package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredProcessor;
import gov.mib.aims.backend.services.incident.StatusChangeCommentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление аналитиков о повторном анализе в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAnalystsReanalysisPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        dbQueueService.produceTask(
                NotifyAnalystsReanalysisRequiredProcessor.class,
                new NotifyAnalystsReanalysisRequiredPayload(
                        context.getId(),
                        StatusChangeCommentHolder.getCommentExcerpt()
                )
        );
    }
}
