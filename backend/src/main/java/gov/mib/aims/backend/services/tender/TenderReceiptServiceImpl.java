package gov.mib.aims.backend.services.tender;

import gov.mib.aims.backend.exception.Errors;
import gov.mib.aims.backend.generated.model.EquipmentHistoryEventApi;
import gov.mib.aims.backend.generated.model.ReceiveTenderRequest;
import gov.mib.aims.backend.generated.model.TenderReceiptResponse;
import gov.mib.aims.backend.model.TenderParticipantStatus;
import gov.mib.aims.backend.repository.TenderParticipantRepository;
import gov.mib.aims.backend.repository.TenderRepository;
import gov.mib.aims.backend.services.equipment.EquipmentRegistrationService;
import gov.mib.aims.backend.services.transaction.RetryableSerializable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Атомарная приёмка поставки с регистрацией экземпляров и обеих историй.
 */
@Service
@RequiredArgsConstructor
public class TenderReceiptServiceImpl implements TenderReceiptService {

    private final TenderRepository repository;
    private final TenderParticipantRepository participantRepository;
    private final EquipmentRegistrationService registrationService;
    private final TenderHistoryService historyService;
    private final TenderWorkflow workflow;

    @Override
    @RetryableSerializable
    public TenderReceiptResponse receive(Long id, ReceiveTenderRequest request) {
        var tender = repository.findById(id).orElseThrow(Errors::tenderNotFound);
        workflow.receive(tender);
        if (!participantRepository.existsByTenderIdAndStatus(id, TenderParticipantStatus.WINNER)) {
            throw Errors.tenderConflict("Для приёмки поставки необходимо выбрать победителя");
        }
        if (request.getEquipment().size() != tender.getQuantity()) {
            throw Errors.validationError("Количество оборудования должно совпадать с количеством в тендере");
        }
        if (request.getEquipment().stream().anyMatch(Objects::isNull)) {
            throw Errors.validationError("Укажите реквизиты каждого экземпляра оборудования");
        }

        var equipment = request.getEquipment().stream().map(item -> registrationService.register(
                tender.getEquipmentType(), item.getModel(), item.getSerialNumber(), id,
                EquipmentHistoryEventApi.RECEIVED)).toList();
        repository.saveAndFlush(tender);

        return new TenderReceiptResponse().tender(historyService.record(tender, request.getComment()))
                .equipment(equipment);
    }
}
