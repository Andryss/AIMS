package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.RoleNames;
import gov.mib.aims.backend.repository.AppUserRepository;
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
 * Уведомляет оперативных агентов о готовности инцидента к выполнению.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyAgentsIncidentReadyPayload.QUEUE_NAME)
public class NotifyAgentsIncidentReadyProcessor implements DbQueueProcessor<NotifyAgentsIncidentReadyPayload> {

    private final IncidentRepository incidentRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    @Override
    public TaskExecutionResult execute(NotifyAgentsIncidentReadyPayload payload) {
        long incidentId = payload.getIncidentId();
        if (!incidentRepository.existsById(incidentId)) {
            log.warn("Incident {} not found for agent notification", incidentId);
            return TaskExecutionResult.finish();
        }
        String message = "Инцидент №" + incidentId + " готов к выполнению";
        String relatedRef = EntityRef.format(EntityType.INCIDENT, incidentId);
        List<AppUserEntity> agents = appUserRepository.findAllByRoleName(RoleNames.AGENT);
        for (AppUserEntity agent : agents) {
            notificationService.send(agent.getId(), message, List.of(relatedRef));
        }
        return TaskExecutionResult.finish();
    }
}
