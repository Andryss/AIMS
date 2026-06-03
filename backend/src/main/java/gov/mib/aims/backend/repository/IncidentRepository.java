package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.IncidentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий инцидентов.
 */
public interface IncidentRepository extends JpaRepository<IncidentEntity, Long> {
}
