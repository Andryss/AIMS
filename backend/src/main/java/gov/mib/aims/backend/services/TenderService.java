package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.ChangeTenderStatusRequest;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveTenderRequest;
import gov.mib.aims.backend.generated.model.SelectTenderWinnerRequest;
import gov.mib.aims.backend.generated.model.TenderHistoryListResponse;
import gov.mib.aims.backend.generated.model.TenderListResponse;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.generated.model.TenderStatusApi;

/**
 * Тендеры на закупку и их жизненный цикл.
 */
public interface TenderService {

    /**
     * Создаёт потребность в закупке оборудования.
     */
    TenderResponse create(SaveTenderRequest request);

    /**
     * Возвращает карточку тендера.
     */
    TenderResponse getById(Long id);

    /**
     * Изменяет условия закупки в доступном состоянии.
     */
    TenderResponse update(Long id, SaveTenderRequest request);

    /**
     * Ищет тендеры с пагинацией.
     */
    TenderListResponse list(String q, TenderStatusApi status, EquipmentTypeApi type,
                            Long incidentId, int page, int size);

    /**
     * Начинает сбор предложений либо отменяет тендер.
     */
    TenderResponse changeStatus(Long id, ChangeTenderStatusRequest request);

    /**
     * Выбирает победителя и переводит тендер в ожидание поставки.
     */
    TenderResponse selectWinner(Long id, SelectTenderWinnerRequest request);

    /**
     * Возвращает хронологические снимки изменений.
     */
    TenderHistoryListResponse listHistory(Long id, int page, int size);
}
