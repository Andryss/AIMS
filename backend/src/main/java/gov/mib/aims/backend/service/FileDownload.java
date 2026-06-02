package gov.mib.aims.backend.service;

/**
 * Содержимое файла для отдачи клиенту.
 *
 * @param content байты файла
 * @param contentType MIME-тип
 * @param fileName имя файла для Content-Disposition
 */
public record FileDownload(byte[] content, String contentType, String fileName) {
}
