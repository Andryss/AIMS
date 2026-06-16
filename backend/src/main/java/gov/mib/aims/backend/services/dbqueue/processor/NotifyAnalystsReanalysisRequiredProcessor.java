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
 * Уведомляет аналитиков о необходимости повторного анализа инцидента.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyAnalystsReanalysisRequiredPayload.QUEUE_NAME)
public class NotifyAnalystsReanalysisRequiredProcessor
        implements DbQueueProcessor<NotifyAnalystsReanalysisRequiredPayload> {

    private final IncidentRepository incidentRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    @Override
    public TaskExecutionResult execute(NotifyAnalystsReanalysisRequiredPayload payload) {
        long incidentId = payload.getIncidentId();
        if (!incidentRepository.existsById(incidentId)) {
            log.warn("Incident {} not found for reanalysis notification", incidentId);
            return TaskExecutionResult.finish();
        }
        String message = "По инциденту №" + incidentId + " требуется повторный анализ";
        if (payload.getCommentExcerpt() != null && !payload.getCommentExcerpt().isBlank()) {
            message += ": " + payload.getCommentExcerpt();
        }
        String relatedRef = EntityRef.format(EntityType.INCIDENT, incidentId);
        List<AppUserEntity> analysts = appUserRepository.findAllByRoleName(RoleNames.ANALYST);
        for (AppUserEntity analyst : analysts) {
            notificationService.send(analyst.getId(), message, List.of(relatedRef));
        }
        return TaskExecutionResult.finish();
    }
}
