package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.EquipmentMovementsApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementHistoryListResponse;
import gov.mib.aims.backend.generated.model.ChangeEquipmentMovementStatusRequest;
import gov.mib.aims.backend.generated.model.EquipmentMovementListResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.SaveEquipmentMovementRequest;
import gov.mib.aims.backend.services.EquipmentMovementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * API заявок на выдачу и возврат оборудования.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class EquipmentMovementsApiImpl implements EquipmentMovementsApi {

    private final EquipmentMovementService service;

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_MOVEMENT_READ')")
    public EquipmentMovementListResponse listEquipmentMovements(Long incidentId, Long responsibleUserId,
            EquipmentMovementStatusApi status, EquipmentMovementOperationApi operation, Long equipmentId,
            Integer page, Integer size) {
        log.info("GET /api/v1/equipment-movements incidentId={} page={} size={}", incidentId, page, size);

        return service.list(incidentId, responsibleUserId, status, operation, equipmentId, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_MOVEMENT_CREATE')")
    public EquipmentMovementResponse createEquipmentMovement(SaveEquipmentMovementRequest request) {
        log.info("POST /api/v1/equipment-movements");

        return service.create(request);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_MOVEMENT_CREATE')")
    public EquipmentMovementResponse updateEquipmentMovement(Long id, SaveEquipmentMovementRequest request) {
        log.info("PUT /api/v1/equipment-movements/{}", id);

        return service.update(id, request);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('EQUIPMENT_MOVEMENT_CREATE', 'EQUIPMENT_MOVEMENT_PROCESS')")
    public EquipmentMovementResponse changeEquipmentMovementStatus(Long id,
                                                                  ChangeEquipmentMovementStatusRequest request) {
        log.info("PATCH /api/v1/equipment-movements/{}/status target={}", id, request.getStatus());

        return service.changeStatus(id, request);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_MOVEMENT_READ')")
    public EquipmentMovementResponse getEquipmentMovement(Long id) {
        log.info("GET /api/v1/equipment-movements/{}", id);

        return service.getById(id);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_MOVEMENT_READ')")
    public EquipmentMovementHistoryListResponse listEquipmentMovementHistory(Long id, Integer page, Integer size) {
        log.info("GET /api/v1/equipment-movements/{}/history page={} size={}", id, page, size);

        return service.listHistory(id, page, size);
    }
}
