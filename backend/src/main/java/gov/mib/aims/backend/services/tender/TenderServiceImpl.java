package gov.mib.aims.backend.services.tender;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.ChangeTenderStatusRequest;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveTenderRequest;
import gov.mib.aims.backend.generated.model.SelectTenderWinnerRequest;
import gov.mib.aims.backend.generated.model.TenderHistoryListResponse;
import gov.mib.aims.backend.generated.model.TenderHistorySnapshot;
import gov.mib.aims.backend.generated.model.TenderListResponse;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.generated.model.TenderStatusApi;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.EquipmentType;
import gov.mib.aims.backend.model.Permission;
import gov.mib.aims.backend.model.TenderParticipantStatus;
import gov.mib.aims.backend.model.TenderStatus;
import gov.mib.aims.backend.repository.IncidentRepository;
import gov.mib.aims.backend.repository.TenderParticipantRepository;
import gov.mib.aims.backend.repository.TenderRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.auth.CurrentUserService;
import gov.mib.aims.backend.services.history.HistoryReader;
import gov.mib.aims.backend.services.mapping.TenderMapper;
import gov.mib.aims.backend.services.query.PageParameters;
import gov.mib.aims.backend.services.transaction.RetryableSerializable;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Закупочные потребности, доступ к редактированию и аудит тендеров.
 */
@Service
@RequiredArgsConstructor
public class TenderServiceImpl implements TenderService {

    private final TenderRepository repository;
    private final IncidentRepository incidentRepository;
    private final HistoryReader historyReader;
    private final TenderHistoryService historyService;
    private final TenderParticipantRepository participantRepository;
    private final CurrentUserService currentUserService;
    private final TenderMapper mapper;
    private final TenderWorkflow workflow;
    private final ChronoService chronoService;

    @Override
    @RetryableSerializable
    public TenderResponse create(SaveTenderRequest request) {
        if (request.getIncidentId() != null && !incidentRepository.existsById(request.getIncidentId())) {
            throw Errors.incidentNotFound();
        }

        TenderEntity entity = new TenderEntity();
        entity.setStatus(TenderStatus.NEED);
        entity.setCreatedByUserId(currentUserService.getCurrentUserId());
        entity.setIncidentId(request.getIncidentId());
        applyRequest(entity, request);

        return save(entity, null);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TenderResponse getById(Long id) {
        TenderEntity tender = find(id);
        Long winner = participantRepository.findByTenderIdInAndStatus(List.of(id), TenderParticipantStatus.WINNER)
                .stream().map(item -> item.getId()).findFirst().orElse(null);

        return mapper.toResponse(tender, winner);
    }

    @Override
    @RetryableSerializable
    public TenderResponse update(Long id, SaveTenderRequest request) {
        TenderEntity entity = find(id);
        if (entity.getStatus() != TenderStatus.NEED && entity.getStatus() != TenderStatus.COLLECTING_OFFERS) {
            throw Errors.tenderConflict("Изменение тендера недоступно в этом статусе");
        }

        boolean manager = currentUserService.hasPermission(Permission.TENDER_MANAGE);
        boolean ownNeed = entity.getStatus() == TenderStatus.NEED
                && entity.getCreatedByUserId().equals(currentUserService.getCurrentUserId());
        if (!manager && !ownNeed) {
            throw Errors.accessDenied();
        }
        if (!Objects.equals(entity.getIncidentId(), request.getIncidentId())) {
            throw Errors.validationError("Инцидент-основание тендера нельзя изменить");
        }

        EquipmentType requestedType = EquipmentType.valueOf(request.getEquipmentType().name());
        boolean changedTypeOrQuantity = entity.getEquipmentType() != requestedType
                || !entity.getQuantity().equals(request.getQuantity());
        if (changedTypeOrQuantity && participantRepository.existsByTenderId(id)) {
            throw Errors.tenderConflict("После получения предложений тип и количество оборудования менять нельзя");
        }

        applyRequest(entity, request);

        return save(entity, null);
    }

    @Override
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public TenderListResponse list(String q, TenderStatusApi status, EquipmentTypeApi type,
                                   Long incidentId, int page, int size) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        var result = repository.search(query, status == null ? null : status.name(),
                type == null ? null : type.name(), incidentId, PageParameters.of(page, size).toPageable());

        var ids = result.getContent().stream().map(TenderEntity::getId).toList();
        var winners = participantRepository.findByTenderIdInAndStatus(ids, TenderParticipantStatus.WINNER).stream()
                .collect(Collectors.toMap(item -> item.getTenderId(), item -> item.getId()));

        return mapper.toListResponse(result, winners);
    }

    @Override
    @RetryableSerializable
    public TenderResponse changeStatus(Long id, ChangeTenderStatusRequest request) {
        TenderEntity entity = find(id);
        workflow.changeStatus(entity, TenderStatus.valueOf(request.getStatus().name()), request.getComment());

        return save(entity, request.getComment());
    }

    @Override
    @RetryableSerializable
    public TenderResponse selectWinner(Long id, SelectTenderWinnerRequest request) {
        TenderEntity entity = find(id);
        var participant = participantRepository.findByIdAndTenderId(request.getParticipantId(), id)
                .orElseThrow(Errors::tenderParticipantNotFound);
        workflow.selectWinner(entity);

        participant.setStatus(TenderParticipantStatus.WINNER);
        participant.setUpdatedAt(entity.getUpdatedAt());
        participantRepository.save(participant);

        return save(entity, null);
    }

    @Override
    @Transactional(readOnly = true)
    public TenderHistoryListResponse listHistory(Long id, int page, int size) {
        find(id);

        var result = historyReader.read(EntityType.TENDER, id, page, size,
                TenderHistorySnapshot.class);

        return mapper.toHistoryListResponse(result);
    }

    private TenderEntity find(Long id) {
        return repository.findById(id).orElseThrow(Errors::tenderNotFound);
    }

    private void applyRequest(TenderEntity entity, SaveTenderRequest request) {
        entity.setEquipmentType(EquipmentType.valueOf(request.getEquipmentType().name()));
        entity.setQuantity(request.getQuantity());
        entity.setConditions(TextNormalizer.optional(request.getConditions()));
        entity.setUpdatedAt(chronoService.now());
    }

    private TenderResponse save(TenderEntity entity, String comment) {
        repository.saveAndFlush(entity);
        return historyService.record(entity, comment);
    }
}
