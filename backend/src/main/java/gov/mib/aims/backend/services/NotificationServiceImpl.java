package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.NotificationEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.model.EntityRef;
import gov.mib.aims.backend.model.NotificationRecord;
import gov.mib.aims.backend.repository.NotificationRepository;
import gov.mib.aims.backend.services.mapping.NotificationMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link NotificationService}.
 */
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final ChronoService chronoService;

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
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
                .createdAt(chronoService.now())
                .build();
        entity = notificationRepository.save(entity);
        return notificationMapper.toRecord(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationRecord> listForCurrentUser(int page, int size) {
        Long userId = currentUserService.getCurrentUserId();
        Pageable pageable = PageParameters.of(page, size).toPageable();
        return notificationRepository.findByRecipientUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(notificationMapper::toRecord);
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
            entity.setReadAt(chronoService.now());
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

}
