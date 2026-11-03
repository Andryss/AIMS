package gov.mib.aims.backend.services;

import gov.mib.aims.backend.config.StorageProperties;
import gov.mib.aims.backend.config.TestChronoConfig;
import gov.mib.aims.backend.model.FileDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        storage = new LocalDiskFileStorage(properties,
                new ChronoService(Clock.fixed(TestChronoConfig.FIXED_INSTANT, ZoneOffset.UTC)));
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

        assertThat(descriptor.storedAt()).isEqualTo(TestChronoConfig.FIXED_INSTANT);
        assertThat(descriptor.storageId()).isNotBlank();
        assertThat(Files.exists(tempDir.resolve(descriptor.storageId()))).isTrue();
        assertThat(storage.read(descriptor.storageId())).contains(content);
    }

    @Test
    void readReturnsEmptyWhenFileMissing() {
        assertThat(storage.read("non-existent-id")).isEmpty();
    }

    @Test
    void interruptedWriteDoesNotLeavePartialFile() throws Exception {
        InputStream broken = new InputStream() {
            private int position;

            @Override
            public int read() throws IOException {
                if (position++ == 0) {
                    return 1;
                }
                throw new IOException("Interrupted upload");
            }
        };

        assertThatThrownBy(() -> storage.store("photo.jpg", "image/jpeg", broken, 2))
                .isInstanceOf(IllegalStateException.class);
        try (var files = Files.list(tempDir)) {
            assertThat(files).isEmpty();
        }
    }
}
