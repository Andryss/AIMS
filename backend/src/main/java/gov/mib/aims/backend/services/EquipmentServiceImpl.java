package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEntry;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEventApi;
import gov.mib.aims.backend.generated.model.EquipmentHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentHistorySnapshot;
import gov.mib.aims.backend.generated.model.EquipmentListResponse;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.EquipmentStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveEquipmentRequest;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.EquipmentStatus;
import gov.mib.aims.backend.model.EquipmentType;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.mapping.EquipmentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.retry.annotation.Retryable;

import java.time.ZoneOffset;
import java.util.Locale;

/**
 * Складской учет и аудит изменений реквизитов.
 */
@Service
@RequiredArgsConstructor
public class EquipmentServiceImpl implements EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final EntityHistoryRepository historyRepository;
    private final EntityHistoryService historyService;
    private final ObjectMapperWrapper objectMapper;
    private final EquipmentMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public EquipmentListResponse list(String q, EquipmentTypeApi type, EquipmentStatusApi status,
                                      Long holderUserId, int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        Page<EquipmentEntity> result = equipmentRepository.search(query,
                type == null ? null : type.name(), status == null ? null : status.name(), holderUserId,
                PageRequest.of(page, size));

        return new EquipmentListResponse().items(result.map(mapper::toResponse).getContent())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages());
    }

    @Override
    @Transactional
    public EquipmentResponse create(SaveEquipmentRequest request) {
        EquipmentEntity entity = new EquipmentEntity();
        entity.setStatus(EquipmentStatus.AVAILABLE);

        return save(entity, request, EquipmentHistoryEventApi.REGISTERED);
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public EquipmentResponse update(Long id, SaveEquipmentRequest request) {
        EquipmentEntity entity = equipmentRepository.findById(id).orElseThrow(Errors::equipmentNotFound);

        return save(entity, request, EquipmentHistoryEventApi.UPDATED);
    }

    private EquipmentResponse save(EquipmentEntity entity, SaveEquipmentRequest request,
                                   EquipmentHistoryEventApi event) {
        entity.setType(EquipmentType.valueOf(request.getType().name()));
        entity.setModel(request.getModel().trim());
        entity.setSerialNumber(request.getSerialNumber().trim());

        try {
            equipmentRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw Errors.equipmentConflict("Оборудование с таким серийным номером уже существует");
        }

        EquipmentResponse response = mapper.toResponse(entity);

        historyService.recordChange(EntityType.EQUIPMENT, entity.getId(),
                new EquipmentHistorySnapshot().event(event).equipment(response));

        return response;
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

        var result = historyRepository.findByEntityTypeAndEntityIdOrderByChangedAtAsc(EntityType.EQUIPMENT, id,
                PageRequest.of(page, size, Sort.by("id")));

        return new EquipmentHistoryListResponse().items(result.map(entry -> new EquipmentHistoryEntry()
                        .id(entry.getId()).changedAt(entry.getChangedAt().atOffset(ZoneOffset.UTC))
                        .changedByUserId(entry.getChangedByUserId())
                        .snapshot(objectMapper.readValue(entry.getSnapshot(), EquipmentHistorySnapshot.class)))
                .getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages());
    }
}
