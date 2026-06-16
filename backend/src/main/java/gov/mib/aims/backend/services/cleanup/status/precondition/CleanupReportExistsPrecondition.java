package gov.mib.aims.backend.services.cleanup.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.services.incident.status.precondition.StatusTransitionPrecondition;
import org.springframework.stereotype.Component;

/**
 * Для завершения очистки отчёт должен существовать.
 */
@Component
public class CleanupReportExistsPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    @Override
    public void check(IncidentEntity context) {
        if (context.getCleanupReportId() == null) {
            throw Errors.validationError("Cleanup report must exist before completing cleanup");
        }
    }
}
