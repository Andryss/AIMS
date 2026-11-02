package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.util.List;

/**
 * Независимое представление заявки для API и аудита.
 */
@Component
public class EquipmentMovementMapper {

    /**
     * Создает снимок заявки с реквизитами выбранных экземпляров.
     */
    public EquipmentMovementResponse toResponse(EquipmentMovementEntity entity, List<EquipmentResponse> equipment) {
        return new EquipmentMovementResponse().id(entity.getId())
                .operation(EquipmentMovementOperationApi.valueOf(entity.getOperation().name()))
                .status(EquipmentMovementStatusApi.valueOf(entity.getStatus().name()))
                .incidentId(entity.getIncidentId()).responsibleUserId(entity.getResponsibleUserId())
                .sourceIssueMovementId(entity.getSourceIssueMovementId()).createdByUserId(entity.getCreatedByUserId())
                .comment(entity.getComment()).createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC))
                .completedAt(entity.getCompletedAt() == null ? null
                        : entity.getCompletedAt().atOffset(ZoneOffset.UTC)).equipment(equipment);
    }
}
