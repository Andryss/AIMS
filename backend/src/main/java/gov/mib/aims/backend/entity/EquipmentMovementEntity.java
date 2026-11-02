package gov.mib.aims.backend.entity;

import gov.mib.aims.backend.model.EquipmentMovementOperation;
import gov.mib.aims.backend.model.EquipmentMovementStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.time.Instant;

/**
 * Заявка на передачу конкретных экземпляров оборудования.
 */
@Entity
@Table(name = "equipment_movement")
@Getter
@Setter
public class EquipmentMovementEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentMovementOperation operation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EquipmentMovementStatus status;

    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    @Column(name = "responsible_user_id", nullable = false)
    private Long responsibleUserId;

    @Column(name = "source_issue_movement_id", updatable = false)
    private Long sourceIssueMovementId;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;

    private String comment;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;
}
