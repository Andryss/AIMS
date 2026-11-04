package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.entity.NotificationEntity;
import gov.mib.aims.backend.generated.model.NotificationItem;
import gov.mib.aims.backend.generated.model.NotificationListResponse;
import gov.mib.aims.backend.model.NotificationRecord;
import gov.mib.aims.backend.services.ChronoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Представления сохранённых уведомлений для сервиса и API.
 */
@Component
@RequiredArgsConstructor
public class NotificationMapper {

    private final ChronoService chronoService;

    /**
     * Копирует данные уведомления из JPA-сущности.
     */
    public NotificationRecord toRecord(NotificationEntity entity) {
        return new NotificationRecord(
                entity.getId(),
                entity.getMessage(),
                List.copyOf(entity.getRelatedEntities()),
                entity.getReadAt() != null,
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }

    /**
     * Представляет временные метки уведомления в UTC.
     */
    public NotificationItem toItem(NotificationRecord record) {
        NotificationItem item = new NotificationItem()
                .id(record.id())
                .message(record.message())
                .relatedEntities(record.relatedEntities())
                .read(record.read())
                .createdAt(chronoService.toOffsetDateTime(record.createdAt()));
        if (record.readAt() != null) {
            item.readAt(chronoService.toOffsetDateTime(record.readAt()));
        }
        return item;
    }

    /**
     * Собирает элементы и метаданные страницы для API.
     */
    public NotificationListResponse toListResponse(Page<NotificationRecord> page) {
        return new NotificationListResponse().items(page.map(this::toItem).getContent())
                .page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages());
    }
}
