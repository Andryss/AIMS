package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.EquipmentStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

/**
 * Представление экземпляра оборудования в API и истории.
 */
@Component
public class EquipmentMapper {

    /**
     * Создает независимый от JPA снимок экземпляра.
     */
    public EquipmentResponse toResponse(EquipmentEntity entity) {
        return new EquipmentResponse().id(entity.getId())
                .type(EquipmentTypeApi.valueOf(entity.getType().name())).model(entity.getModel())
                .serialNumber(entity.getSerialNumber()).status(EquipmentStatusApi.valueOf(entity.getStatus().name()))
                .holderUserId(entity.getHolderUserId()).currentIssueMovementId(entity.getCurrentIssueMovementId())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
