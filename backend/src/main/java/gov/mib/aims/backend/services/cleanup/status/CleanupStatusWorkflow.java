package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.incident.status.postaction.StatusTransitionPostAction;
import gov.mib.aims.backend.services.incident.status.precondition.StatusTransitionPrecondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Workflow смены статуса очистки.
 */
@Service
@RequiredArgsConstructor
public class CleanupStatusWorkflow {

    private final CleanupStatusTransitionGraph transitionGraph;
    private final IncidentRepository incidentRepository;
    private final Clock clock;

    /**
     * Меняет статус очистки инцидента.
     *
     * @param incident инцидент
     * @param target целевой статус
     * @return обновлённый инцидент
     */
    public IncidentEntity changeStatus(IncidentEntity incident, CleanupStatus target) {
        CleanupStatus current = incident.getCleanupStatus();
        if (!transitionGraph.isAllowed(current, target)) {
            throw Errors.invalidCleanupStatusTransition();
        }
        CleanupStatusTransition<IncidentEntity> transition = transitionGraph.getTransition(current, target);
        if (transition == null) {
            throw Errors.invalidCleanupStatusTransition();
        }
        for (StatusTransitionPrecondition<IncidentEntity> precondition : transition.getPreconditions()) {
            precondition.check(incident);
        }
        incident.setCleanupStatus(target);
        incident.setUpdatedAt(LocalDateTime.now(clock));
        incident = incidentRepository.save(incident);
        for (StatusTransitionPostAction<IncidentEntity> postAction : transition.getPostActions()) {
            postAction.execute(incident);
        }
        return incident;
    }
}
