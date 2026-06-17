package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyAgentsPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyAnalystsPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyAnalystsReanalysisPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyIncidentPreparedPostAction;
import gov.mib.aims.backend.services.incident.status.postaction.EnqueueNotifyOperatorClarificationPostAction;
import gov.mib.aims.backend.services.incident.status.precondition.AlienLinkedPrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.AssignmentCompletePrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.AttachmentsExistPrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.RolePrecondition;
import gov.mib.aims.backend.services.incident.status.precondition.ResponsibleAgentPrecondition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Граф допустимых переходов статусов инцидента и их конфигурация.
 */
@Component
public class IncidentStatusTransitionGraph {

    private final Map<StatusTransitionKey, IncidentStatusTransition<IncidentEntity>> transitions;

    /**
     * Регистрирует переходы UC1/UC2 и строит индекс по паре from/to.
     */
    public IncidentStatusTransitionGraph(
            AttachmentsExistPrecondition attachmentsExistPrecondition,
            RolePrecondition operatorRolePrecondition,
            RolePrecondition analystRolePrecondition,
            RolePrecondition agentRolePrecondition,
            AlienLinkedPrecondition alienLinkedPrecondition,
            AssignmentCompletePrecondition assignmentCompletePrecondition,
            EnqueueNotifyAnalystsPostAction enqueueNotifyAnalystsPostAction,
            EnqueueNotifyAgentsPostAction enqueueNotifyAgentsPostAction,
            EnqueueNotifyOperatorClarificationPostAction enqueueNotifyOperatorClarificationPostAction,
            EnqueueNotifyAnalystsReanalysisPostAction enqueueNotifyAnalystsReanalysisPostAction,
            EnqueueNotifyIncidentPreparedPostAction enqueueNotifyIncidentPreparedPostAction,
            ResponsibleAgentPrecondition responsibleAgentPrecondition
    ) {
        List<IncidentStatusTransition<IncidentEntity>> transitionList = List.of(
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.DRAFT)
                        .to(IncidentStatus.READY_FOR_ANALYSIS)
                        .preconditions(List.of(attachmentsExistPrecondition, operatorRolePrecondition))
                        .postActions(List.of(enqueueNotifyAnalystsPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.READY_FOR_ANALYSIS)
                        .to(IncidentStatus.READY_FOR_EXECUTION)
                        .preconditions(List.of(analystRolePrecondition, alienLinkedPrecondition))
                        .postActions(List.of(enqueueNotifyAgentsPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.READY_FOR_ANALYSIS)
                        .to(IncidentStatus.CLARIFICATION_REQUIRED)
                        .preconditions(List.of(analystRolePrecondition))
                        .postActions(List.of(enqueueNotifyOperatorClarificationPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.CLARIFICATION_REQUIRED)
                        .to(IncidentStatus.READY_FOR_ANALYSIS)
                        .preconditions(List.of(attachmentsExistPrecondition, operatorRolePrecondition))
                        .postActions(List.of(enqueueNotifyAnalystsPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.READY_FOR_EXECUTION)
                        .to(IncidentStatus.PREPARATION_FOR_EXECUTION)
                        .preconditions(List.of(agentRolePrecondition))
                        .postActions(List.of())
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.PREPARATION_FOR_EXECUTION)
                        .to(IncidentStatus.PREPARED_FOR_EXECUTION)
                        .preconditions(List.of(agentRolePrecondition, assignmentCompletePrecondition))
                        .postActions(List.of(enqueueNotifyIncidentPreparedPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.READY_FOR_EXECUTION)
                        .to(IncidentStatus.CLARIFICATION_REQUIRED)
                        .preconditions(List.of(agentRolePrecondition))
                        .postActions(List.of(enqueueNotifyOperatorClarificationPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.READY_FOR_EXECUTION)
                        .to(IncidentStatus.REANALYSIS_REQUIRED)
                        .preconditions(List.of(agentRolePrecondition))
                        .postActions(List.of(enqueueNotifyAnalystsReanalysisPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.PREPARATION_FOR_EXECUTION)
                        .to(IncidentStatus.CLARIFICATION_REQUIRED)
                        .preconditions(List.of(agentRolePrecondition))
                        .postActions(List.of(enqueueNotifyOperatorClarificationPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.PREPARATION_FOR_EXECUTION)
                        .to(IncidentStatus.REANALYSIS_REQUIRED)
                        .preconditions(List.of(agentRolePrecondition))
                        .postActions(List.of(enqueueNotifyAnalystsReanalysisPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.REANALYSIS_REQUIRED)
                        .to(IncidentStatus.READY_FOR_ANALYSIS)
                        .preconditions(List.of(analystRolePrecondition))
                        .postActions(List.of(enqueueNotifyAnalystsPostAction))
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.PREPARED_FOR_EXECUTION)
                        .to(IncidentStatus.EXECUTING)
                        .preconditions(List.of(responsibleAgentPrecondition))
                        .postActions(List.of())
                        .build(),
                IncidentStatusTransition.<IncidentEntity>builder()
                        .from(IncidentStatus.EXECUTING)
                        .to(IncidentStatus.EXECUTION_COMPLETED)
                        .preconditions(List.of(responsibleAgentPrecondition))
                        .postActions(List.of())
                        .build()
        );
        this.transitions = transitionList.stream()
                .collect(Collectors.toMap(
                        t -> StatusTransitionKey.of(t.getFrom(), t.getTo()),
                        Function.identity(),
                        (a, b) -> a
                ));
    }

    /**
     * Проверяет, допустим ли переход.
     *
     * @param from исходный статус
     * @param to целевой статус
     * @return true, если переход зарегистрирован
     */
    public boolean isAllowed(IncidentStatus from, IncidentStatus to) {
        return transitions.containsKey(StatusTransitionKey.of(from, to));
    }

    /**
     * Возвращает конфигурацию перехода.
     *
     * @param from исходный статус
     * @param to целевой статус
     * @return переход или null, если не зарегистрирован
     */
    public IncidentStatusTransition<IncidentEntity> getTransition(IncidentStatus from, IncidentStatus to) {
        return transitions.get(StatusTransitionKey.of(from, to));
    }
}
