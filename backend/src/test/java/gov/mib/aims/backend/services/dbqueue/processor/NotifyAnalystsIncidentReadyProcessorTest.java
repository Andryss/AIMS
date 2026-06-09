package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.config.TestClockConfig;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты процессора уведомлений аналитикам.
 */
class NotifyAnalystsIncidentReadyProcessorTest extends BaseDbTest {

    @Autowired
    private NotifyAnalystsIncidentReadyProcessor processor;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void executeSendsNotificationToAnalyst() {
        Long operatorId = appUserRepository.findByLogin("operator").orElseThrow().getId();
        Long analystId = appUserRepository.findByLogin("analyst").orElseThrow().getId();
        LocalDateTime now = TestClockConfig.FIXED_LOCAL_DATE_TIME;
        IncidentEntity incident = incidentRepository.save(IncidentEntity.builder()
                .status(IncidentStatus.READY_FOR_ANALYSIS)
                .eventType(IncidentEventType.UNIDENTIFIED_SIGHTING)
                .location("Test location")
                .detectedAt(now)
                .description("Test description")
                .attachmentFileIds(List.of())
                .createdByUserId(operatorId)
                .createdAt(now)
                .updatedAt(now)
                .build());

        processor.execute(new NotifyAnalystsIncidentReadyPayload(incident.getId()));

        assertThat(notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(analystId, Pageable.unpaged()))
                .hasSize(1)
                .first()
                .satisfies(n -> {
                    assertThat(n.getMessage()).contains(String.valueOf(incident.getId()));
                    assertThat(n.getRelatedEntities()).contains("INCIDENT:" + incident.getId());
                });
    }
}
