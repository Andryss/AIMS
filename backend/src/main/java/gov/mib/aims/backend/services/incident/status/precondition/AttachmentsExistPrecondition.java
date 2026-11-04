package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Проверяет существование всех вложенных файлов.
 */
@Component
@RequiredArgsConstructor
public class AttachmentsExistPrecondition implements StatusTransitionPrecondition<IncidentTransitionContext> {

    private final AttachmentValidator attachmentValidator;

    @Override
    public void check(IncidentTransitionContext context) {
        attachmentValidator.assertAllExist(context.incident().getAttachmentFileIds());
    }
}
