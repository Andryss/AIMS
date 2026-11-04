package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.StoredFileEntity;
import gov.mib.aims.backend.generated.model.FileUploadResponse;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Преобразование данных в представления API.
 */
@Component
@RequiredArgsConstructor
public class FileMapper {

    private final ChronoService chronoService;

    /**
     * Создаёт представление, независимое от исходной сущности.
     */
    public FileUploadResponse toResponse(StoredFileEntity entity) {
        return new FileUploadResponse()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()));
    }
}
