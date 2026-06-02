package gov.mib.aims.backend.storage;

import java.time.Instant;

/**
 * Метаданные файла после сохранения в хранилище.
 *
 * @param storageId уникальный строковый идентификатор в хранилище
 * @param originalFileName исходное имя файла
 * @param contentType MIME-тип
 * @param sizeBytes размер в байтах
 * @param storedAt момент сохранения
 */
public record FileDescriptor(
        String storageId,
        String originalFileName,
        String contentType,
        long sizeBytes,
        Instant storedAt
) {
}
