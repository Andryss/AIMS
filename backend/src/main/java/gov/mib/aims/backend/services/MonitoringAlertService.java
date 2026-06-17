package gov.mib.aims.backend.services;

import gov.mib.aims.backend.generated.model.IngestMonitoringEventRequest;
import gov.mib.aims.backend.generated.model.MonitoringAlertListResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertResponse;
import gov.mib.aims.backend.generated.model.MonitoringAlertStatusApi;

/**
 * Сервис алертов внешней системы мониторинга.
 */
public interface MonitoringAlertService {

    /**
     * Принимает событие от внешней системы (идемпотентно по {@code externalEventId}).
     */
    MonitoringAlertResponse ingest(IngestMonitoringEventRequest request);

    /**
     * Возвращает страницу алертов с опциональным фильтром по статусу.
     */
    MonitoringAlertListResponse list(MonitoringAlertStatusApi status, int page, int size);

    /**
     * Возвращает алерт по идентификатору.
     */
    MonitoringAlertResponse getById(Long id);
}
