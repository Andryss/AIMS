package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.IntegrationApi;
import gov.mib.aims.backend.generated.model.IngestMonitoringEventRequest;
import gov.mib.aims.backend.generated.model.MonitoringAlertResponse;
import gov.mib.aims.backend.services.MonitoringAlertService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер inbound-интеграции с внешними системами.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class IntegrationApiImpl implements IntegrationApi {

    private final MonitoringAlertService monitoringAlertService;

    @Override
    @PreAuthorize("hasAuthority('MONITORING_EVENT_INGEST')")
    public MonitoringAlertResponse ingestMonitoringEvent(
            @Valid IngestMonitoringEventRequest ingestMonitoringEventRequest
    ) {
        log.info("POST /api/v1/integration/monitoring/events externalEventId={}",
                ingestMonitoringEventRequest.getExternalEventId());
        return monitoringAlertService.ingest(ingestMonitoringEventRequest);
    }
}
