package gov.mib.aims.backend.storage;

import java.io.InputStream;
import java.util.Optional;

/**
 * Абстракция хранилища файлов (локальный диск, S3 и т.д.).
 */
public interface FileStorage {

    /**
     * Сохраняет файл и возвращает дескриптор с уникальным storageId.
     *
     * @param originalFileName исходное имя файла
     * @param contentType MIME-тип
     * @param content поток содержимого
     * @param sizeBytes ожидаемый размер в байтах
     * @return дескриптор сохранённого файла
     */
    FileDescriptor store(String originalFileName, String contentType, InputStream content, long sizeBytes);

    /**
     * Читает содержимое файла по storageId.
     *
     * @param storageId идентификатор в хранилище
     * @return байты файла или пусто, если файл не найден
     */
    Optional<byte[]> read(String storageId);

    /**
     * Удаляет файл из хранилища.
     *
     * @param storageId идентификатор в хранилище
     */
    void delete(String storageId);
}
