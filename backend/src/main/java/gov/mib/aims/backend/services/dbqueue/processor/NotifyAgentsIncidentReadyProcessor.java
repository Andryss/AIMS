package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueSettings;
import gov.mib.aims.backend.services.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

/**
 * Уведомляет оперативных агентов о готовности инцидента к выполнению.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyAgentsIncidentReadyPayload.QUEUE_NAME)
public class NotifyAgentsIncidentReadyProcessor implements DbQueueProcessor<NotifyAgentsIncidentReadyPayload> {

    private final IncidentRepository incidentRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public TaskExecutionResult execute(NotifyAgentsIncidentReadyPayload payload) {
        long incidentId = payload.getIncidentId();
        if (!incidentRepository.existsById(incidentId)) {
            log.warn("Incident {} not found for agent notification", incidentId);
            return TaskExecutionResult.finish();
        }
        String message = "Инцидент №" + incidentId + " готов к выполнению";
        notificationDispatcher.sendToRole(Role.AGENT, message, EntityType.INCIDENT, incidentId);
        return TaskExecutionResult.finish();
    }
}
