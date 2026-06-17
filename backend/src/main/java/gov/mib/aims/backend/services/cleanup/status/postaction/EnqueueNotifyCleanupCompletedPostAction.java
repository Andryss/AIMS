package gov.mib.aims.backend.services.cleanup.status.postaction;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedProcessor;
import gov.mib.aims.backend.services.incident.status.postaction.StatusTransitionPostAction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Ставит задачу на уведомление ответственного после завершения очистки в рамках текущей транзакции.
 */
@Component
@RequiredArgsConstructor
public class EnqueueNotifyCleanupCompletedPostAction implements StatusTransitionPostAction<IncidentEntity> {

    private final DbQueueService dbQueueService;

    @Override
    public void execute(IncidentEntity context) {
        dbQueueService.produceTask(
                NotifyCleanupCompletedProcessor.class,
                new NotifyCleanupCompletedPayload(context.getId())
        );
    }
}
