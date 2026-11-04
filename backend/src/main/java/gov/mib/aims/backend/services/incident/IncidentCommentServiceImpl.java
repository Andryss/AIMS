package gov.mib.aims.backend.services.incident;

import gov.mib.aims.backend.entity.IncidentCommentEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.CreateIncidentCommentRequest;
import gov.mib.aims.backend.generated.model.IncidentCommentListResponse;
import gov.mib.aims.backend.generated.model.IncidentCommentResponse;
import gov.mib.aims.backend.repository.IncidentCommentRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.mapping.IncidentCommentMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Реализация {@link IncidentCommentService}.
 */
@Service
@RequiredArgsConstructor
public class IncidentCommentServiceImpl implements IncidentCommentService {

    private final IncidentCommentMapper commentMapper;

    private final IncidentCommentRepository incidentCommentRepository;
    private final IncidentRepository incidentRepository;
    private final CurrentUserService currentUserService;
    private final ChronoService chronoService;

    @Override
    @Transactional
    public IncidentCommentResponse create(Long incidentId, CreateIncidentCommentRequest request) {
        assertIncidentExists(incidentId);
        String text = TextNormalizer.required(request.getText(), "Укажите текст комментария");
        return commentMapper.toResponse(saveComment(incidentId, text, currentUserService.getCurrentUserId()));
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
        Page<IncidentCommentEntity> result = incidentCommentRepository.findByIncidentIdOrderByCreatedAtAscIdAsc(
                incidentId,
                PageParameters.of(page, size).toPageable()
        );
        return commentMapper.toListResponse(result);
    }

    private IncidentCommentEntity saveComment(Long incidentId, String text, Long authorUserId) {
        IncidentCommentEntity entity = IncidentCommentEntity.builder()
                .incidentId(incidentId)
                .authorUserId(authorUserId)
                .text(text)
                .createdAt(chronoService.now())
                .build();
        return incidentCommentRepository.save(entity);
    }

    private void assertIncidentExists(Long incidentId) {
        if (!incidentRepository.existsById(incidentId)) {
            throw Errors.incidentNotFound();
        }
    }

}
