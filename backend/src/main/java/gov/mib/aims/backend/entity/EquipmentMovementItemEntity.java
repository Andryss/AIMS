package gov.mib.aims.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Экземпляр, включенный в заявку на движение.
 */
@Entity
@Table(name = "equipment_movement_item")
@Getter
@Setter
public class EquipmentMovementItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "movement_id", nullable = false)
    private Long movementId;

    @Column(name = "equipment_item_id", nullable = false)
    private Long equipmentItemId;
}
