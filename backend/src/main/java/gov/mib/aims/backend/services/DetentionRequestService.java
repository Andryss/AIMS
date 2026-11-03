package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.DetentionRequestHistoryListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.generated.model.SaveDetentionRequest;
import gov.mib.aims.backend.generated.model.ChangeDetentionRequestStatusRequest;

/**
 * Заявки на заключение и история их изменений.
 */
public interface DetentionRequestService {

    /**
     * Создаёт новую заявку для незавершённого инцидента.
     */
    DetentionRequestResponse create(SaveDetentionRequest request);

    /**
     * Возвращает карточку заявки.
     */
    DetentionRequestResponse getById(Long id);

    /**
     * Изменяет сведения о субъекте и назначенный срок.
     */
    DetentionRequestResponse update(Long id, SaveDetentionRequest request);

    /**
     * Изменяет статус рассмотрения и фактического заключения.
     */
    DetentionRequestResponse changeStatus(Long id, ChangeDetentionRequestStatusRequest request);

    /**
     * Ищет заявки по субъекту, инциденту и статусу.
     */
    DetentionRequestListResponse list(String q, Long incidentId, DetentionRequestStatusApi status, int page, int size);

    /**
     * Возвращает сохранённые снимки в порядке изменений.
     */
    DetentionRequestHistoryListResponse listHistory(Long id, int page, int size);
}
