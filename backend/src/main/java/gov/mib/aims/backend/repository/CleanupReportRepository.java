package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.CleanupReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий отчётов об очистке.
 */
public interface CleanupReportRepository extends JpaRepository<CleanupReportEntity, Long> {

    /**
     * Находит отчёт по id инцидента.
     *
     * @param incidentId идентификатор инцидента
     * @return отчёт, если есть
     */
    Optional<CleanupReportEntity> findByIncidentId(Long incidentId);

    /**
     * Проверяет наличие отчёта для инцидента.
     *
     * @param incidentId идентификатор инцидента
     * @return true, если отчёт существует
     */
    boolean existsByIncidentId(Long incidentId);
}
