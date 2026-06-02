package gov.mib.aims.backend.storage;

import gov.mib.aims.backend.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Хранение файлов на локальном диске.
 */
@Component
@RequiredArgsConstructor
public class LocalDiskFileStorage implements FileStorage {

    private final StorageProperties storageProperties;

    @Override
    public FileDescriptor store(String originalFileName, String contentType, InputStream content, long sizeBytes) {
        String storageId = UUID.randomUUID().toString();
        Path targetPath = resolvePath(storageId);
        try {
            Files.createDirectories(targetPath.getParent());
            Files.copy(content, targetPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to store file: " + storageId, e);
        }
        return new FileDescriptor(
                storageId,
                originalFileName,
                contentType,
                sizeBytes,
                Instant.now()
        );
    }

    @Override
    public Optional<byte[]> read(String storageId) {
        Path path = resolvePath(storageId);
        if (!Files.isRegularFile(path)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Files.readAllBytes(path));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read file: " + storageId, e);
        }
    }

    @Override
    public void delete(String storageId) {
        Path path = resolvePath(storageId);
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete file: " + storageId, e);
        }
    }

    private Path resolvePath(String storageId) {
        return Path.of(storageProperties.getBasePath()).resolve(storageId);
    }
}
