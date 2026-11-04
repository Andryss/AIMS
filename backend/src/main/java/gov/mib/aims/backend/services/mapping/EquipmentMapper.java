package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEntry;
import gov.mib.aims.backend.generated.model.EquipmentHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentHistorySnapshot;
import gov.mib.aims.backend.generated.model.EquipmentListResponse;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.EquipmentStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.model.HistoryRecord;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

/**
 * Представление экземпляра оборудования в API и истории.
 */
@Component
@RequiredArgsConstructor
public class EquipmentMapper {

    private final ChronoService chronoService;

    /**
     * Создает независимый от JPA снимок экземпляра.
     */
    public EquipmentResponse toResponse(EquipmentEntity entity) {
        return new EquipmentResponse().id(entity.getId())
                .type(EquipmentTypeApi.valueOf(entity.getType().name())).model(entity.getModel())
                .serialNumber(entity.getSerialNumber()).status(EquipmentStatusApi.valueOf(entity.getStatus().name()))
                .holderUserId(entity.getHolderUserId()).currentIssueMovementId(entity.getCurrentIssueMovementId())
                .sourceTenderId(entity.getSourceTenderId())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()));
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public EquipmentListResponse toListResponse(Page<EquipmentEntity> page) {
        return new EquipmentListResponse().items(page.map(this::toResponse).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public EquipmentHistoryListResponse toHistoryListResponse(Page<HistoryRecord<EquipmentHistorySnapshot>> page) {
        return new EquipmentHistoryListResponse().items(page.map(this::toHistoryEntry).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    private EquipmentHistoryEntry toHistoryEntry(HistoryRecord<EquipmentHistorySnapshot> entry) {
        return new EquipmentHistoryEntry().id(entry.id())
                .changedAt(chronoService.toOffsetDateTime(entry.changedAt()))
                .changedByUserId(entry.changedByUserId()).snapshot(entry.snapshot());
    }
}
