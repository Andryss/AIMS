package gov.mib.aims.backend.services.notification;

import gov.mib.aims.backend.entity.AppUserEntity;
import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.Role;
import gov.mib.aims.backend.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Общая доставка уведомлений выбранным получателям.
 */
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationService notificationService;
    private final AppUserRepository appUserRepository;

    /**
     * Отправляет уведомление всем пользователям с указанной ролью.
     *
     * @param role роль получателей
     * @param message текст уведомления
     * @param entityType тип связанной сущности
     * @param entityId идентификатор связанной сущности
     */
    public void sendToRole(Role role, String message, EntityType entityType, long entityId) {
        List<Long> recipientIds = appUserRepository.findAllByRoleName(role.getCode()).stream()
                .map(AppUserEntity::getId)
                .toList();
        sendToUsers(recipientIds, message, entityType, entityId);
    }

    /**
     * Отправляет по одному уведомлению каждому получателю, пропуская отсутствующие идентификаторы.
     *
     * @param recipientIds идентификаторы получателей; null или пустая коллекция не создают уведомлений
     * @param message текст уведомления
     * @param entityType тип связанной сущности
     * @param entityId идентификатор связанной сущности
     */
    public void sendToUsers(Collection<Long> recipientIds, String message, EntityType entityType, long entityId) {
        if (recipientIds == null || recipientIds.isEmpty()) {
            return;
        }
        List<String> relatedEntities = List.of(EntityRef.format(entityType, entityId));
        for (Long recipientId : new LinkedHashSet<>(recipientIds)) {
            if (recipientId != null) {
                notificationService.send(recipientId, message, relatedEntities);
            }
        }
    }
}
