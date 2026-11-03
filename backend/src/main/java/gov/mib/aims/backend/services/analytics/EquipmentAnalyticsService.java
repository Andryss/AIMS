package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentInventoryAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentTypeApi;
import gov.mib.aims.backend.generated.model.ExpectedDeliveriesAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OpenEquipmentRequestsAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OutstandingEquipmentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.repository.analytics.EquipmentAnalyticsRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
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
    private final ObjectMapperWrapper mapper;
    private final ChronoService chronoService;

    /**
     * Количество поступивших экземпляров за период.
     */
    public CountAnalyticsResponse receivedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.receivedCount(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество выданных экземпляров за период.
     */
    public CountAnalyticsResponse issuedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.issuedCount(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество возвращённых экземпляров за период.
     */
    public CountAnalyticsResponse returnedCount(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.returnedCount(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Общее количество на границе периода.
     */
    public CountAnalyticsResponse totalAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.totalAtTo(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество доступных экземпляров на границе периода.
     */
    public CountAnalyticsResponse availableAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.availableAtTo(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество выданных экземпляров на границе периода.
     */
    public CountAnalyticsResponse issuedAtTo(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.issuedAtTo(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Динамика событий с пустыми интервалами.
     */
    public TrendAnalyticsResponse trend(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.trend(period.from(), period.to(),
                buckets.start(), buckets.months(), buckets.seconds(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, TrendAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Остатки оборудования по типам.
     */
    public EquipmentInventoryAnalyticsResponse inventoryByType(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.inventoryByType(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, EquipmentInventoryAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Открытые заявки по типам оборудования и операциям.
     */
    public OpenEquipmentRequestsAnalyticsResponse openRequests(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.openRequests(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, OpenEquipmentRequestsAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Ожидаемые поставки по типам оборудования.
     */
    public ExpectedDeliveriesAnalyticsResponse expectedDeliveriesByType(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.expectedDeliveriesByType(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, ExpectedDeliveriesAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Активные выдачи по инцидентам и ответственным.
     */
    public OutstandingEquipmentAnalyticsResponse outstandingByIncident(OffsetDateTime from, OffsetDateTime to,
            EquipmentTypeApi equipmentType, Long responsibleUserId, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.outstandingByIncident(period.from(), period.to(),
                equipmentType == null ? null : equipmentType.name(), responsibleUserId, incidentId);
        var response = mapper.readValue(json, OutstandingEquipmentAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }
}
