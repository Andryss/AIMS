package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredProcessor;
import gov.mib.aims.backend.services.incident.StatusChangeCommentHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление создателя инцидента в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyOperatorClarificationPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        dbQueueService.produceTask(
                NotifyOperatorClarificationRequiredProcessor.class,
                new NotifyOperatorClarificationRequiredPayload(
                        context.getId(),
                        StatusChangeCommentHolder.getCommentExcerpt()
                )
        );
    }
}
