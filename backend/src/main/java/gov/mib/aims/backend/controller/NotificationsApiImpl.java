package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.NotificationsApi;
import gov.mib.aims.backend.generated.model.NotificationItem;
import gov.mib.aims.backend.generated.model.NotificationListResponse;
import gov.mib.aims.backend.generated.model.UnreadCountResponse;
import gov.mib.aims.backend.model.NotificationRecord;
import gov.mib.aims.backend.services.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

/**
 * REST-контроллер уведомлений.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationsApiImpl implements NotificationsApi {

    private final NotificationService notificationService;

    @Override
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public NotificationListResponse listNotifications(Integer page, Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        log.info("GET /api/v1/notifications page={} size={}", pageNumber, pageSize);
        Page<NotificationRecord> result = notificationService.listForCurrentUser(pageNumber, pageSize);
        return new NotificationListResponse()
                .items(result.getContent().stream().map(this::toItem).toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    @Override
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public UnreadCountResponse getUnreadNotificationsCount() {
        log.info("GET /api/v1/notifications/unread-count");
        return new UnreadCountResponse().count(notificationService.countUnreadForCurrentUser());
    }

    @Override
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public void markNotificationRead(Long id) {
        log.info("PATCH /api/v1/notifications/{}/read", id);
        notificationService.markAsRead(id);
    }

    private NotificationItem toItem(NotificationRecord record) {
        NotificationItem item = new NotificationItem()
                .id(record.id())
                .message(record.message())
                .relatedEntities(record.relatedEntities())
                .read(record.read())
                .createdAt(toOffsetDateTime(record.createdAt()));
        if (record.readAt() != null) {
            item.readAt(toOffsetDateTime(record.readAt()));
        }
        return item;
    }

    private static OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
