package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.EquipmentApi;
import gov.mib.aims.backend.generated.model.EquipmentHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentListResponse;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.EquipmentStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveEquipmentRequest;
import gov.mib.aims.backend.services.equipment.EquipmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * API учета экземпляров оборудования.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class EquipmentApiImpl implements EquipmentApi {

    private final EquipmentService service;

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_READ')")
    public EquipmentListResponse listEquipment(String q, EquipmentTypeApi type, EquipmentStatusApi status,
                                               Long holderUserId, Integer page, Integer size) {
        log.info("GET /api/v1/equipment page={} size={}", page, size);

        return service.list(q, type, status, holderUserId, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_WRITE')")
    public EquipmentResponse createEquipment(SaveEquipmentRequest request) {
        log.info("POST /api/v1/equipment");

        return service.create(request);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_WRITE')")
    public EquipmentResponse updateEquipment(Long id, SaveEquipmentRequest request) {
        log.info("PUT /api/v1/equipment/{}", id);

        return service.update(id, request);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_READ')")
    public EquipmentResponse getEquipment(Long id) {
        log.info("GET /api/v1/equipment/{}", id);

        return service.getById(id);
    }

    @Override
    @PreAuthorize("hasAuthority('EQUIPMENT_READ')")
    public EquipmentHistoryListResponse listEquipmentHistory(Long id, Integer page, Integer size) {
        log.info("GET /api/v1/equipment/{}/history page={} size={}", id, page, size);

        return service.listHistory(id, page, size);
    }
}
