package gov.mib.aims.backend.services.equipment;

import gov.mib.aims.backend.generated.model.EquipmentMovementHistoryListResponse;
import gov.mib.aims.backend.generated.model.ChangeEquipmentMovementStatusRequest;
import gov.mib.aims.backend.generated.model.EquipmentMovementListResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementOperationApi;
import gov.mib.aims.backend.generated.model.EquipmentMovementResponse;
import gov.mib.aims.backend.generated.model.EquipmentMovementStatusApi;
import gov.mib.aims.backend.generated.model.SaveEquipmentMovementRequest;

/**
 * Заявки на выдачу и возврат оборудования.
 */
public interface EquipmentMovementService {

    /**
     * Возвращает страницу заявок с указанными фильтрами.
     */
    EquipmentMovementListResponse list(Long incidentId, Long responsibleUserId, EquipmentMovementStatusApi status,
                                       EquipmentMovementOperationApi operation, Long equipmentId, int page, int size);

    /**
     * Создает заявку в ожидании обработки.
     */
    EquipmentMovementResponse create(SaveEquipmentMovementRequest request);

    /**
     * Исправляет заявку, еще не принятую в обработку.
     */
    EquipmentMovementResponse update(Long id, SaveEquipmentMovementRequest request);

    /**
     * Меняет статус и выполняет фактическую передачу при завершении заявки.
     */
    EquipmentMovementResponse changeStatus(Long id, ChangeEquipmentMovementStatusRequest request);

    /**
     * Возвращает карточку заявки с текущими данными оборудования.
     */
    EquipmentMovementResponse getById(Long id);

    /**
     * Возвращает историю заявки с реквизитами на момент изменения.
     */
    EquipmentMovementHistoryListResponse listHistory(Long id, int page, int size);
}
