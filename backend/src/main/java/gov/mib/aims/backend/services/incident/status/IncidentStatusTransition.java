package gov.mib.aims.backend.services.incident.status;

import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.services.incident.status.postaction.StatusTransitionPostAction;
import gov.mib.aims.backend.services.incident.status.precondition.StatusTransitionPrecondition;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Описание перехода статуса инцидента.
 *
 * @param <T> тип контекста
 */
@Value
@Builder
public class IncidentStatusTransition<T> {

    IncidentStatus from;
    IncidentStatus to;
    List<StatusTransitionPrecondition<T>> preconditions;
    List<StatusTransitionPostAction<T>> postActions;
}
