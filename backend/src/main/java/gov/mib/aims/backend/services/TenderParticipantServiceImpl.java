package gov.mib.aims.backend.services;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.entity.TenderParticipantEntity;
import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.SaveTenderParticipantRequest;
import gov.mib.aims.backend.generated.model.TenderParticipantListResponse;
import gov.mib.aims.backend.generated.model.TenderParticipantResponse;
import gov.mib.aims.backend.model.TenderParticipantStatus;
import gov.mib.aims.backend.model.TenderStatus;
import gov.mib.aims.backend.repository.TenderParticipantRepository;
import gov.mib.aims.backend.repository.TenderRepository;
import gov.mib.aims.backend.services.mapping.TenderParticipantMapper;
import gov.mib.aims.backend.services.tender.TenderHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

/**
 * Предложения участников, ограничения жизненного цикла и аудит изменений.
 */
@Service
@RequiredArgsConstructor
public class TenderParticipantServiceImpl implements TenderParticipantService {

    private final TenderRepository tenderRepository;
    private final TenderParticipantRepository participantRepository;
    private final CurrentUserService currentUserService;
    private final TenderParticipantMapper mapper;
    private final TenderHistoryService historyService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public TenderParticipantListResponse list(Long tenderId, int page, int size) {
        if (!tenderRepository.existsById(tenderId)) {
            throw Errors.tenderNotFound();
        }

        var result = participantRepository.findByTenderIdOrderByCreatedAtAscIdAsc(tenderId, PageRequest.of(page, size));

        return new TenderParticipantListResponse().items(result.map(mapper::toResponse).getContent())
                .page(result.getNumber()).size(result.getSize())
                .totalElements(result.getTotalElements()).totalPages(result.getTotalPages());
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TenderParticipantResponse create(Long tenderId, SaveTenderParticipantRequest request) {
        TenderEntity tender = findTender(tenderId);
        assertCollecting(tender);
        TenderParticipantEntity participant = new TenderParticipantEntity();
        participant.setTenderId(tenderId);
        participant.setCreatedByUserId(currentUserService.getCurrentUserId());
        participant.setStatus(TenderParticipantStatus.PARTICIPATING);

        return save(tender, participant, request);
    }

    @Override
    @Retryable(interceptor = "retryInterceptor")
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TenderParticipantResponse update(Long tenderId, Long participantId, SaveTenderParticipantRequest request) {
        TenderEntity tender = findTender(tenderId);
        TenderParticipantEntity participant = participantRepository.findByIdAndTenderId(participantId, tenderId)
                .orElseThrow(Errors::tenderParticipantNotFound);
        assertCollecting(tender);

        return save(tender, participant, request);
    }

    private TenderEntity findTender(Long id) {
        return tenderRepository.findById(id).orElseThrow(Errors::tenderNotFound);
    }

    private void assertCollecting(TenderEntity tender) {
        if (tender.getStatus() != TenderStatus.COLLECTING_OFFERS) {
            throw Errors.tenderConflict("Предложения можно изменять только во время сбора");
        }
    }

    private TenderParticipantResponse save(TenderEntity tender, TenderParticipantEntity participant,
                                           SaveTenderParticipantRequest request) {
        String name = request.getName().trim();
        String proposal = request.getProposal().trim();
        if (name.isBlank() || proposal.isBlank()) {
            throw Errors.validationError("Укажите название участника и текст предложения");
        }

        var updatedAt = clock.instant();
        participant.setName(name);
        participant.setProposal(proposal);
        participant.setUpdatedAt(updatedAt);
        participantRepository.saveAndFlush(participant);
        tender.setUpdatedAt(updatedAt);
        historyService.record(tender, null);

        return mapper.toResponse(participant);
    }
}
