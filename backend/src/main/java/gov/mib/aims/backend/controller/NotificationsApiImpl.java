package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.NotificationsApi;
import gov.mib.aims.backend.generated.model.NotificationListResponse;
import gov.mib.aims.backend.generated.model.UnreadCountResponse;
import gov.mib.aims.backend.model.NotificationRecord;
import gov.mib.aims.backend.services.NotificationService;
import gov.mib.aims.backend.services.mapping.NotificationMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер уведомлений.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class NotificationsApiImpl implements NotificationsApi {

    private final NotificationMapper notificationMapper;

    private final NotificationService notificationService;

    @Override
    @PreAuthorize("hasAuthority('NOTIFICATION_READ')")
    public NotificationListResponse listNotifications(Integer page, Integer size) {
        var pagination = PageParameters.of(page, size, 20);
        log.info("GET /api/v1/notifications page={} size={}", pagination.page(), pagination.size());
        Page<NotificationRecord> result = notificationService.listForCurrentUser(pagination.page(), pagination.size());
        return notificationMapper.toListResponse(result);
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

}
