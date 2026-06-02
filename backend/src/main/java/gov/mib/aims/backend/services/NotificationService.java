package gov.mib.aims.backend.services;

import gov.mib.aims.backend.model.NotificationRecord;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Сервис уведомлений.
 */
public interface NotificationService {

    int MAX_PAGE_SIZE = 100;

    /**
     * Создаёт уведомление для получателя.
     *
     * @param recipientUserId id получателя
     * @param message текст
     * @param relatedEntities ссылки на сущности (INCIDENT:42)
     * @return созданное уведомление
     */
    NotificationRecord send(Long recipientUserId, String message, List<String> relatedEntities);

    /**
     * Список уведомлений текущего пользователя.
     *
     * @param page номер страницы (0-based)
     * @param size размер страницы
     * @return страница уведомлений
     */
    Page<NotificationRecord> listForCurrentUser(int page, int size);

    /**
     * Число непрочитанных уведомлений текущего пользователя.
     *
     * @return количество
     */
    long countUnreadForCurrentUser();

    /**
     * Помечает уведомление прочитанным (только своё).
     *
     * @param notificationId id уведомления
     */
    void markAsRead(Long notificationId);
}
