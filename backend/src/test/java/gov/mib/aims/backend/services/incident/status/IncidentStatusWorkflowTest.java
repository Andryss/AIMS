package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.BaseException;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.dbqueue.DbQueueProcessor;
import gov.mib.aims.backend.services.dbqueue.DbQueueService;
import gov.mib.aims.backend.services.dbqueue.QueuePayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyAnalystsReanalysisRequiredPayload;
import gov.mib.aims.backend.services.dbqueue.processor.NotifyOperatorClarificationRequiredPayload;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyAgentsPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyAnalystsPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyAnalystsReanalysisPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyIncidentPreparedPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyOperatorClarificationPostAction;
import gov.mib.aims.backend.services.incident.status.precondition.AlienLinkedPrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.AssignmentCompletePrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.AttachmentsExistPrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.EquipmentReturnedPrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.ResponsibleAgentPrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.RolePrecondition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IncidentStatusWorkflowTest {

    private final RecordingQueue queue = new RecordingQueue();
    private final ChronoService chronoService = new ChronoService(
            Clock.fixed(Instant.parse("2026-11-05T10:00:00Z"), ZoneOffset.UTC));
    private final IncidentStatusWorkflow workflow = new IncidentStatusWorkflow(graph(), chronoService);

    @Test
    void changingStatusMutatesWithoutPersistenceOrNotification() {
        IncidentEntity incident = IncidentEntity.builder()
                .id(42L).status(IncidentStatus.READY_FOR_ANALYSIS).build();

        assertThatCode(() -> workflow.changeStatus(incident, IncidentStatus.CLARIFICATION_REQUIRED,
                "Explain the sighting"))
                .doesNotThrowAnyException();

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.CLARIFICATION_REQUIRED);
        assertThat(incident.getUpdatedAt()).isEqualTo(chronoService.localDateTimeNow());
        assertThat(queue.payloads).isEmpty();
    }

    @Test
    void afterSaveUsesEachIncidentsExplicitCommentWhenTransitionsAreInterleaved() {
        IncidentEntity first = IncidentEntity.builder().id(42L).status(IncidentStatus.READY_FOR_ANALYSIS).build();
        IncidentEntity second = IncidentEntity.builder().id(43L).status(IncidentStatus.READY_FOR_EXECUTION).build();
        String firstComment = "  Need a clearer location  ";
        String secondComment = "a".repeat(121);

        workflow.changeStatus(first, IncidentStatus.CLARIFICATION_REQUIRED, firstComment);
        workflow.changeStatus(second, IncidentStatus.REANALYSIS_REQUIRED, secondComment);
        assertThat(queue.payloads).isEmpty();

        workflow.afterSave(second, IncidentStatus.READY_FOR_EXECUTION, secondComment);
        workflow.afterSave(first, IncidentStatus.READY_FOR_ANALYSIS, firstComment);

        assertThat(queue.payloads).hasSize(2);
        assertThat(queue.payloads.get(0)).isInstanceOfSatisfying(NotifyAnalystsReanalysisRequiredPayload.class,
                payload -> {
                    assertThat(payload.getIncidentId()).isEqualTo(43L);
                    assertThat(payload.getCommentExcerpt()).isEqualTo("a".repeat(120) + "…");
                });
        assertThat(queue.payloads.get(1)).isInstanceOfSatisfying(NotifyOperatorClarificationRequiredPayload.class,
                payload -> {
                    assertThat(payload.getIncidentId()).isEqualTo(42L);
                    assertThat(payload.getCommentExcerpt()).isEqualTo("Need a clearer location");
                });
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n", "\0"})
    void absentCommentDoesNotReuseAnotherTransitionsComment(String comment) {
        IncidentEntity incident = IncidentEntity.builder().id(42L).status(IncidentStatus.READY_FOR_ANALYSIS).build();
        workflow.changeStatus(incident, IncidentStatus.CLARIFICATION_REQUIRED, "Previous explanation");
        workflow.afterSave(incident, IncidentStatus.READY_FOR_ANALYSIS, "Previous explanation");
        IncidentEntity second = IncidentEntity.builder().id(43L).status(IncidentStatus.READY_FOR_ANALYSIS).build();
        workflow.changeStatus(second, IncidentStatus.CLARIFICATION_REQUIRED, comment);
        workflow.afterSave(second, IncidentStatus.READY_FOR_ANALYSIS, comment);

        assertThat(queue.payloads.get(1)).isInstanceOfSatisfying(NotifyOperatorClarificationRequiredPayload.class,
                payload -> assertThat(payload.getCommentExcerpt()).isNull());
    }

    @Test
    void invalidTransitionDoesNotMutateOrNotify() {
        IncidentEntity incident = IncidentEntity.builder().id(42L).status(IncidentStatus.DRAFT)
                .updatedAt(chronoService.localDateTimeNow().minusDays(1)).build();
        var originalUpdatedAt = incident.getUpdatedAt();

        assertThatThrownBy(() -> workflow.changeStatus(incident, IncidentStatus.EXECUTING, "Invalid"))
                .isInstanceOfSatisfying(BaseException.class, error -> {
                    assertThat(error.getCode()).isEqualTo(400);
                    assertThat(error.getMessage()).isEqualTo("incident.invalid_status_transition");
                });

        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.DRAFT);
        assertThat(incident.getUpdatedAt()).isEqualTo(originalUpdatedAt);
        assertThat(queue.payloads).isEmpty();
    }

    private IncidentStatusTransitionGraph graph() {
        CurrentUserService currentUser = new CurrentUserService(null) {
            @Override
            public boolean hasAnyRole(Role... roles) {
                return true;
            }
        };
        return new IncidentStatusTransitionGraph(
                new AttachmentsExistPrecondition(null),
                new RolePrecondition(currentUser, Role.OPERATOR),
                new RolePrecondition(currentUser, Role.ANALYST),
                new RolePrecondition(currentUser, Role.AGENT),
                new AlienLinkedPrecondition(),
                new AssignmentCompletePrecondition(),
                new EnqueueNotifyAnalystsPostAction(queue),
                new EnqueueNotifyAgentsPostAction(queue),
                new EnqueueNotifyOperatorClarificationPostAction(queue),
                new EnqueueNotifyAnalystsReanalysisPostAction(queue),
                new EnqueueNotifyIncidentPreparedPostAction(queue),
                new ResponsibleAgentPrecondition(currentUser),
                new EquipmentReturnedPrecondition(null, null));
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
