package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.generated.model.TenderStatusApi;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
}
