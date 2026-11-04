package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.MonitoringApi;
import gov.mib.aims.backend.generated.model.MonitoringAlertListResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertStatusApi;
import gov.mib.aims.backend.services.MonitoringAlertService;
import gov.mib.aims.backend.services.query.PageParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер алертов внешнего мониторинга для операторов.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class MonitoringApiImpl implements MonitoringApi {

    private final MonitoringAlertService monitoringAlertService;

    @Override
    @PreAuthorize("hasAuthority('MONITORING_ALERT_READ')")
    public MonitoringAlertListResponse listMonitoringAlerts(
            MonitoringAlertStatusApi status,
            Integer page,
            Integer size
    ) {
        var pagination = PageParameters.of(page, size, 20);
        log.info("GET /api/v1/monitoring/alerts status={} page={} size={}",
                status, pagination.page(), pagination.size());
        return monitoringAlertService.list(status, pagination.page(), pagination.size());
    }

    @Override
    @PreAuthorize("hasAuthority('MONITORING_ALERT_READ')")
    public MonitoringAlertResponse getMonitoringAlert(Long id) {
        log.info("GET /api/v1/monitoring/alerts/{}", id);
        return monitoringAlertService.getById(id);
    }
}
