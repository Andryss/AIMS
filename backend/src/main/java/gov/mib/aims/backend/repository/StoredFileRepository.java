package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.StoredFileEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий метаданных файлов.
 */
public interface StoredFileRepository extends JpaRepository<StoredFileEntity, Long> {
}
