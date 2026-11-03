package gov.mib.aims.backend.services.tender;

import gov.mib.aims.backend.entity.TenderEntity;
import gov.mib.aims.backend.generated.model.TenderHistorySnapshot;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.model.EntityType;
import gov.mib.aims.backend.model.TenderParticipantStatus;
import gov.mib.aims.backend.repository.TenderParticipantRepository;
import gov.mib.aims.backend.repository.EquipmentRepository;
import gov.mib.aims.backend.services.EntityHistoryService;
import gov.mib.aims.backend.services.mapping.TenderMapper;
import gov.mib.aims.backend.services.mapping.TenderParticipantMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Единый снимок тендера и предложений в транзакции вызывающего сервиса.
 */
@Service
@RequiredArgsConstructor
public class TenderHistoryService {

    private final TenderParticipantRepository participantRepository;
    private final EquipmentRepository equipmentRepository;
    private final TenderParticipantMapper participantMapper;
    private final TenderMapper tenderMapper;
    private final EntityHistoryService historyService;

    /**
     * Записывает текущее состояние без ссылок на изменяемые JPA-сущности.
     */
    public TenderResponse record(TenderEntity tender, String comment) {
        var participants = participantRepository.findByTenderIdOrderByCreatedAtAscIdAsc(tender.getId());
        Long winner = participants.stream().filter(item -> item.getStatus() == TenderParticipantStatus.WINNER)
                .map(item -> item.getId()).findFirst().orElse(null);
        TenderResponse response = tenderMapper.toResponse(tender, winner);
        var snapshot = new TenderHistorySnapshot().tender(response)
                .participants(participants.stream().map(participantMapper::toResponse).toList())
                .receivedEquipmentIds(equipmentRepository.findIdsBySourceTenderId(tender.getId()))
                .transitionComment(comment == null ? null : comment.trim());
        historyService.recordChange(EntityType.TENDER, tender.getId(), snapshot);

        return response;
    }
}
