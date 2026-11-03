package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.DetentionRequestHistoryEntry;
import gov.mib.aims.backend.generated.model.DetentionRequestHistoryListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestHistorySnapshot;
import gov.mib.aims.backend.generated.model.DetentionRequestListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.generated.model.SaveDetentionRequest;
import gov.mib.aims.backend.generated.model.ChangeDetentionRequestStatusRequest;
import gov.mib.aims.backend.model.DetentionRequestStatus;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.IncidentStatus;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.repository.AlienRepository;
import gov.mib.aims.backend.repository.DetentionRequestRepository;
import gov.mib.aims.backend.repository.EntityHistoryRepository;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.services.mapping.DetentionRequestMapper;
import gov.mib.aims.backend.services.detention.DetentionRequestWorkflow;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Objects;

/**
 * Сведения о заключении, права редактирования и независимые снимки изменений.
 */
@Service
@RequiredArgsConstructor
public class DetentionRequestServiceImpl implements DetentionRequestService {

    private final DetentionRequestRepository repository;
    private final IncidentRepository incidentRepository;
    private final AlienRepository alienRepository;
    private final EntityHistoryRepository historyRepository;
    private final EntityHistoryService historyService;
    private final CurrentUserService currentUserService;
    private final ObjectMapperWrapper objectMapper;
    private final DetentionRequestMapper mapper;
    private final DetentionRequestWorkflow workflow;
    private final Clock clock;

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DetentionRequestResponse create(SaveDetentionRequest request) {
        var incident = incidentRepository.findById(request.getIncidentId()).orElseThrow(Errors::incidentNotFound);
        if (incident.getStatus() == IncidentStatus.EXECUTION_COMPLETED) {
            throw Errors.detentionRequestConflict("Создавать заявку можно только для незавершённого инцидента");
        }

        DetentionRequestEntity entity = new DetentionRequestEntity();
        entity.setIncidentId(request.getIncidentId());
        entity.setStatus(DetentionRequestStatus.NEW);
        entity.setCreatedByUserId(currentUserService.getCurrentUserId());
        applyRequest(entity, request);

        return save(entity, request.getComment());
    }

    @Override
    @Transactional(readOnly = true)
    public DetentionRequestResponse getById(Long id) {
        return mapper.toResponse(find(id));
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DetentionRequestResponse update(Long id, SaveDetentionRequest request) {
        DetentionRequestEntity entity = find(id);
        if (entity.getStatus() != DetentionRequestStatus.NEW && entity.getStatus() != DetentionRequestStatus.ACCEPTED
                && entity.getStatus() != DetentionRequestStatus.IN_CUSTODY) {
            throw Errors.detentionRequestConflict("Редактирование заявки недоступно в этом статусе");
        }

        boolean ownNew = entity.getStatus() == DetentionRequestStatus.NEW
                && entity.getCreatedByUserId().equals(currentUserService.getCurrentUserId());
        if (!currentUserService.hasPermission(Permission.DETENTION_PROCESS)
                && !ownNew) {
            throw Errors.accessDenied();
        }
        if (!Objects.equals(entity.getIncidentId(), request.getIncidentId())) {
            throw Errors.validationError("Инцидент-основание заявки нельзя изменить");
        }
        if (!entity.getDurationDays().equals(request.getDurationDays())
                && (request.getComment() == null || request.getComment().trim().isBlank())) {
            throw Errors.validationError("Укажите причину изменения срока заключения");
        }

        applyRequest(entity, request);

        return save(entity, request.getComment());
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public DetentionRequestResponse changeStatus(Long id, ChangeDetentionRequestStatusRequest request) {
        DetentionRequestEntity entity = find(id);
        workflow.changeStatus(entity, DetentionRequestStatus.valueOf(request.getStatus().name()), request.getComment());

        return save(entity, request.getComment());
    }

    @Override
    @Transactional(readOnly = true)
    public DetentionRequestListResponse list(String q, Long incidentId, DetentionRequestStatusApi status,
                                            int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        var result = repository.search(query, incidentId, status == null ? null : status.name(),
                PageRequest.of(page, size));

        return new DetentionRequestListResponse().items(result.map(mapper::toResponse).getContent())
                .page(result.getNumber()).size(result.getSize()).totalElements(result.getTotalElements())
                .totalPages(result.getTotalPages());
    }

    @Override
    @Transactional(readOnly = true)
    public DetentionRequestHistoryListResponse listHistory(Long id, int page, int size) {
        find(id);
        var result = historyRepository.findByEntityTypeAndEntityIdOrderByChangedAtAsc(EntityType.DETENTION_REQUEST,
                id, PageRequest.of(page, size, Sort.by("id")));

        return new DetentionRequestHistoryListResponse().items(result.map(entry -> new DetentionRequestHistoryEntry()
                        .id(entry.getId()).changedAt(entry.getChangedAt().atOffset(ZoneOffset.UTC))
                        .changedByUserId(entry.getChangedByUserId())
                        .snapshot(objectMapper.readValue(entry.getSnapshot(), DetentionRequestHistorySnapshot.class)))
                .getContent()).page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages());
    }

    private DetentionRequestEntity find(Long id) {
        return repository.findById(id).orElseThrow(Errors::detentionRequestNotFound);
    }

    private void applyRequest(DetentionRequestEntity entity, SaveDetentionRequest request) {
        if (request.getSubjectName().trim().isBlank() || request.getSubjectDescription().trim().isBlank()) {
            throw Errors.validationError("Заполните имя и описание субъекта");
        }
        if (request.getAlienId() != null && !alienRepository.existsById(request.getAlienId())) {
            throw Errors.alienNotFound();
        }

        entity.setSubjectName(request.getSubjectName().trim());
        entity.setSubjectDescription(request.getSubjectDescription().trim());
        entity.setAlienId(request.getAlienId());
        entity.setDurationDays(request.getDurationDays());
        entity.setUpdatedAt(clock.instant().truncatedTo(ChronoUnit.MICROS));
    }

    private DetentionRequestResponse save(DetentionRequestEntity entity, String comment) {
        repository.saveAndFlush(entity);
        DetentionRequestResponse response = mapper.toResponse(entity);
        historyService.recordChange(EntityType.DETENTION_REQUEST, entity.getId(), new DetentionRequestHistorySnapshot()
                .request(response).transitionComment(comment == null ? null : comment.trim()));

        return response;
    }
}
