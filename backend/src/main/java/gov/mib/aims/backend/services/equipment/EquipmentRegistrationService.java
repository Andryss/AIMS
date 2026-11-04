package gov.mib.aims.backend.services.equipment;

import gov.mib.aims.backend.entity.EquipmentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEventApi;
import gov.mib.aims.backend.generated.model.EquipmentHistorySnapshot;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.EquipmentStatus;
import gov.mib.aims.backend.model.EquipmentType;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.history.EntityHistoryService;
import gov.mib.aims.backend.services.mapping.EquipmentMapper;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Регистрация и сохранение реквизитов оборудования с общими правилами и аудитом.
 */
@Service
@RequiredArgsConstructor
public class EquipmentRegistrationService {

    private final EquipmentRepository repository;
    private final EquipmentMapper mapper;
    private final EntityHistoryService historyService;

    /**
     * Регистрирует доступный экземпляр в транзакции вызывающего сервиса.
     */
    @Transactional
    public EquipmentResponse register(EquipmentType type, String model, String serialNumber,
                                       Long sourceTenderId, EquipmentHistoryEventApi event) {
        EquipmentEntity entity = new EquipmentEntity();
        entity.setStatus(EquipmentStatus.AVAILABLE);
        entity.setSourceTenderId(sourceTenderId);

        return save(entity, type, model, serialNumber, event);
    }

    /**
     * Меняет реквизиты, сохраняя происхождение и текущее состояние экземпляра.
     */
    @Transactional
    public EquipmentResponse update(EquipmentEntity entity, EquipmentType type, String model, String serialNumber) {
        return save(entity, type, model, serialNumber, EquipmentHistoryEventApi.UPDATED);
    }

    private EquipmentResponse save(EquipmentEntity entity, EquipmentType type, String model, String serialNumber,
                                    EquipmentHistoryEventApi event) {
        String normalizedModel = TextNormalizer.required(model, "Укажите модель и серийный номер оборудования");
        String normalizedSerial = TextNormalizer.required(serialNumber, "Укажите модель и серийный номер оборудования");

        entity.setType(type);
        entity.setModel(normalizedModel);
        entity.setSerialNumber(normalizedSerial);
        try {
            repository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException ex) {
            throw Errors.equipmentConflict("Оборудование с таким серийным номером уже существует");
        }

        EquipmentResponse response = mapper.toResponse(entity);
        historyService.recordChange(EntityType.EQUIPMENT, entity.getId(),
                new EquipmentHistorySnapshot().event(event).equipment(response));

        return response;
    }
}
