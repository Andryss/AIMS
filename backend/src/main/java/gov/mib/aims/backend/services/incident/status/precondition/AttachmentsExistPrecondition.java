package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.services.validation.AttachmentValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Проверяет существование всех вложенных файлов.
 */
@Component
@RequiredArgsConstructor
public class AttachmentsExistPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    private final AttachmentValidator attachmentValidator;

    @Override
    public void check(IncidentEntity context) {
        attachmentValidator.assertAllExist(context.getAttachmentFileIds());
    }
}
