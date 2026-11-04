package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentInventoryAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.ExpectedDeliveriesAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OpenEquipmentRequestsAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OutstandingEquipmentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.repository.analytics.EquipmentAnalyticsRepository;
import gov.mib.aims.backend.services.mapping.AnalyticsResponseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Историческая аналитика склада и движения оборудования.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class EquipmentAnalyticsService {

    private final EquipmentAnalyticsRepository repository;
    private final AnalyticsReportExecutor reports;
    private final AnalyticsResponseMapper responseMapper;

    /**
     * Количество поступивших экземпляров за период.
     */
    public CountAnalyticsResponse receivedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.receivedCount(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Количество выданных экземпляров за период.
     */
    public CountAnalyticsResponse issuedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.issuedCount(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Количество возвращённых экземпляров за период.
     */
    public CountAnalyticsResponse returnedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.returnedCount(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Общее количество на границе периода.
     */
    public CountAnalyticsResponse totalAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.totalAtTo(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Количество доступных экземпляров на границе периода.
     */
    public CountAnalyticsResponse availableAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.availableAtTo(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Количество выданных экземпляров на границе периода.
     */
    public CountAnalyticsResponse issuedAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.issuedAtTo(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Динамика событий с пустыми интервалами.
     */
    public TrendAnalyticsResponse trend(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toTrend(reports.execute(from, to,
                query -> repository.trend(query.from(), query.to(),
                        query.buckets().start(), query.buckets().months(), query.buckets().seconds(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Остатки оборудования по типам.
     */
    public EquipmentInventoryAnalyticsResponse inventoryByType(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toEquipmentInventory(reports.execute(from, to,
                query -> repository.inventoryByType(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Открытые заявки по типам оборудования и операциям.
     */
    public OpenEquipmentRequestsAnalyticsResponse openRequests(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toOpenEquipmentRequests(reports.execute(from, to,
                query -> repository.openRequests(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Ожидаемые поставки по типам оборудования.
     */
    public ExpectedDeliveriesAnalyticsResponse expectedDeliveriesByType(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toExpectedDeliveries(reports.execute(from, to,
                query -> repository.expectedDeliveriesByType(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }

    /**
     * Активные выдачи по инцидентам и ответственным.
     */
    public OutstandingEquipmentAnalyticsResponse outstandingByIncident(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        var filters = new EquipmentAnalyticsFilter(equipmentType, responsibleUserId, incidentId);

        return responseMapper.toOutstandingEquipment(reports.execute(from, to,
                query -> repository.outstandingByIncident(query.from(), query.to(),
                        filters.equipmentTypeName(), filters.responsibleUserId(), filters.incidentId())));
    }
}
