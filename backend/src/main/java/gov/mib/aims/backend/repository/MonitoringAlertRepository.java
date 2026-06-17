package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.MonitoringAlertEntity;
import gov.mib.aims.backend.model.MonitoringAlertStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий алертов внешнего мониторинга.
 */
public interface MonitoringAlertRepository extends JpaRepository<MonitoringAlertEntity, Long> {

    /**
     * Проверяет наличие алерта с указанным внешним идентификатором события.
     */
    boolean existsByExternalEventId(String externalEventId);

    /**
     * Возвращает алерт по внешнему идентификатору события.
     */
    Optional<MonitoringAlertEntity> findByExternalEventId(String externalEventId);

    /**
     * Возвращает страницу алертов с указанным статусом.
     */
    Page<MonitoringAlertEntity> findByStatusOrderByReceivedAtDesc(
            MonitoringAlertStatus status,
            Pageable pageable
    );

    /**
     * Возвращает страницу всех алертов, отсортированных по времени получения.
     */
    Page<MonitoringAlertEntity> findAllByOrderByReceivedAtDesc(Pageable pageable);
}
