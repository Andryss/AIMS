package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.IncidentEntity;
import gov.mib.aims.backend.generated.model.IncidentHistoryEntry;
import gov.mib.aims.backend.generated.model.IncidentHistoryListResponse;
import gov.mib.aims.backend.generated.model.IncidentHistorySnapshot;
import gov.mib.aims.backend.model.HistoryRecord;
import gov.mib.aims.backend.model.IncidentSnapshot;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

/**
 * Отделяет сохраняемый снимок инцидента от JPA и публичного API.
 */
@Component
@RequiredArgsConstructor
public class IncidentSnapshotMapper {

    private final IncidentMapper incidentMapper;
    private final ChronoService chronoService;

    /**
     * Копирует состояние в прежнюю плоскую структуру JSON истории.
     */
    public IncidentSnapshot toStorage(IncidentEntity entity) {
        return IncidentSnapshot.builder().id(entity.getId()).status(entity.getStatus()).eventType(entity.getEventType())
                .location(entity.getLocation()).detectedAt(entity.getDetectedAt()).description(entity.getDescription())
                .victimCount(entity.getVictimCount()).threatLevel(entity.getThreatLevel())
                .attachmentFileIds(entity.getAttachmentFileIds()).alienId(entity.getAlienId())
                .cleanupStatus(entity.getCleanupStatus()).cleanupReportId(entity.getCleanupReportId())
                .responsibleUserId(entity.getResponsibleUserId()).executorUserIds(entity.getExecutorUserIds())
                .createdByUserId(entity.getCreatedByUserId()).monitoringAlertId(entity.getMonitoringAlertId())
                .createdAt(entity.getCreatedAt()).updatedAt(entity.getUpdatedAt()).build();
    }

    /**
     * Преобразует старый или новый снимок хранения в публичный ответ.
     */
    public IncidentHistorySnapshot toResponse(IncidentSnapshot snapshot) {
        return new IncidentHistorySnapshot().status(incidentMapper.toApiStatus(snapshot.status()))
                .eventType(incidentMapper.toApiEventType(snapshot.eventType())).location(snapshot.location())
                .detectedAt(chronoService.toOffsetDateTime(snapshot.detectedAt())).description(snapshot.description())
                .victimCount(snapshot.victimCount()).threatLevel(snapshot.threatLevel())
                .attachmentFileIds(new ArrayList<>(snapshot.attachmentFileIds())).alienId(snapshot.alienId())
                .responsibleUserId(snapshot.responsibleUserId())
                .executorUserIds(new ArrayList<>(snapshot.executorUserIds()))
                .cleanupStatus(incidentMapper.toApiCleanupStatus(snapshot.cleanupStatus()))
                .cleanupReportId(snapshot.cleanupReportId());
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public IncidentHistoryListResponse toHistoryListResponse(Page<HistoryRecord<IncidentSnapshot>> page) {
        return new IncidentHistoryListResponse().items(page.map(this::toHistoryEntry).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    private IncidentHistoryEntry toHistoryEntry(HistoryRecord<IncidentSnapshot> entry) {
        return new IncidentHistoryEntry().id(entry.id())
                .changedAt(chronoService.toOffsetDateTime(entry.changedAt()))
                .changedByUserId(entry.changedByUserId()).snapshot(toResponse(entry.snapshot()));
    }
}
