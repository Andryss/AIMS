package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.TenderParticipantEntity;
import gov.mib.aims.backend.model.TenderParticipantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Предложения участников с привязкой к тендеру.
 */
public interface TenderParticipantRepository extends JpaRepository<TenderParticipantEntity, Long> {

    /**
     * Возвращает страницу предложений в порядке создания.
     */
    Page<TenderParticipantEntity> findByTenderIdOrderByCreatedAtAscIdAsc(Long tenderId, Pageable pageable);

    /**
     * Возвращает все предложения для независимого снимка истории.
     */
    List<TenderParticipantEntity> findByTenderIdOrderByCreatedAtAscIdAsc(Long tenderId);

    /**
     * Находит участника только в пределах указанного тендера.
     */
    Optional<TenderParticipantEntity> findByIdAndTenderId(Long id, Long tenderId);

    /**
     * Проверяет, зафиксированы ли тип и количество закупки предложениями.
     */
    boolean existsByTenderId(Long tenderId);

    /**
     * Проверяет наличие выбранного победителя перед приёмкой поставки.
     */
    boolean existsByTenderIdAndStatus(Long tenderId, TenderParticipantStatus status);

    /**
     * Загружает победителей страницы тендеров одним запросом.
     */
    List<TenderParticipantEntity> findByTenderIdInAndStatus(List<Long> tenderIds, TenderParticipantStatus status);
}
