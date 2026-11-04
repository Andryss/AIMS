package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.EquipmentStatisticsApi;
import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentInventoryAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.ExpectedDeliveriesAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OpenEquipmentRequestsAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OutstandingEquipmentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.services.analytics.EquipmentAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Статистика склада и движения оборудования.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ANALYTICS_READ')")
public class EquipmentStatisticsApiImpl implements EquipmentStatisticsApi {

    private final EquipmentAnalyticsService equipmentService;

    @Override
    public CountAnalyticsResponse getEquipmentReceivedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/received-count from={} to={}",
                from, to);

        return equipmentService.receivedCount(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public CountAnalyticsResponse getEquipmentIssuedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/issued-count from={} to={}",
                from, to);

        return equipmentService.issuedCount(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public CountAnalyticsResponse getEquipmentReturnedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/returned-count from={} to={}",
                from, to);

        return equipmentService.returnedCount(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public CountAnalyticsResponse getEquipmentTotalAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/total-at-to from={} to={}",
                from, to);

        return equipmentService.totalAtTo(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public CountAnalyticsResponse getEquipmentAvailableAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/available-at-to from={} to={}",
                from, to);

        return equipmentService.availableAtTo(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public CountAnalyticsResponse getEquipmentIssuedAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/issued-at-to from={} to={}",
                from, to);

        return equipmentService.issuedAtTo(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public TrendAnalyticsResponse getEquipmentTrend(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/trend from={} to={}",
                from, to);

        return equipmentService.trend(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public EquipmentInventoryAnalyticsResponse getEquipmentInventoryByType(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/inventory-by-type from={} to={}",
                from, to);

        return equipmentService.inventoryByType(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public OpenEquipmentRequestsAnalyticsResponse getEquipmentOpenRequests(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/open-requests from={} to={}",
                from, to);

        return equipmentService.openRequests(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public ExpectedDeliveriesAnalyticsResponse getEquipmentExpectedDeliveriesByType(OffsetDateTime from,
            OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/expected-deliveries-by-type from={} to={}",
                from, to);

        return equipmentService.expectedDeliveriesByType(from, to, equipmentType,
                responsibleUserId, incidentId);
    }

    @Override
    public OutstandingEquipmentAnalyticsResponse getEquipmentOutstandingByIncident(OffsetDateTime from,
            OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        log.info("GET /api/v1/equipment/statistics/outstanding-by-incident from={} to={}",
                from, to);

        return equipmentService.outstandingByIncident(from, to, equipmentType,
                responsibleUserId, incidentId);
    }
}
