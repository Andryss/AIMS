package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.NotificationService;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import java.util.List;

/**
 * Уведомляет исполнителей о назначении на инцидент.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyExecutorsAssignedPayload.QUEUE_NAME)
public class NotifyExecutorsAssignedProcessor implements DbQueueProcessor<NotifyExecutorsAssignedPayload> {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    @Override
    public TaskExecutionResult execute(NotifyExecutorsAssignedPayload payload) {
        long incidentId = payload.getIncidentId();
        if (!incidentRepository.existsById(incidentId)) {
            log.warn("Incident {} not found for executor notification", incidentId);
            return TaskExecutionResult.finish();
        }
        String message = "Вы назначены исполнителем по инциденту №" + incidentId;
        String relatedRef = EntityRef.format(EntityType.INCIDENT, incidentId);
        for (Long userId : payload.getExecutorUserIds()) {
            notificationService.send(userId, message, List.of(relatedRef));
        }
        return TaskExecutionResult.finish();
    }
}
