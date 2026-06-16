package gov.mib.aims.backend.services.cleanup.status;

import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.services.incident.status.postaction.StatusTransitionPostAction;
import gov.mib.aims.backend.services.incident.status.precondition.StatusTransitionPrecondition;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Описание перехода статуса очистки.
 *
 * @param <T> тип контекста
 */
@Value
@Builder
public class CleanupStatusTransition<T> {

    CleanupStatus from;
    CleanupStatus to;
    List<StatusTransitionPrecondition<T>> preconditions;
    List<StatusTransitionPostAction<T>> postActions;
}
