package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.EquipmentMovementItemEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * Состав заявок на движение.
 */
public interface EquipmentMovementItemRepository extends JpaRepository<EquipmentMovementItemEntity, Long> {

    /**
     * Загружает состав нескольких заявок одним запросом.
     */
    List<EquipmentMovementItemEntity> findAllByMovementIdInOrderByIdAsc(Collection<Long> movementIds);

    /**
     * Удаляет прежний состав редактируемой заявки.
     */
    @Modifying
    @Query("delete from EquipmentMovementItemEntity i where i.movementId = :id")
    void deleteByMovementId(@Param("id") Long id);
}
