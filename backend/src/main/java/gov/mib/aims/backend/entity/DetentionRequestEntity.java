package gov.mib.aims.backend.entity;

import gov.mib.aims.backend.model.DetentionRequestStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * Заявка с данными субъекта и сроком его заключения.
 */
@Entity
@Table(name = "detention_request")
@Getter
@Setter
public class DetentionRequestEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "subject_name", nullable = false)
    private String subjectName;

    @Column(name = "subject_description", nullable = false)
    private String subjectDescription;

    @Column(name = "alien_id")
    private Long alienId;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DetentionRequestStatus status;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Generated(event = EventType.INSERT)
    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "admitted_at")
    private Instant admittedAt;

    @Column(name = "released_at")
    private Instant releasedAt;

}
