package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.MonitoringAlertStatus;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.MonitoringAlertRepository;
import gov.mib.aims.backend.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты {@link NotifyOperatorsMonitoringAlertProcessor}.
 */
class NotifyOperatorsMonitoringAlertProcessorTest extends BaseDbTest {

    @Autowired
    private NotifyOperatorsMonitoringAlertProcessor processor;

    @Autowired
    private MonitoringAlertRepository monitoringAlertRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void sendsNotificationToAllOperators() {
        LocalDateTime now = LocalDateTime.now();
        MonitoringAlertEntity alert = monitoringAlertRepository.save(MonitoringAlertEntity.builder()
                .externalEventId("processor-test-001")
                .sourceSystem("EXTERNAL_MONITORING_V1")
                .status(MonitoringAlertStatus.NEW)
                .eventType(IncidentEventType.UNIDENTIFIED_SIGHTING)
                .location("Test location")
                .detectedAt(now)
                .description("Test description")
                .mediaUrls(List.of())
                .rawPayload(Map.of("externalEventId", "processor-test-001"))
                .receivedAt(now)
                .createdAt(now)
                .build());

        processor.execute(new NotifyOperatorsMonitoringAlertPayload(alert.getId()));

        long operatorId = appUserRepository.findByLogin("operator").orElseThrow().getId();
        var notifications = notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(
                operatorId,
                org.springframework.data.domain.PageRequest.of(0, 10)
        );
        assertThat(notifications.getContent())
                .anyMatch(n -> n.getMessage().contains("Test location")
                        && n.getRelatedEntities().contains("MONITORING_ALERT:" + alert.getId()));
    }
}
