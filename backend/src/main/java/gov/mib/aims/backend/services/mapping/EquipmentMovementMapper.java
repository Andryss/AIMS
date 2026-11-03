package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.EquipmentMovementEntity;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
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
}
