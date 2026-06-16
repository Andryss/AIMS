package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.generated.model.SetIncidentExecutorsRequest;
import gov.mib.aims.backend.generated.model.SetIncidentResponsibleRequest;

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

    /**
     * Возвращает страницу инцидентов.
     *
     * @param page номер страницы (с нуля)
     * @param size размер страницы
     * @return список инцидентов с метаданными пагинации
     */
    IncidentListResponse list(int page, int size);

    /**
     * Привязывает тип инопланетянина к инциденту.
     *
     * @param id идентификатор инцидента
     * @param request id записи справочника
     * @return обновлённый инцидент
     */
    IncidentResponse linkAlien(Long id, LinkIncidentAlienRequest request);

    /**
     * Назначает или снимает ответственного агента.
     *
     * @param id идентификатор инцидента
     * @param request id пользователя или null для снятия
     * @return обновлённый инцидент
     */
    IncidentResponse setResponsible(Long id, SetIncidentResponsibleRequest request);

    /**
     * Полностью заменяет список исполнителей.
     *
     * @param id идентификатор инцидента
     * @param request список id исполнителей
     * @return обновлённый инцидент
     */
    IncidentResponse setExecutors(Long id, SetIncidentExecutorsRequest request);
}
