package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.status.AbstractStatusWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

/**
 * Workflow смены статуса очистки.
 */
@Service
@RequiredArgsConstructor
public class CleanupStatusWorkflow
        extends AbstractStatusWorkflow<IncidentEntity, CleanupStatus> {

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
        var transition = transitionGraph.getTransition(incident.getCleanupStatus(), target);
        if (transition == null) {
            throw Errors.invalidCleanupStatusTransition();
        }

        return applyTransition(incident, transition);
    }

    @Override
    protected IncidentEntity applyStatus(IncidentEntity incident, CleanupStatus target) {
        incident.setCleanupStatus(target);
        incident.setUpdatedAt(LocalDateTime.now(clock));

        return incidentRepository.save(incident);
    }
}
