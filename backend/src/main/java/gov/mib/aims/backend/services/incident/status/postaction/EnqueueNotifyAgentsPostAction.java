package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyProcessor;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPostAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление оперативных агентов в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAgentsPostAction implements StatusTransitionPostAction<IncidentTransitionContext> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentTransitionContext context) {
        dbQueueService.produceTask(
                NotifyAgentsIncidentReadyProcessor.class,
                new NotifyAgentsIncidentReadyPayload(context.incident().getId())
        );
    }
}
