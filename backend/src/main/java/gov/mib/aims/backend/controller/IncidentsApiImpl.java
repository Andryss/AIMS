package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.IncidentsApi;
import gov.mib.aims.backend.generated.model.ChangeIncidentStatusRequest;
import gov.mib.aims.backend.generated.model.CreateIncidentRequest;
import gov.mib.aims.backend.generated.model.IncidentListResponse;
import gov.mib.aims.backend.generated.model.IncidentResponse;
import gov.mib.aims.backend.generated.model.LinkIncidentAlienRequest;
import gov.mib.aims.backend.services.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер инцидентов.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class IncidentsApiImpl implements IncidentsApi {

    private final IncidentService incidentService;

    @Override
    @PreAuthorize("hasAuthority('INCIDENT_READ')")
    public IncidentListResponse listIncidents(Integer page, Integer size) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 20;
        log.info("GET /api/v1/incidents page={} size={}", pageNumber, pageSize);
        return incidentService.list(pageNumber, pageSize);
    }

    @Override
    @PreAuthorize("hasAuthority('INCIDENT_CREATE')")
    public IncidentResponse createIncident(@Valid CreateIncidentRequest createIncidentRequest) {
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
    public IncidentResponse changeIncidentStatus(
            Long id,
            @Valid ChangeIncidentStatusRequest changeIncidentStatusRequest
    ) {
        log.info("POST /api/v1/incidents/{}/status", id);
        return incidentService.changeStatus(id, changeIncidentStatusRequest);
    }

    @Override
    @PreAuthorize("hasAuthority('INCIDENT_ALIEN_LINK')")
    public IncidentResponse linkIncidentAlien(Long id, @Valid LinkIncidentAlienRequest linkIncidentAlienRequest) {
        log.info("PUT /api/v1/incidents/{}/alien", id);
        return incidentService.linkAlien(id, linkIncidentAlienRequest);
    }
}
