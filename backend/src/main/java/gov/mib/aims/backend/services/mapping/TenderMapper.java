package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.generated.model.TenderStatusApi;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * Представление тендера в API и независимых снимках истории.
 */
@Component
public class TenderMapper {

    /**
     * Создаёт представление тендера с выбранным победителем.
     */
    public TenderResponse toResponse(TenderEntity entity, Long winnerParticipantId) {
        return new TenderResponse().id(entity.getId()).winnerParticipantId(winnerParticipantId)
                .equipmentType(EquipmentTypeApi.valueOf(entity.getEquipmentType().name()))
                .quantity(entity.getQuantity()).conditions(entity.getConditions()).incidentId(entity.getIncidentId())
                .status(TenderStatusApi.valueOf(entity.getStatus().name())).createdByUserId(entity.getCreatedByUserId())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .updatedAt(entity.getUpdatedAt().atOffset(ZoneOffset.UTC))
                .completedAt(entity.getCompletedAt() == null ? null : entity.getCompletedAt().atOffset(ZoneOffset.UTC));
    }
}
