package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.services.cleanup.status.postaction.EnqueueNotifyCleanupCompletedPostAction;
import gov.mib.aims.backend.services.cleanup.status.precondition.CleanupReportExistsPrecondition;
import gov.mib.aims.backend.services.cleanup.status.precondition.IncidentExecutingPrecondition;
import gov.mib.aims.backend.services.status.StatusTransition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Граф допустимых переходов статуса очистки.
 */
@Component
public class CleanupStatusTransitionGraph {

    private final Map<CleanupStatusTransitionKey, StatusTransition<IncidentEntity, CleanupStatus>> transitions;

    /**
     * Регистрирует линейный граф статусов очистки.
     */
    public CleanupStatusTransitionGraph(
            IncidentExecutingPrecondition incidentExecutingPrecondition,
            CleanupReportExistsPrecondition cleanupReportExistsPrecondition,
            EnqueueNotifyCleanupCompletedPostAction enqueueNotifyCleanupCompletedPostAction
    ) {
        List<StatusTransition<IncidentEntity, CleanupStatus>> transitionList = List.of(
                StatusTransition.<IncidentEntity, CleanupStatus>builder()
                        .from(null)
                        .to(CleanupStatus.PREPARATION)
                        .preconditions(List.of(incidentExecutingPrecondition))
                        .postActions(List.of())
                        .build(),
                StatusTransition.<IncidentEntity, CleanupStatus>builder()
                        .from(CleanupStatus.PREPARATION)
                        .to(CleanupStatus.EXECUTION)
                        .preconditions(List.of(incidentExecutingPrecondition))
                        .postActions(List.of())
                        .build(),
                StatusTransition.<IncidentEntity, CleanupStatus>builder()
                        .from(CleanupStatus.EXECUTION)
                        .to(CleanupStatus.COMPLETED)
                        .preconditions(List.of(incidentExecutingPrecondition, cleanupReportExistsPrecondition))
                        .postActions(List.of(enqueueNotifyCleanupCompletedPostAction))
                        .build()
        );
        this.transitions = transitionList.stream()
                .collect(Collectors.toMap(
                        t -> CleanupStatusTransitionKey.of(t.getFrom(), t.getTo()),
                        Function.identity()
                ));
    }

    /**
     * Проверяет, допустим ли переход.
     *
     * @param from исходный статус (null для первого)
     * @param to целевой статус
     * @return true, если переход зарегистрирован
     */
    public boolean isAllowed(CleanupStatus from, CleanupStatus to) {
        return transitions.containsKey(CleanupStatusTransitionKey.of(from, to));
    }

    /**
     * Возвращает конфигурацию перехода.
     *
     * @param from исходный статус
     * @param to целевой статус
     * @return переход или null
     */
    public StatusTransition<IncidentEntity, CleanupStatus> getTransition(CleanupStatus from, CleanupStatus to) {
        return transitions.get(CleanupStatusTransitionKey.of(from, to));
    }
}
