package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import gov.mib.aims.backend.generated.model.DetentionRequestResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * Представление заявки на заключение и её дат в UTC.
 */
@Component
public class DetentionRequestMapper {

    /**
     * Создаёт представление заявки для API и снимка истории.
     */
    public DetentionRequestResponse toResponse(DetentionRequestEntity entity) {
        return new DetentionRequestResponse().id(entity.getId()).incidentId(entity.getIncidentId())
                .subjectName(entity.getSubjectName()).subjectDescription(entity.getSubjectDescription())
                .alienId(entity.getAlienId()).durationDays(entity.getDurationDays())
                .status(DetentionRequestStatusApi.valueOf(entity.getStatus().name()))
                .createdByUserId(entity.getCreatedByUserId()).reviewedByUserId(entity.getReviewedByUserId())
                .createdAt(toDateTime(entity.getCreatedAt())).updatedAt(toDateTime(entity.getUpdatedAt()))
                .admittedAt(toDateTime(entity.getAdmittedAt())).releasedAt(toDateTime(entity.getReleasedAt()))
                .plannedReleaseAt(toDateTime(entity.getPlannedReleaseAt()));
    }

    private OffsetDateTime toDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
