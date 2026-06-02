package gov.mib.aims.backend.services;

import gov.mib.aims.backend.config.StorageProperties;
import gov.mib.aims.backend.model.FileDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Тесты {@link LocalDiskFileStorage}.
 */
class LocalDiskFileStorageTest {

    @TempDir
    Path tempDir;

    private LocalDiskFileStorage storage;

    @BeforeEach
    void setUp() {
        StorageProperties properties = new StorageProperties();
        properties.setBasePath(tempDir.toString());
        storage = new LocalDiskFileStorage(properties);
    }

    @Test
    void storeAndReadRoundtrip() {
        byte[] content = "MIB file content".getBytes();
        FileDescriptor descriptor = storage.store(
                "report.jpg",
                "image/jpeg",
                new ByteArrayInputStream(content),
                content.length
        );

        assertThat(descriptor.storageId()).isNotBlank();
        assertThat(Files.exists(tempDir.resolve(descriptor.storageId()))).isTrue();
        assertThat(storage.read(descriptor.storageId())).contains(content);
    }

    @Test
    void readReturnsEmptyWhenFileMissing() {
        assertThat(storage.read("non-existent-id")).isEmpty();
    }
}
