package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.IncidentsApi;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.services.IncidentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер инцидентов.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class IncidentsApiImpl implements IncidentsApi {

    private final IncidentService incidentService;

    @Override
    @PreAuthorize("hasAuthority('INCIDENT_CREATE')")
    public IncidentResponse createIncident(CreateIncidentRequest createIncidentRequest) {
        log.info("POST /api/v1/incidents");
        return incidentService.create(createIncidentRequest);
    }

    @Override
    @PreAuthorize("hasAuthority('INCIDENT_READ')")
    public IncidentResponse getIncident(Long id) {
        log.info("GET /api/v1/incidents/{}", id);
        return incidentService.getById(id);
    }

    @Override
    @PreAuthorize("hasAuthority('INCIDENT_STATUS_CHANGE')")
    public IncidentResponse changeIncidentStatus(Long id, ChangeIncidentStatusRequest changeIncidentStatusRequest) {
        log.info("POST /api/v1/incidents/{}/status", id);
        return incidentService.changeStatus(id, changeIncidentStatusRequest);
    }
}
