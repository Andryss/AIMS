package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.CreateIncidentCommentRequest;
import gov.mib.aims.backend.generated.model.IncidentCommentListResponse;
import gov.mib.aims.backend.generated.model.IncidentCommentResponse;

/**
 * Сервис комментариев к инцидентам.
 */
public interface IncidentCommentService {

    /**
     * Создаёт комментарий к инциденту.
     *
     * @param incidentId идентификатор инцидента
     * @param request текст комментария
     * @return созданный комментарий
     */
    IncidentCommentResponse create(Long incidentId, CreateIncidentCommentRequest request);

    /**
     * Создаёт комментарий от текущего пользователя (без проверки INCIDENT_COMMENT — для смены статуса).
     *
     * @param incidentId идентификатор инцидента
     * @param text текст комментария
     */
    void createFromStatusChange(Long incidentId, String text);

    /**
     * Возвращает страницу комментариев инцидента.
     *
     * @param incidentId идентификатор инцидента
     * @param page номер страницы
     * @param size размер страницы
     * @return список комментариев
     */
    IncidentCommentListResponse list(Long incidentId, int page, int size);
}
