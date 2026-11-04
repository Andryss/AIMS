package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueSettings;
import gov.mib.aims.backend.services.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

/**
 * Уведомляет исполнителей о назначении на инцидент.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyExecutorsAssignedPayload.QUEUE_NAME)
public class NotifyExecutorsAssignedProcessor implements DbQueueProcessor<NotifyExecutorsAssignedPayload> {

    private final IncidentRepository incidentRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public TaskExecutionResult execute(NotifyExecutorsAssignedPayload payload) {
        long incidentId = payload.getIncidentId();
        if (!incidentRepository.existsById(incidentId)) {
            log.warn("Incident {} not found for executor notification", incidentId);
            return TaskExecutionResult.finish();
        }
        String message = "Вы назначены исполнителем по инциденту №" + incidentId;
        notificationDispatcher.sendToUsers(
                payload.getExecutorUserIds(), message, EntityType.INCIDENT, incidentId);
        return TaskExecutionResult.finish();
    }
}
