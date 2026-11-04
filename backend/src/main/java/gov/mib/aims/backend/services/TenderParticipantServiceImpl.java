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
import gov.mib.aims.backend.services.query.PageParameters;
import gov.mib.aims.backend.services.tender.TenderHistoryService;
import gov.mib.aims.backend.services.transaction.RetryableSerializable;
import gov.mib.aims.backend.services.validation.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ChronoService chronoService;

    @Override
    @Transactional(readOnly = true)
    public TenderParticipantListResponse list(Long tenderId, int page, int size) {
        if (!tenderRepository.existsById(tenderId)) {
            throw Errors.tenderNotFound();
        }

        var result = participantRepository.findByTenderIdOrderByCreatedAtAscIdAsc(tenderId,
                PageParameters.of(page, size).toPageable());

        return mapper.toListResponse(result);
    }

    @Override
    @RetryableSerializable
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
    @RetryableSerializable
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
        String name = TextNormalizer.required(request.getName(), "Укажите название участника и текст предложения");
        String proposal = TextNormalizer.required(request.getProposal(),
                "Укажите название участника и текст предложения");

        var updatedAt = chronoService.now();
        participant.setName(name);
        participant.setProposal(proposal);
        participant.setUpdatedAt(updatedAt);
        participantRepository.saveAndFlush(participant);
        tender.setUpdatedAt(updatedAt);
        historyService.record(tender, null);

        return mapper.toResponse(participant);
    }
}
