package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import gov.mib.aims.backend.generated.model.DetentionRequestHistoryEntry;
import gov.mib.aims.backend.generated.model.DetentionRequestHistoryListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestHistorySnapshot;
import gov.mib.aims.backend.generated.model.DetentionRequestListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.model.HistoryRecord;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;

/**
 * Представление заявки на заключение и её дат в UTC.
 */
@Component
@RequiredArgsConstructor
public class DetentionRequestMapper {

    private final ChronoService chronoService;

    /**
     * Создаёт представление заявки для API и снимка истории.
     */
    public DetentionRequestResponse toResponse(DetentionRequestEntity entity) {
        var plannedReleaseAt = chronoService.plus(entity.getAdmittedAt(), entity.getDurationDays(), ChronoUnit.DAYS);

        return new DetentionRequestResponse().id(entity.getId()).incidentId(entity.getIncidentId())
                .subjectName(entity.getSubjectName()).subjectDescription(entity.getSubjectDescription())
                .alienId(entity.getAlienId()).durationDays(entity.getDurationDays())
                .status(DetentionRequestStatusApi.valueOf(entity.getStatus().name()))
                .createdByUserId(entity.getCreatedByUserId()).reviewedByUserId(entity.getReviewedByUserId())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()))
                .updatedAt(chronoService.toOffsetDateTime(entity.getUpdatedAt()))
                .admittedAt(chronoService.toOffsetDateTime(entity.getAdmittedAt()))
                .releasedAt(chronoService.toOffsetDateTime(entity.getReleasedAt()))
                .plannedReleaseAt(chronoService.toOffsetDateTime(plannedReleaseAt));
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public DetentionRequestListResponse toListResponse(Page<DetentionRequestEntity> page) {
        return new DetentionRequestListResponse().items(page.map(this::toResponse).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public DetentionRequestHistoryListResponse toHistoryListResponse(
            Page<HistoryRecord<DetentionRequestHistorySnapshot>> page) {
        return new DetentionRequestHistoryListResponse().items(page.map(this::toHistoryEntry).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    private DetentionRequestHistoryEntry toHistoryEntry(HistoryRecord<DetentionRequestHistorySnapshot> entry) {
        return new DetentionRequestHistoryEntry().id(entry.id())
                .changedAt(chronoService.toOffsetDateTime(entry.changedAt()))
                .changedByUserId(entry.changedByUserId()).snapshot(entry.snapshot());
    }
}
