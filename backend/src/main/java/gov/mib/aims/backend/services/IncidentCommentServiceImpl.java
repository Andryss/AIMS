package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.IncidentCommentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.CreateIncidentCommentRequest;
import gov.mib.aims.backend.generated.model.IncidentCommentListResponse;
import gov.mib.aims.backend.generated.model.IncidentCommentResponse;
import gov.mib.aims.backend.repository.IncidentCommentRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

/**
 * Реализация {@link IncidentCommentService}.
 */
@Service
@RequiredArgsConstructor
public class IncidentCommentServiceImpl implements IncidentCommentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final IncidentCommentRepository incidentCommentRepository;
    private final IncidentRepository incidentRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Override
    @Transactional
    public IncidentCommentResponse create(Long incidentId, CreateIncidentCommentRequest request) {
        assertIncidentExists(incidentId);
        return toResponse(saveComment(incidentId, request.getText().trim(), currentUserService.getCurrentUserId()));
    }

    @Override
    @Transactional
    public void createFromStatusChange(Long incidentId, String text) {
        if (text == null || text.isBlank()) {
            return;
        }
        assertIncidentExists(incidentId);
        saveComment(incidentId, text.trim(), currentUserService.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public IncidentCommentListResponse list(Long incidentId, int page, int size) {
        assertIncidentExists(incidentId);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<IncidentCommentEntity> result = incidentCommentRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(
                incidentId,
                PageRequest.of(page, pageSize)
        );
        return new IncidentCommentListResponse()
                .items(result.getContent().stream()
                        .map(this::toResponse)
                        .toList())
                .page(result.getNumber())
                .size(result.getSize())
                .totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    private IncidentCommentEntity saveComment(Long incidentId, String text, Long authorUserId) {
        IncidentCommentEntity entity = IncidentCommentEntity.builder()
                .incidentId(incidentId)
                .authorUserId(authorUserId)
                .text(text)
                .createdAt(Instant.now(clock))
                .build();
        return incidentCommentRepository.save(entity);
    }

    private void assertIncidentExists(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw Errors.incidentNotFound();
        }
    }

    private IncidentCommentResponse toResponse(IncidentCommentEntity entity) {
        return new IncidentCommentResponse()
                .id(entity.getId())
                .incidentId(entity.getIncidentId())
                .authorUserId(entity.getAuthorUserId())
                .text(entity.getText())
                .createdAt(entity.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
