package gov.mib.aims.backend.repository;

import gov.mib.aims.backend.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Репозиторий уведомлений.
 */
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    /**
     * Список уведомлений получателя, от новых к старым.
     *
     * @param recipientUserId id получателя
     * @param pageable пагинация
     * @return страница уведомлений
     */
    Page<NotificationEntity> findByRecipientUserIdOrderByCreatedAtDesc(
            Long recipientUserId,
            Pageable pageable
    );

    /**
     * Количество непрочитанных уведомлений получателя.
     *
     * @param recipientUserId id получателя
     * @return число непрочитанных
     */
    long countByRecipientUserIdAndReadAtIsNull(Long recipientUserId);

    /**
     * Уведомление по id, принадлежащее получателю.
     *
     * @param id id уведомления
     * @param recipientUserId id получателя
     * @return уведомление, если найдено
     */
    Optional<NotificationEntity> findByIdAndRecipientUserId(Long id, Long recipientUserId);
}
