package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyIncidentPreparedProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление о готовности инцидента к исполнению в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyIncidentPreparedPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        dbQueueService.produceTask(
                NotifyIncidentPreparedProcessor.class,
                new NotifyIncidentPreparedPayload(context.getId())
        );
    }
}
