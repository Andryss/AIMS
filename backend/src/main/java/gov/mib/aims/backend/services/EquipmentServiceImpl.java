package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEventApi;
import gov.mib.aims.backend.generated.model.EquipmentHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentHistorySnapshot;
import gov.mib.aims.backend.generated.model.EquipmentListResponse;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.EquipmentStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveEquipmentRequest;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.EquipmentType;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.equipment.EquipmentRegistrationService;
import gov.mib.aims.backend.services.history.HistoryReader;
import gov.mib.aims.backend.services.mapping.EquipmentMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import gov.mib.aims.backend.services.transaction.RetryableSerializable;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/**
 * Складской учет и аудит изменений реквизитов.
 */
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final HistoryReader historyReader;
    private final EquipmentRegistrationService registrationService;
    private final EquipmentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public EquipmentListResponse list(String q, EquipmentTypeApi type, EquipmentStatusApi status,
                                      Long holderUserId, int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Page<EquipmentEntity> result = equipmentRepository.search(query,
                type == null ? null : type.name(), status == null ? null : status.name(), holderUserId,
                PageParameters.of(page, size).toPageable());

        return mapper.toListResponse(result);
    }

    @Override
    @Transactional
    public EquipmentResponse create(SaveEquipmentRequest request) {
        return registrationService.register(EquipmentType.valueOf(request.getType().name()),
                request.getModel(), request.getSerialNumber(), null, EquipmentHistoryEventApi.REGISTERED);
    }

    @Override
    @RetryableSerializable
    public EquipmentResponse update(Long id, SaveEquipmentRequest request) {
        EquipmentEntity entity = equipmentRepository.findById(id).orElseThrow(Errors::equipmentNotFound);

        return registrationService.update(entity, EquipmentType.valueOf(request.getType().name()),
                request.getModel(), request.getSerialNumber());
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentResponse getById(Long id) {
        return mapper.toResponse(equipmentRepository.findById(id).orElseThrow(Errors::equipmentNotFound));
    }

    @Override
    @Transactional(readOnly = true)
    public EquipmentHistoryListResponse listHistory(Long id, int page, int size) {
        if (!equipmentRepository.existsById(id)) {
            throw Errors.equipmentNotFound();
        }

        var result = historyReader.read(EntityType.EQUIPMENT, id, page, size,
                EquipmentHistorySnapshot.class);

        return mapper.toHistoryListResponse(result);
    }
}
