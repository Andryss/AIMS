package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.StoredFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

/**
 * Репозиторий метаданных файлов.
 */
public interface StoredFileRepository extends JpaRepository<StoredFileEntity, Long> {
    /**
     * Возвращает количество существующих файлов из набора уникальных id.
     */
    long countByIdIn(Collection<Long> ids);
}
