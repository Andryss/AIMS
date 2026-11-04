package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedProcessor;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPostAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление о готовности инцидента к исполнению в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyIncidentPreparedPostAction implements StatusTransitionPostAction<IncidentTransitionContext> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentTransitionContext context) {
        dbQueueService.produceTask(
                NotifyIncidentPreparedProcessor.class,
                new NotifyIncidentPreparedPayload(context.incident().getId())
        );
    }
}
