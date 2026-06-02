package gov.mib.aims.backend.service;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.entity.StoredFileEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.FileUploadResponse;
import gov.mib.aims.backend.repository.AppUserRepository;
import gov.mib.aims.backend.repository.StoredFileRepository;
import gov.mib.aims.backend.storage.FileDescriptor;
import gov.mib.aims.backend.storage.FileStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Сервис загрузки и скачивания файлов.
 */
@Service
@RequiredArgsConstructor
public class FileService {

    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final AppUserRepository appUserRepository;

    /**
     * Сохраняет файл в хранилище и записывает метаданные в БД.
     *
     * @param file загружаемый файл
     * @return метаданные с числовым id
     */
    @Transactional
    public FileUploadResponse upload(MultipartFile file) {
        validateUpload(file);
        Long userId = resolveCurrentUserId();
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        long size = file.getSize();
        FileDescriptor descriptor;
        try {
            descriptor = fileStorage.store(fileName, contentType, file.getInputStream(), size);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read upload stream", e);
        }
        StoredFileEntity entity = StoredFileEntity.builder()
                .storageId(descriptor.storageId())
                .fileName(descriptor.originalFileName())
                .contentType(descriptor.contentType())
                .fileSize(descriptor.sizeBytes())
                .createdAt(descriptor.storedAt())
                .createdByUserId(userId)
                .build();
        entity = storedFileRepository.save(entity);
        return new FileUploadResponse()
                .id(entity.getId())
                .fileName(entity.getFileName())
                .contentType(entity.getContentType())
                .fileSize(entity.getFileSize())
                .createdAt(OffsetDateTime.ofInstant(entity.getCreatedAt(), ZoneOffset.UTC));
    }

    /**
     * Возвращает содержимое файла по числовому id.
     *
     * @param id идентификатор записи в stored_file
     * @return байты и метаданные для HTTP-ответа
     */
    @Transactional(readOnly = true)
    public FileDownload download(Long id) {
        StoredFileEntity entity = storedFileRepository.findById(id)
                .orElseThrow(Errors::fileNotFound);
        byte[] content = fileStorage.read(entity.getStorageId())
                .orElseThrow(Errors::fileNotFound);
        return new FileDownload(content, entity.getContentType(), entity.getFileName());
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw Errors.fileEmpty();
        }
    }

    private Long resolveCurrentUserId() {
        String login = getCurrentLogin();
        AppUserEntity user = appUserRepository.findByLogin(login)
                .orElseThrow(() -> Errors.userNotFound(login));
        return user.getId();
    }

    private String getCurrentLogin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw Errors.unauthorized();
        }
        return authentication.getName();
    }
}
