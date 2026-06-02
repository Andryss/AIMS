package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.NotificationEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.NotificationRecord;
import gov.mib.aims.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link NotificationService}.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public NotificationRecord send(Long recipientUserId, String message, List<String> relatedEntities) {
        validateSendRequest(recipientUserId, message, relatedEntities);
        List<String> refs = normalizeRelatedEntities(relatedEntities);
        NotificationEntity entity = NotificationEntity.builder()
                .recipientUserId(recipientUserId)
                .message(message.trim())
                .relatedEntities(refs)
                .readAt(null)
                .createdAt(Instant.now())
                .build();
        entity = notificationRepository.save(entity);
        return toRecord(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationRecord> listForCurrentUser(int page, int size) {
        Long userId = currentUserService.getCurrentUserId();
        Pageable pageable = PageRequest.of(page, normalizePageSize(size));
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::toRecord);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnreadForCurrentUser() {
        Long userId = currentUserService.getCurrentUserId();
        return notificationRepository.countByRecipientUserIdAndReadAtIsNull(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long notificationId) {
        if (notificationId == null) {
            throw Errors.validationError("notificationId is required");
        }
        Long userId = currentUserService.getCurrentUserId();
        NotificationEntity entity = notificationRepository.findByIdAndRecipientUserId(notificationId, userId)
                .orElseThrow(Errors::notificationNotFound);
        if (entity.getReadAt() == null) {
            entity.setReadAt(Instant.now());
            notificationRepository.save(entity);
        }
    }

    private void validateSendRequest(Long recipientUserId, String message, List<String> relatedEntities) {
        if (recipientUserId == null) {
            throw Errors.validationError("recipientUserId is required");
        }
        if (message == null || message.isBlank()) {
            throw Errors.validationError("message is required");
        }
        if (relatedEntities != null) {
            for (String ref : relatedEntities) {
                EntityRef.parse(ref);
            }
        }
    }

    private List<String> normalizeRelatedEntities(List<String> relatedEntities) {
        if (relatedEntities == null || relatedEntities.isEmpty()) {
            return new ArrayList<>();
        }
        return List.copyOf(relatedEntities);
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private NotificationRecord toRecord(NotificationEntity entity) {
        return new NotificationRecord(
                entity.getId(),
                entity.getMessage(),
                List.copyOf(entity.getRelatedEntities()),
                entity.getReadAt() != null,
                entity.getReadAt(),
                entity.getCreatedAt()
        );
    }
}
