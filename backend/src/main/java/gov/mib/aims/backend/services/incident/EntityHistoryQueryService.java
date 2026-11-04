package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.generated.model.IncidentHistoryListResponse;

/**
 * Чтение истории изменений сущностей.
 */
public interface EntityHistoryQueryService {

    /**
     * Возвращает историю изменений инцидента.
     *
     * @param incidentId идентификатор инцидента
     * @param page номер страницы
     * @param size размер страницы
     * @return записи истории в хронологическом порядке
     */
    IncidentHistoryListResponse listIncidentHistory(Long incidentId, int page, int size);
}
