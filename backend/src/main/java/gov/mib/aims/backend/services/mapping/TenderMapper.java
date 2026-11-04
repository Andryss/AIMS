package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.TenderHistoryEntry;
import gov.mib.aims.backend.generated.model.TenderHistoryListResponse;
import gov.mib.aims.backend.generated.model.TenderHistorySnapshot;
import gov.mib.aims.backend.generated.model.TenderListResponse;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.generated.model.TenderStatusApi;
import gov.mib.aims.backend.model.HistoryRecord;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Представление тендера в API и независимых снимках истории.
 */
@Component
@RequiredArgsConstructor
public class TenderMapper {

    private final ChronoService chronoService;

    /**
     * Создаёт представление тендера с выбранным победителем.
     */
    public TenderResponse toResponse(TenderEntity entity, Long winnerParticipantId) {
        return new TenderResponse().id(entity.getId()).winnerParticipantId(winnerParticipantId)
                .equipmentType(EquipmentTypeApi.valueOf(entity.getEquipmentType().name()))
                .quantity(entity.getQuantity()).conditions(entity.getConditions()).incidentId(entity.getIncidentId())
                .status(TenderStatusApi.valueOf(entity.getStatus().name())).createdByUserId(entity.getCreatedByUserId())
                .createdAt(chronoService.toOffsetDateTime(entity.getCreatedAt()))
                .updatedAt(chronoService.toOffsetDateTime(entity.getUpdatedAt()))
                .completedAt(chronoService.toOffsetDateTime(entity.getCompletedAt()));
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public TenderListResponse toListResponse(Page<TenderEntity> page, Map<Long, Long> winners) {
        var items = page.map(entity -> toResponse(entity, winners.get(entity.getId()))).getContent();

        return new TenderListResponse().items(items)
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public TenderHistoryListResponse toHistoryListResponse(Page<HistoryRecord<TenderHistorySnapshot>> page) {
        return new TenderHistoryListResponse().items(page.map(this::toHistoryEntry).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }

    private TenderHistoryEntry toHistoryEntry(HistoryRecord<TenderHistorySnapshot> entry) {
        return new TenderHistoryEntry().id(entry.id())
                .changedAt(chronoService.toOffsetDateTime(entry.changedAt()))
                .changedByUserId(entry.changedByUserId()).snapshot(entry.snapshot());
    }
}
