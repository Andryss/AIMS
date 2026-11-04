package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.MonitoringAlertRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueSettings;
import gov.mib.aims.backend.services.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yoomoney.tech.dbqueue.api.TaskExecutionResult;

/**
 * Отправляет in-app уведомления операторам о новом алерте внешнего мониторинга.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DbQueueSettings(NotifyOperatorsMonitoringAlertPayload.QUEUE_NAME)
public class NotifyOperatorsMonitoringAlertProcessor
        implements DbQueueProcessor<NotifyOperatorsMonitoringAlertPayload> {

    private final ChronoService chronoService;
    private final MonitoringAlertRepository monitoringAlertRepository;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public TaskExecutionResult execute(NotifyOperatorsMonitoringAlertPayload payload) {
        long alertId = payload.getMonitoringAlertId();
        MonitoringAlertEntity alert = monitoringAlertRepository.findById(alertId).orElse(null);
        if (alert == null) {
            log.warn("Monitoring alert {} not found for operator notification", alertId);
            return TaskExecutionResult.finish();
        }
        String detectedAt = chronoService.formatUtc(alert.getDetectedAt(), "yyyy-MM-dd HH:mm");
        String message = "Новая активность: " + alert.getLocation() + ", " + detectedAt;
        notificationDispatcher.sendToRole(Role.OPERATOR, message, EntityType.MONITORING_ALERT, alertId);
        return TaskExecutionResult.finish();
    }
}
