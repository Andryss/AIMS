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
 * Отправляет in-app уведомления аналитикам о готовности инцидента к анализу.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyAnalystsIncidentReadyPayload.QUEUE_NAME)
public class NotifyAnalystsIncidentReadyProcessor implements DbQueueProcessor<NotifyAnalystsIncidentReadyPayload> {

    private final IncidentRepository incidentRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public TaskExecutionResult execute(NotifyAnalystsIncidentReadyPayload payload) {
        long incidentId = payload.getIncidentId();
        if (!incidentRepository.existsById(incidentId)) {
            log.warn("Incident {} not found for analyst notification", incidentId);
            return TaskExecutionResult.finish();
        }
        String message = "Новый инцидент №" + incidentId + " готов к анализу";
        notificationDispatcher.sendToRole(Role.ANALYST, message, EntityType.INCIDENT, incidentId);
        return TaskExecutionResult.finish();
    }
}
