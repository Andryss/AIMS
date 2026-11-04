package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredProcessor;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPostAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление аналитиков о повторном анализе в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAnalystsReanalysisPostAction
        implements StatusTransitionPostAction<IncidentTransitionContext> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentTransitionContext context) {
        dbQueueService.produceTask(
                NotifyAnalystsReanalysisRequiredProcessor.class,
                new NotifyAnalystsReanalysisRequiredPayload(
                        context.incident().getId(),
                        context.commentExcerpt()
                )
        );
    }
}
