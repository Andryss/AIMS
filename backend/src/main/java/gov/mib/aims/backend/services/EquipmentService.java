package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.EquipmentHistoryListResponse;
import gov.mib.aims.backend.generated.model.EquipmentListResponse;
import gov.mib.aims.backend.generated.model.EquipmentResponse;
import gov.mib.aims.backend.generated.model.EquipmentStatusApi;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveEquipmentRequest;

/**
 * Просмотр и редактирование складского учета.
 */
public interface EquipmentService {

    /**
     * Возвращает страницу оборудования.
     */
    EquipmentListResponse list(String q, EquipmentTypeApi type, EquipmentStatusApi status,
                               Long holderUserId, int page, int size);

    /**
     * Регистрирует экземпляр на складе.
     */
    EquipmentResponse create(SaveEquipmentRequest request);

    /**
     * Исправляет реквизиты экземпляра без изменения держателя.
     */
    EquipmentResponse update(Long id, SaveEquipmentRequest request);

    /**
     * Возвращает карточку экземпляра.
     */
    EquipmentResponse getById(Long id);

    /**
     * Возвращает страницу истории экземпляра.
     */
    EquipmentHistoryListResponse listHistory(Long id, int page, int size);
}
