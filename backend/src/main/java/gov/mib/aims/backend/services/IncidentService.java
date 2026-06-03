package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;

/**
 * Сервис инцидентов.
 */
public interface IncidentService {

    /**
     * Создаёт инцидент в статусе DRAFT.
     *
     * @param request данные инцидента
     * @return созданный инцидент
     */
    IncidentResponse create(CreateIncidentRequest request);

    /**
     * Меняет статус инцидента.
     *
     * @param id идентификатор инцидента
     * @param request целевой статус
     * @return обновлённый инцидент
     */
    IncidentResponse changeStatus(Long id, ChangeIncidentStatusRequest request);

    /**
     * Возвращает инцидент по id.
     *
     * @param id идентификатор
     * @return инцидент
     */
    IncidentResponse getById(Long id);
}
