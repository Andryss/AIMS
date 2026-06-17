package gov.mib.aims.backend.services.incident.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsIncidentReadyProcessor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу в db-queue на уведомление аналитиков в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyAnalystsPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        dbQueueService.produceTask(
                NotifyAnalystsIncidentReadyProcessor.class,
                new NotifyAnalystsIncidentReadyPayload(context.getId())
        );
    }
}
