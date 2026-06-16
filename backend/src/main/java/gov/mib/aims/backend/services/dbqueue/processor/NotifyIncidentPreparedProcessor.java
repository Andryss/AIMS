package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.entity.IncidentEntity;
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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Уведомляет ответственного и исполнителей о подготовке инцидента к выполнению.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyIncidentPreparedPayload.QUEUE_NAME)
public class NotifyIncidentPreparedProcessor implements DbQueueProcessor<NotifyIncidentPreparedPayload> {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    @Override
    public TaskExecutionResult execute(NotifyIncidentPreparedPayload payload) {
        long incidentId = payload.getIncidentId();
        Optional<IncidentEntity> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            log.warn("Incident {} not found for prepared notification", incidentId);
            return TaskExecutionResult.finish();
        }
        IncidentEntity incident = incidentOpt.get();
        String message = "Инцидент №" + incidentId + " подготовлен к выполнению";
        String relatedRef = EntityRef.format(EntityType.INCIDENT, incidentId);
        Set<Long> recipientIds = new LinkedHashSet<>();
        if (incident.getResponsibleUserId() != null) {
            recipientIds.add(incident.getResponsibleUserId());
        }
        if (incident.getExecutorUserIds() != null) {
            recipientIds.addAll(incident.getExecutorUserIds());
        }
        for (Long userId : recipientIds) {
            notificationService.send(userId, message, List.of(relatedRef));
        }
        return TaskExecutionResult.finish();
    }
}
