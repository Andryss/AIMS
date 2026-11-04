package gov.mib.aims.backend.model;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Стабильный формат хранения истории инцидента, совместимый с прежним JSON сущности.
 */
@Builder
public record IncidentSnapshot(Long id, IncidentStatus status, IncidentEventType eventType, String location,
        LocalDateTime detectedAt, String description, Integer victimCount, Integer threatLevel,
        List<Long> attachmentFileIds, Long alienId, CleanupStatus cleanupStatus, Long cleanupReportId,
        Long responsibleUserId, List<Long> executorUserIds, Long createdByUserId, Long monitoringAlertId,
        LocalDateTime createdAt, LocalDateTime updatedAt) {

    public IncidentSnapshot {
        attachmentFileIds = attachmentFileIds == null ? List.of() : List.copyOf(attachmentFileIds);
        executorUserIds = executorUserIds == null ? List.of() : List.copyOf(executorUserIds);
    }
}
