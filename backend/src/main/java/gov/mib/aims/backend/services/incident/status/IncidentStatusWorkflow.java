package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.status.AbstractStatusWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Workflow смены статуса инцидента.
 */
@Service
@RequiredArgsConstructor
public class IncidentStatusWorkflow
        extends AbstractStatusWorkflow<IncidentEntity, IncidentStatus> {

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
        var transition = transitionGraph.getTransition(incident.getStatus(), target);
        if (transition == null) {
            throw Errors.invalidStatusTransition();
        }

        return applyTransition(incident, transition);
    }

    @Override
    protected IncidentEntity applyStatus(IncidentEntity incident, IncidentStatus target) {
        incident.setStatus(target);
        incident.setUpdatedAt(LocalDateTime.now(clock));

        return incidentRepository.save(incident);
    }
}
