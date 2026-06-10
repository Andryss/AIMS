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

import java.util.List;
import java.util.Optional;

/**
 * Уведомляет создателя инцидента о необходимости уточнения данных.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyOperatorClarificationRequiredPayload.QUEUE_NAME)
public class NotifyOperatorClarificationRequiredProcessor
        implements DbQueueProcessor<NotifyOperatorClarificationRequiredPayload> {

    private final IncidentRepository incidentRepository;
    private final NotificationService notificationService;

    @Override
    public TaskExecutionResult execute(NotifyOperatorClarificationRequiredPayload payload) {
        long incidentId = payload.getIncidentId();
        Optional<IncidentEntity> incidentOpt = incidentRepository.findById(incidentId);
        if (incidentOpt.isEmpty()) {
            log.warn("Incident {} not found for operator clarification notification", incidentId);
            return TaskExecutionResult.finish();
        }
        IncidentEntity incident = incidentOpt.get();
        String message = "По инциденту №" + incidentId + " требуется уточнение";
        String relatedRef = EntityRef.format(EntityType.INCIDENT, incidentId);
        notificationService.send(incident.getCreatedByUserId(), message, List.of(relatedRef));
        return TaskExecutionResult.finish();
    }
}
