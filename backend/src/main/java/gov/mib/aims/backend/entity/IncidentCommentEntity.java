package gov.mib.aims.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;

/**
 * Комментарий к инциденту.
 */
@Entity
@Table(name = "incident_comment")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class IncidentCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "incident_id", nullable = false)
    private Long incidentId;

    @Column(name = "author_user_id", nullable = false)
    private Long authorUserId;

    @Column(nullable = false)
    private String text;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
