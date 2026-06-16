package gov.mib.aims.backend.entity;

import gov.mib.aims.backend.model.CleanupStatus;
import gov.mib.aims.backend.model.IncidentEventType;
import gov.mib.aims.backend.model.IncidentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Инцидент.
 */
@Entity
@Table(name = "incident")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IncidentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IncidentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private IncidentEventType eventType;

    @Column(nullable = false)
    private String location;

    @Column(name = "detected_at", nullable = false)
    private LocalDateTime detectedAt;

    @Column(nullable = false)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attachment_file_ids", nullable = false)
    @Builder.Default
    private List<Long> attachmentFileIds = new ArrayList<>();

    @Column(name = "alien_id")
    private Long alienId;

    @Enumerated(EnumType.STRING)
    @Column(name = "cleanup_status")
    private CleanupStatus cleanupStatus;

    @Column(name = "cleanup_report_id")
    private Long cleanupReportId;

    @Column(name = "responsible_user_id")
    private Long responsibleUserId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "executor_user_ids", nullable = false)
    @Builder.Default
    private List<Long> executorUserIds = new ArrayList<>();

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
