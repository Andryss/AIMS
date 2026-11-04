package gov.mib.aims.backend.services.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.IncidentStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AbstractGraphStatusWorkflowTest {

    @Test
    void changingStatusDefersPostActionsUntilAfterPersistence() {
        List<Long> notifiedIncidentIds = new ArrayList<>();
        IncidentEntity incident = IncidentEntity.builder().id(42L).status(IncidentStatus.DRAFT).build();
        StatusTransition<IncidentEntity, IncidentStatus> transition =
                StatusTransition.<IncidentEntity, IncidentStatus>builder()
                        .from(IncidentStatus.DRAFT)
                        .to(IncidentStatus.READY_FOR_ANALYSIS)
                        .preconditions(List.of())
                        .postActions(List.of(entity -> notifiedIncidentIds.add(entity.getId())))
                        .build();
        var workflow = new AbstractGraphStatusWorkflow<IncidentEntity, IncidentStatus>() {
            @Override
            protected IncidentEntity applyStatus(IncidentEntity entity, IncidentStatus target) {
                entity.setStatus(target);
                return entity;
            }
        };

        IncidentEntity changed = workflow.applyTransition(incident, transition);

        assertThat(changed).isSameAs(incident);
        assertThat(changed.getStatus()).isEqualTo(IncidentStatus.READY_FOR_ANALYSIS);
        assertThat(notifiedIncidentIds).isEmpty();

        workflow.executePostActions(changed, transition);

        assertThat(notifiedIncidentIds).containsExactly(42L);
    }
}
