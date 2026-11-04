package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistoryEntry;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistorySnapshot;
import gov.mib.aims.backend.generated.model.EquipmentMovementListResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.model.HistoryRecord;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Независимое представление заявки для API и аудита.
 */
@Component
@RequiredArgsConstructor
public class EquipmentMovementMapper {

    private final ChronoService chronoService;

    /**
     * Создает снимок заявки с реквизитами выбранных экземпляров.
     */
    public EquipmentMovementResponse toResponse(EquipmentMovementEntity entity, List<EquipmentResponse> equipment) {
        return new EquipmentMovementResponse().id(entity.getId())
                .operation(EquipmentMovementOperationApi.valueOf(entity.getOperation().name()))
                .status(EquipmentMovementStatusApi.valueOf(entity.getStatus().name()))
                .incidentId(entity.getIncidentId()).responsibleUserId(entity.getResponsibleUserId())
                .sourceIssueMovementId(entity.getSourceIssueMovementId()).createdByUserId(entity.getCreatedByUserId())
                .comment(entity.getComment()).createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()))
                .completedAt(entity.getCompletedAt() == null ? null
                        : chronoService.toOffsetDateTime(entity.getCompletedAt())).equipment(equipment);
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public EquipmentMovementListResponse toListResponse(
            Page<EquipmentMovementEntity> page, List<EquipmentMovementResponse> items) {
        return new EquipmentMovementListResponse().items(items)
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public EquipmentMovementHistoryListResponse toHistoryListResponse(
            Page<HistoryRecord<EquipmentMovementHistorySnapshot>> page) {
        return new EquipmentMovementHistoryListResponse().items(page.map(this::toHistoryEntry).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    private EquipmentMovementHistoryEntry toHistoryEntry(HistoryRecord<EquipmentMovementHistorySnapshot> entry) {
        return new EquipmentMovementHistoryEntry().id(entry.id())
                .changedAt(chronoService.toOffsetDateTime(entry.changedAt()))
                .changedByUserId(entry.changedByUserId()).snapshot(entry.snapshot());
    }
}
