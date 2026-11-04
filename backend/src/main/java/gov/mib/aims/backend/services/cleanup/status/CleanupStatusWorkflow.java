package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.status.AbstractGraphStatusWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Workflow смены статуса очистки в транзакции вызывающего сервиса.
 */
@Service
@RequiredArgsConstructor
public class CleanupStatusWorkflow
        extends AbstractGraphStatusWorkflow<IncidentEntity, CleanupStatus> {

    private final CleanupStatusTransitionGraph transitionGraph;
    private final ChronoService chronoService;

    /**
     * Проверяет переход и изменяет статус очистки без сохранения и постановки задач в очередь.
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

    /**
     * Ставит задачи после сохранения инцидента и истории в той же транзакции сервиса.
     *
     * @param incident сохранённый инцидент
     * @param previousStatus статус очистки до перехода
     */
    public void afterSave(IncidentEntity incident, CleanupStatus previousStatus) {
        var transition = transitionGraph.getTransition(previousStatus, incident.getCleanupStatus());
        if (transition == null) {
            throw Errors.invalidCleanupStatusTransition();
        }
        executePostActions(incident, transition);
    }

    @Override
    protected IncidentEntity applyStatus(IncidentEntity incident, CleanupStatus target) {
        incident.setCleanupStatus(target);
        incident.setUpdatedAt(chronoService.localDateTimeNow());

        return incident;
    }
}
