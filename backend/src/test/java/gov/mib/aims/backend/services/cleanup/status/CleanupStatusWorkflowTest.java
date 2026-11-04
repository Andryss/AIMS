package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.cleanup.status.postaction.EnqueueNotifyCleanupCompletedPostAction;
import gov.mib.aims.backend.services.cleanup.status.precondition.CleanupReportExistsPrecondition;
import gov.mib.aims.backend.services.cleanup.status.precondition.IncidentExecutingPrecondition;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyCleanupCompletedPayload;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CleanupStatusWorkflowTest {

    private final RecordingQueue queue = new RecordingQueue();
    private final ChronoService chronoService = new ChronoService(
            Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC));
    private final CleanupStatusWorkflow workflow = new CleanupStatusWorkflow(
            new CleanupStatusTransitionGraph(new IncidentExecutingPrecondition(), new CleanupReportExistsPrecondition(),
                    new EnqueueNotifyCleanupCompletedPostAction(queue)), chronoService);

    @Test
    void completionDefersNotificationUntilAfterSave() {
        IncidentEntity incident = IncidentEntity.builder().id(42L).status(IncidentStatus.EXECUTING)
                .cleanupStatus(CleanupStatus.EXECUTION).cleanupReportId(10L).build();

        assertThat(workflow.changeStatus(incident, CleanupStatus.COMPLETED)).isSameAs(incident);

        assertThat(incident.getCleanupStatus()).isEqualTo(CleanupStatus.COMPLETED);
        assertThat(incident.getUpdatedAt()).isEqualTo(chronoService.localDateTimeNow());
        assertThat(queue.payloads).isEmpty();

        workflow.afterSave(incident, CleanupStatus.EXECUTION);

        assertThat(queue.payloads).singleElement().isInstanceOfSatisfying(NotifyCleanupCompletedPayload.class,
                payload -> assertThat(payload.getIncidentId()).isEqualTo(42L));
    }

    @Test
    void missingReportDoesNotMutateOrNotify() {
        IncidentEntity incident = IncidentEntity.builder().id(42L).status(IncidentStatus.EXECUTING)
                .cleanupStatus(CleanupStatus.EXECUTION).build();

        assertThatThrownBy(() -> workflow.changeStatus(incident, CleanupStatus.COMPLETED))
                .isInstanceOfSatisfying(BaseException.class,
                        error -> assertThat(error.getMessage()).isEqualTo("validation.error"));

        assertThat(incident.getCleanupStatus()).isEqualTo(CleanupStatus.EXECUTION);
        assertThat(incident.getUpdatedAt()).isNull();
        assertThat(queue.payloads).isEmpty();
    }

    private static final class RecordingQueue extends DbQueueService {
        private final List<QueuePayload> payloads = new ArrayList<>();

        private RecordingQueue() {
            super(List.of());
        }

        @Override
        public <P extends QueuePayload> void produceTask(Class<? extends DbQueueProcessor<P>> processorClass,
                                                        P payload) {
            payloads.add(payload);
        }
    }
}
