package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.DetentionRequestsApi;
import gov.mib.aims.backend.generated.model.DetentionRequestHistoryListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestListResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.generated.model.SaveDetentionRequest;
import gov.mib.aims.backend.generated.model.ChangeDetentionRequestStatusRequest;
import gov.mib.aims.backend.services.DetentionRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * API заявок на заключение и их истории.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class DetentionRequestsApiImpl implements DetentionRequestsApi {

    private final DetentionRequestService service;

    @Override
    @PreAuthorize("hasAuthority('DETENTION_READ')")
    public DetentionRequestListResponse listDetentionRequests(String q, Long incidentId,
                                                              DetentionRequestStatusApi status,
                                                              Integer page, Integer size) {
        log.info("GET /api/v1/detention-requests");

        return service.list(q, incidentId, status, page, size);
    }

    @Override
    @PreAuthorize("hasAuthority('DETENTION_CREATE')")
    public DetentionRequestResponse createDetentionRequest(SaveDetentionRequest request) {
        log.info("POST /api/v1/detention-requests");

        return service.create(request);
    }

    @Override
    @PreAuthorize("hasAuthority('DETENTION_READ')")
    public DetentionRequestResponse getDetentionRequest(Long id) {
        log.info("GET /api/v1/detention-requests/{}", id);

        return service.getById(id);
    }

    @Override
    @PreAuthorize("hasAuthority('DETENTION_EDIT')")
    public DetentionRequestResponse updateDetentionRequest(Long id, SaveDetentionRequest request) {
        log.info("PUT /api/v1/detention-requests/{}", id);

        return service.update(id, request);
    }

    @Override
    @PreAuthorize("hasAnyAuthority('DETENTION_EDIT', 'DETENTION_PROCESS')")
    public DetentionRequestResponse changeDetentionRequestStatus(Long id, ChangeDetentionRequestStatusRequest request) {
        log.info("PATCH /api/v1/detention-requests/{}/status", id);

        return service.changeStatus(id, request);
    }

    @Override
    @PreAuthorize("hasAuthority('DETENTION_READ')")
    public DetentionRequestHistoryListResponse listDetentionRequestHistory(Long id, Integer page, Integer size) {
        log.info("GET /api/v1/detention-requests/{}/history", id);

        return service.listHistory(id, page, size);
    }
}
