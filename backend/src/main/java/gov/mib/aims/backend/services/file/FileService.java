package gov.mib.aims.backend.services.file;

import gov.mib.aims.backend.entity.StoredFileEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.BatchFilesRequest;
import gov.mib.aims.backend.generated.model.BatchFilesResponse;
import gov.mib.aims.backend.generated.model.FileUploadResponse;
import gov.mib.aims.backend.model.FileDescriptor;
import gov.mib.aims.backend.model.FileDownload;
import gov.mib.aims.backend.repository.StoredFileRepository;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.mapping.FileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Сервис загрузки и скачивания файлов.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileMapper fileMapper;

    private static final int BATCH_MAX = 100;

    private final FileStorage fileStorage;
    private final StoredFileRepository storedFileRepository;
    private final CurrentUserService currentUserService;

    /**
     * Сохраняет файл в хранилище и записывает метаданные в БД.
     *
     * @param file загружаемый файл
     * @return метаданные с числовым id
     */
    @Transactional
    public FileUploadResponse upload(MultipartFile file) {
        validateUpload(file);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        try (InputStream content = file.getInputStream()) {
            return store(fileName, contentType, content, file.getSize());
        } catch (IOException e) {
            throw Errors.fileUploadFailed();
        }
    }

    private FileUploadResponse store(String fileName, String contentType, InputStream content, long size) {
        Long userId = currentUserService.getCurrentUserId();
        FileDescriptor descriptor = fileStorage.store(fileName, contentType, content, size);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    try {
                        fileStorage.delete(descriptor.storageId());
                    } catch (RuntimeException e) {
                        log.error("Failed to remove rolled-back upload {}", descriptor.storageId(), e);
                    }
                }
            }
        });

        StoredFileEntity entity = StoredFileEntity.builder()
                .storageId(descriptor.storageId())
                .fileName(descriptor.originalFileName())
                .contentType(descriptor.contentType())
                .fileSize(descriptor.sizeBytes())
                .createdAt(descriptor.storedAt())
                .createdByUserId(userId)
                .build();
        entity = storedFileRepository.save(entity);
        return fileMapper.toResponse(entity);
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

    /**
     * Возвращает метаданные существующих файлов в порядке запрошенных id.
     *
     * @param request идентификаторы файлов
     * @return метаданные без чтения содержимого
     */
    @Transactional(readOnly = true)
    public BatchFilesResponse batch(BatchFilesRequest request) {
        List<Long> ids = request.getIds();
        if (ids == null || ids.size() > BATCH_MAX || ids.stream().anyMatch(id -> id == null || id <= 0)) {
            throw Errors.validationError("Укажите до 100 положительных идентификаторов файлов");
        }
        Map<Long, StoredFileEntity> byId = new HashMap<>();
        storedFileRepository.findAllById(ids).forEach(file -> byId.put(file.getId(), file));

        return new BatchFilesResponse().items(ids.stream().map(byId::get).filter(Objects::nonNull)
                .map(fileMapper::toResponse).toList());
    }

    private void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw Errors.fileEmpty();
        }
    }
}
