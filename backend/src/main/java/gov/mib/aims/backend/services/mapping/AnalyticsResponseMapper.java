package gov.mib.aims.backend.services.mapping;

import gov.mib.aims.backend.generated.model.AnalyticsBucketUnitApi;
import gov.mib.aims.backend.generated.model.AnalyticsMeta;
import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.EquipmentInventoryAnalyticsResponse;
import gov.mib.aims.backend.generated.model.ExpectedDeliveriesAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OpenEquipmentRequestsAnalyticsResponse;
import gov.mib.aims.backend.generated.model.OutstandingEquipmentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.StaleIncidentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.generated.model.UpcomingReleasesAnalyticsResponse;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import gov.mib.aims.backend.services.analytics.AnalyticsBuckets;
import gov.mib.aims.backend.services.analytics.AnalyticsQueryContext;
import gov.mib.aims.backend.services.analytics.AnalyticsReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Преобразование результатов аналитического SQL в типизированные ответы API.
 */
@Component
@RequiredArgsConstructor
public class AnalyticsResponseMapper {

    private final ObjectMapperWrapper mapper;
    private final ChronoService chronoService;

    /**
     * Собирает представление количества, сохраняя данные и полноту истории.
     */
    public CountAnalyticsResponse toCount(AnalyticsReport report) {
        return map(report, CountAnalyticsResponse.class,
                CountAnalyticsResponse::getMeta, CountAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление групп, сохраняя данные и полноту истории.
     */
    public GroupedAnalyticsResponse toGrouped(AnalyticsReport report) {
        return map(report, GroupedAnalyticsResponse.class,
                GroupedAnalyticsResponse::getMeta, GroupedAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление динамики, сохраняя данные и полноту истории.
     */
    public TrendAnalyticsResponse toTrend(AnalyticsReport report) {
        return map(report, TrendAnalyticsResponse.class,
                TrendAnalyticsResponse::getMeta, TrendAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление длительности, сохраняя данные и полноту истории.
     */
    public DurationAnalyticsResponse toDuration(AnalyticsReport report) {
        return map(report, DurationAnalyticsResponse.class,
                DurationAnalyticsResponse::getMeta, DurationAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление давно не изменявшихся инцидентов, сохраняя данные и полноту истории.
     */
    public StaleIncidentAnalyticsResponse toStaleIncident(AnalyticsReport report) {
        return map(report, StaleIncidentAnalyticsResponse.class,
                StaleIncidentAnalyticsResponse::getMeta, StaleIncidentAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление остатков оборудования, сохраняя данные и полноту истории.
     */
    public EquipmentInventoryAnalyticsResponse toEquipmentInventory(AnalyticsReport report) {
        return map(report, EquipmentInventoryAnalyticsResponse.class,
                EquipmentInventoryAnalyticsResponse::getMeta, EquipmentInventoryAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление открытых заявок на оборудование, сохраняя данные и полноту истории.
     */
    public OpenEquipmentRequestsAnalyticsResponse toOpenEquipmentRequests(AnalyticsReport report) {
        return map(report, OpenEquipmentRequestsAnalyticsResponse.class,
                OpenEquipmentRequestsAnalyticsResponse::getMeta, OpenEquipmentRequestsAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление ожидаемых поставок, сохраняя данные и полноту истории.
     */
    public ExpectedDeliveriesAnalyticsResponse toExpectedDeliveries(AnalyticsReport report) {
        return map(report, ExpectedDeliveriesAnalyticsResponse.class,
                ExpectedDeliveriesAnalyticsResponse::getMeta, ExpectedDeliveriesAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление активных выдач оборудования, сохраняя данные и полноту истории.
     */
    public OutstandingEquipmentAnalyticsResponse toOutstandingEquipment(AnalyticsReport report) {
        return map(report, OutstandingEquipmentAnalyticsResponse.class,
                OutstandingEquipmentAnalyticsResponse::getMeta, OutstandingEquipmentAnalyticsResponse::setMeta);
    }

    /**
     * Собирает представление ближайших освобождений, сохраняя данные и полноту истории.
     */
    public UpcomingReleasesAnalyticsResponse toUpcomingReleases(AnalyticsReport report) {
        return map(report, UpcomingReleasesAnalyticsResponse.class,
                UpcomingReleasesAnalyticsResponse::getMeta, UpcomingReleasesAnalyticsResponse::setMeta);
    }

    private <T> T map(AnalyticsReport report, Class<T> responseType,
            Function<T, AnalyticsMeta> getMeta, BiConsumer<T, AnalyticsMeta> setMeta) {
        T response = mapper.readValue(report.json(), responseType);
        Long recordsWithoutSnapshot = getMeta.apply(response).getRecordsWithoutSnapshot();
        setMeta.accept(response, toMeta(report.context(), recordsWithoutSnapshot));

        return response;
    }

    private AnalyticsMeta toMeta(AnalyticsQueryContext context, Long recordsWithoutSnapshot) {
        return new AnalyticsMeta().from(chronoService.toOffsetDateTime(context.from()))
                .to(chronoService.toOffsetDateTime(context.to())).generatedAt(chronoService.offsetDateTimeNow())
                .bucketUnit(toBucketUnit(context.buckets().unit())).bucketStep(context.buckets().step())
                .recordsWithoutSnapshot(recordsWithoutSnapshot);
    }

    private AnalyticsBucketUnitApi toBucketUnit(AnalyticsBuckets.Unit unit) {
        return switch (unit) {
            case HOUR -> AnalyticsBucketUnitApi.HOUR;
            case DAY -> AnalyticsBucketUnitApi.DAY;
            case MONTH -> AnalyticsBucketUnitApi.MONTH;
        };
    }
}
