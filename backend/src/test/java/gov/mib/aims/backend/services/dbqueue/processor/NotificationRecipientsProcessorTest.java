package gov.mib.aims.backend.services.dbqueue.processor;

import gov.mib.aims.backend.BaseDbTest;
import gov.mib.aims.backend.config.TestChronoConfig;
import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Проверяет выбор и устранение повторов явных получателей уведомлений.
 */
class NotificationRecipientsProcessorTest extends BaseDbTest {

    @Autowired
    private NotifyExecutorsAssignedProcessor executorsAssignedProcessor;

    @Autowired
    private NotifyIncidentPreparedProcessor incidentPreparedProcessor;

    @Autowired
    private NotifyCleanupCompletedProcessor cleanupCompletedProcessor;

    @Autowired
    private IncidentRepository incidentRepository;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Test
    void duplicateExecutorRecipientsPersistOneNotificationUsingPayloadSnapshot() {
        Long assignedUserId = userId("agent");
        IncidentEntity incident = createIncident();
        incident.setExecutorUserIds(List.of(userId("agent2")));
        incidentRepository.save(incident);

        executorsAssignedProcessor.execute(new NotifyExecutorsAssignedPayload(
                incident.getId(), List.of(assignedUserId, assignedUserId)));

        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(assignedUserId);
            assertThat(notification.getMessage())
                    .isEqualTo("Вы назначены исполнителем по инциденту №" + incident.getId());
            assertThat(notification.getRelatedEntities()).containsExactly("INCIDENT:" + incident.getId());
        });
    }

    @Test
    void missingExecutorRecipientsDoNotCreateNotifications() {
        Long incidentId = createIncident().getId();

        executorsAssignedProcessor.execute(new NotifyExecutorsAssignedPayload(incidentId, null));
        executorsAssignedProcessor.execute(new NotifyExecutorsAssignedPayload(incidentId, List.of()));
        executorsAssignedProcessor.execute(new NotifyExecutorsAssignedPayload(incidentId, Arrays.asList(null, null)));

        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @Test
    void preparedNotificationDeduplicatesCurrentResponsibleAndExecutors() {
        Long responsibleUserId = userId("agent");
        Long executorUserId = userId("agent2");
        IncidentEntity incident = createIncident();
        NotifyIncidentPreparedPayload payload = new NotifyIncidentPreparedPayload(incident.getId());
        incident.setResponsibleUserId(responsibleUserId);
        incident.setExecutorUserIds(List.of(responsibleUserId, executorUserId, executorUserId));
        incidentRepository.save(incident);

        incidentPreparedProcessor.execute(payload);

        assertThat(notificationRepository.findAll())
                .extracting(notification -> notification.getRecipientUserId())
                .containsExactlyInAnyOrder(responsibleUserId, executorUserId);
    }

    @Test
    void cleanupNotificationUsesCurrentResponsibleAndSkipsMissingResponsible() {
        IncidentEntity incident = createIncident();
        NotifyCleanupCompletedPayload payload = new NotifyCleanupCompletedPayload(incident.getId());
        cleanupCompletedProcessor.execute(payload);
        assertThat(notificationRepository.findAll()).isEmpty();

        Long responsibleUserId = userId("agent2");
        incident.setResponsibleUserId(responsibleUserId);
        incidentRepository.save(incident);
        cleanupCompletedProcessor.execute(payload);

        assertThat(notificationRepository.findAll()).singleElement().satisfies(notification -> {
            assertThat(notification.getRecipientUserId()).isEqualTo(responsibleUserId);
            assertThat(notification.getMessage()).isEqualTo("По инциденту №" + incident.getId() + " завершена очистка");
        });
    }

    private Long userId(String login) {
        return appUserRepository.findByLogin(login).orElseThrow().getId();
    }

    private IncidentEntity createIncident() {
        return incidentRepository.save(IncidentEntity.builder()
                .status(IncidentStatus.READY_FOR_ANALYSIS)
                .eventType(IncidentEventType.UNIDENTIFIED_SIGHTING)
                .location("Notification test location")
                .detectedAt(TestChronoConfig.FIXED_LOCAL_DATE_TIME)
                .description("Notification test incident")
                .createdByUserId(userId("operator"))
                .createdAt(TestChronoConfig.FIXED_LOCAL_DATE_TIME)
                .updatedAt(TestChronoConfig.FIXED_LOCAL_DATE_TIME)
                .build());
    }
}
