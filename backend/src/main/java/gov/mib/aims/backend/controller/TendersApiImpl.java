package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.TendersApi;
import gov.mib.aims.backend.generated.model.ChangeTenderStatusRequest;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.SaveTenderRequest;
import gov.mib.aims.backend.generated.model.SaveTenderParticipantRequest;
import gov.mib.aims.backend.generated.model.SelectTenderWinnerRequest;
import gov.mib.aims.backend.generated.model.TenderParticipantListResponse;
import gov.mib.aims.backend.generated.model.TenderParticipantResponse;
import gov.mib.aims.backend.generated.model.TenderHistoryListResponse;
import gov.mib.aims.backend.generated.model.TenderListResponse;
import gov.mib.aims.backend.generated.model.TenderResponse;
import gov.mib.aims.backend.generated.model.TenderStatusApi;
import gov.mib.aims.backend.services.TenderService;
import gov.mib.aims.backend.services.TenderParticipantService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * API тендеров на закупку оборудования.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class TendersApiImpl implements TendersApi {

    private final TenderService service;
    private final TenderParticipantService participantService;

    @Override
    @PreAuthorize("hasAuthority('TENDER_READ')")
    public TenderListResponse listTenders(String q, TenderStatusApi status, EquipmentTypeApi equipmentType,
                                           Long incidentId, Integer page, Integer size) {
        log.info("GET /api/v1/tenders");

        return service.list(q, status, equipmentType, incidentId, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_CREATE')")
    public TenderResponse createTender(SaveTenderRequest request) {
        log.info("POST /api/v1/tenders");

        return service.create(request);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_READ')")
    public TenderResponse getTender(Long id) {
        log.info("GET /api/v1/tenders/{}", id);

        return service.getById(id);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_EDIT')")
    public TenderResponse updateTender(Long id, SaveTenderRequest request) {
        log.info("PUT /api/v1/tenders/{}", id);

        return service.update(id, request);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('TENDER_EDIT', 'TENDER_MANAGE')")
    public TenderResponse changeTenderStatus(Long id, ChangeTenderStatusRequest request) {
        log.info("PATCH /api/v1/tenders/{}/status", id);

        return service.changeStatus(id, request);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_MANAGE')")
    public TenderResponse selectTenderWinner(Long id, SelectTenderWinnerRequest request) {
        log.info("POST /api/v1/tenders/{}/winner participantId={}", id, request.getParticipantId());

        return service.selectWinner(id, request);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_READ')")
    public TenderHistoryListResponse listTenderHistory(Long id, Integer page, Integer size) {
        log.info("GET /api/v1/tenders/{}/history", id);

        return service.listHistory(id, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_READ')")
    public TenderParticipantListResponse listTenderParticipants(Long id, Integer page, Integer size) {
        log.info("GET /api/v1/tenders/{}/participants page={} size={}", id, page, size);

        return participantService.list(id, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_MANAGE')")
    public TenderParticipantResponse createTenderParticipant(Long id, SaveTenderParticipantRequest request) {
        log.info("POST /api/v1/tenders/{}/participants", id);

        return participantService.create(id, request);
    }

    @Override
    @PreAuthorize("hasAuthority('TENDER_MANAGE')")
    public TenderParticipantResponse updateTenderParticipant(Long id, Long participantId,
                                                             SaveTenderParticipantRequest request) {
        log.info("PUT /api/v1/tenders/{}/participants/{}", id, participantId);

        return participantService.update(id, participantId, request);
    }
}
