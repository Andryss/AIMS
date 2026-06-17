package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.MonitoringAlertRepository;
import gov.mib.aims.backend.services.NotificationService;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueSettings;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Отправляет in-app уведомления операторам о новом алерте внешнего мониторинга.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyOperatorsMonitoringAlertPayload.QUEUE_NAME)
public class NotifyOperatorsMonitoringAlertProcessor
        implements DbQueueProcessor<NotifyOperatorsMonitoringAlertPayload> {

    private static final DateTimeFormatter DETECTED_AT_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

    private final MonitoringAlertRepository monitoringAlertRepository;
    private final AppUserRepository appUserRepository;
    private final NotificationService notificationService;

    @Override
    public TaskExecutionResult execute(NotifyOperatorsMonitoringAlertPayload payload) {
        long alertId = payload.getMonitoringAlertId();
        MonitoringAlertEntity alert = monitoringAlertRepository.findById(alertId).orElse(null);
        if (alert == null) {
            log.warn("Monitoring alert {} not found for operator notification", alertId);
            return TaskExecutionResult.finish();
        }
        String detectedAt = DETECTED_AT_FORMAT.format(alert.getDetectedAt().atOffset(ZoneOffset.UTC));
        String message = "Новая активность: " + alert.getLocation() + ", " + detectedAt;
        String relatedRef = EntityRef.format(EntityType.MONITORING_ALERT, alertId);
        List<AppUserEntity> operators = appUserRepository.findAllByRoleName(Role.OPERATOR.getCode());
        for (AppUserEntity operator : operators) {
            notificationService.send(operator.getId(), message, List.of(relatedRef));
        }
        return TaskExecutionResult.finish();
    }
}
