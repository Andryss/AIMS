package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAgentsIncidentReadyProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление оперативных агентов в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAgentsPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        dbQueueService.produceTask(
                NotifyAgentsIncidentReadyProcessor.class,
                new NotifyAgentsIncidentReadyPayload(context.getId())
        );
    }
}
