package gov.mib.aims.backend.services.incident.status.precondition;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.EquipmentMovementStatus;
import gov.mib.aims.backend.repository.EquipmentMovementRepository;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.incident.IncidentTransitionContext;
import gov.mib.aims.backend.services.status.StatusTransitionPrecondition;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Закрытие инцидента возможно после завершения движений и возврата всех экземпляров.
 */
@Component
@RequiredArgsConstructor
public class EquipmentReturnedPrecondition implements StatusTransitionPrecondition<IncidentTransitionContext> {

    private final EquipmentMovementRepository movementRepository;
    private final EquipmentRepository equipmentRepository;

    @Override
    public void check(IncidentTransitionContext context) {
        var incident = context.incident();
        if (movementRepository.existsByIncidentIdAndStatusIn(incident.getId(),
                List.of(EquipmentMovementStatus.PENDING, EquipmentMovementStatus.PROCESSING))) {
            throw Errors.equipmentConflict("Завершите или отмените открытые заявки на движение оборудования");
        }
        if (equipmentRepository.existsIssuedByIncidentId(incident.getId())) {
            throw Errors.equipmentConflict("Перед завершением инцидента верните выданное оборудование на склад");
        }
    }
}
