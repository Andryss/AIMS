package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.AbstractGraphStatusWorkflow;
import gov.mib.aims.backend.services.status.StatusTransition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Workflow смены статуса инцидента в транзакции вызывающего сервиса.
 */
@Service
@RequiredArgsConstructor
public class IncidentStatusWorkflow
        extends AbstractGraphStatusWorkflow<IncidentTransitionContext, IncidentStatus> {

    private final IncidentStatusTransitionGraph transitionGraph;
    private final ChronoService chronoService;

    /**
     * Проверяет переход и изменяет инцидент без сохранения и постановки задач в очередь.
     *
     * @param incident инцидент
     * @param target целевой статус
     * @param comment комментарий перехода или null
     * @return обновлённый инцидент
     */
    public IncidentEntity changeStatus(IncidentEntity incident, IncidentStatus target, String comment) {
        var transition = requireTransition(incident.getStatus(), target);
        return applyTransition(new IncidentTransitionContext(incident, comment), transition).incident();
    }

    /**
     * Ставит задачи после сохранения инцидента и истории в той же транзакции сервиса.
     *
     * @param incident сохранённый инцидент
     * @param previousStatus статус до перехода
     * @param comment комментарий перехода или null
     */
    public void afterSave(IncidentEntity incident, IncidentStatus previousStatus, String comment) {
        var transition = requireTransition(previousStatus, incident.getStatus());
        executePostActions(new IncidentTransitionContext(incident, comment), transition);
    }

    private StatusTransition<IncidentTransitionContext, IncidentStatus> requireTransition(IncidentStatus from,
                                                                                         IncidentStatus target) {
        var transition = transitionGraph.getTransition(from, target);
        if (transition == null) {
            throw Errors.invalidStatusTransition();
        }
        return transition;
    }

    @Override
    protected IncidentTransitionContext applyStatus(IncidentTransitionContext context, IncidentStatus target) {
        context.incident().setStatus(target);
        context.incident().setUpdatedAt(chronoService.localDateTimeNow());
        return context;
    }
}
