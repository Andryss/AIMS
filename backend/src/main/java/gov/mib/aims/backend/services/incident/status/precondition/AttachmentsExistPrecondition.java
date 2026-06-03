package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Проверяет существование всех вложенных файлов.
 */
@Component
@RequiredArgsConstructor
public class AttachmentsExistPrecondition implements StatusTransitionPrecondition<IncidentEntity> {

    private final StoredFileRepository storedFileRepository;

    @Override
    public void check(IncidentEntity context) {
        for (Long fileId : context.getAttachmentFileIds()) {
            if (!storedFileRepository.existsById(fileId)) {
                throw Errors.attachmentNotFound();
            }
        }
    }
}
