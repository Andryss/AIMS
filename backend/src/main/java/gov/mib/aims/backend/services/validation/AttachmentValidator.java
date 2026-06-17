package gov.mib.aims.backend.services.validation;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.repository.StoredFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Проверяет существование файлов по id в {@code stored_file}.
 */
@Component
@RequiredArgsConstructor
public class AttachmentValidator {

    private final StoredFileRepository storedFileRepository;

    /**
     * Убеждается, что все указанные файлы существуют.
     *
     * @param attachmentFileIds идентификаторы вложений
     */
    public void assertAllExist(List<Long> attachmentFileIds) {
        if (attachmentFileIds == null || attachmentFileIds.isEmpty()) {
            return;
        }
        for (Long fileId : attachmentFileIds) {
            if (!storedFileRepository.existsById(fileId)) {
                throw Errors.attachmentNotFound();
            }
        }
    }
}
