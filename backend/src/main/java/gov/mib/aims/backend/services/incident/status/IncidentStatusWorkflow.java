package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.incident.status.postaction.StatusTransitionPostAction;
import gov.mib.aims.backend.services.incident.status.precondition.StatusTransitionPrecondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Workflow смены статуса инцидента.
 */
@Service
@RequiredArgsConstructor
public class IncidentStatusWorkflow {

    private final IncidentStatusTransitionGraph transitionGraph;
    private final IncidentRepository incidentRepository;
    private final Clock clock;

    /**
     * Меняет статус инцидента с проверкой pre/post действий.
     *
     * @param incident инцидент
     * @param target целевой статус
     * @return обновлённый инцидент
     */
    public IncidentEntity changeStatus(IncidentEntity incident, IncidentStatus target) {
        IncidentStatus current = incident.getStatus();
        if (!transitionGraph.isAllowed(current, target)) {
            throw Errors.invalidStatusTransition();
        }
        IncidentStatusTransition<IncidentEntity> transition = transitionGraph.getTransition(current, target);
        if (transition == null) {
            throw Errors.invalidStatusTransition();
        }
        for (StatusTransitionPrecondition<IncidentEntity> precondition : transition.getPreconditions()) {
            precondition.check(incident);
        }
        incident.setStatus(target);
        incident.setUpdatedAt(LocalDateTime.now(clock));
        incident = incidentRepository.save(incident);
        for (StatusTransitionPostAction<IncidentEntity> postAction : transition.getPostActions()) {
            postAction.execute(incident);
        }
        return incident;
    }
}
