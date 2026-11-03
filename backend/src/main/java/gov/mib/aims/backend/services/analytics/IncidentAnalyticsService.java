package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.StaleIncidentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.repository.analytics.IncidentAnalyticsRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Историческая аналитика инцидентов.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class IncidentAnalyticsService {

    private final IncidentAnalyticsRepository repository;
    private final ObjectMapperWrapper mapper;
    private final ChronoService chronoService;

    /**
     * Количество созданных записей за период.
     */
    public CountAnalyticsResponse createdCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.createdCount(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество завершённых инцидентов за период.
     */
    public CountAnalyticsResponse completedCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.completedCount(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Общее количество на границе периода.
     */
    public CountAnalyticsResponse totalAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.totalAtTo(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество завершённых инцидентов на границе периода.
     */
    public CountAnalyticsResponse completedAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.completedAtTo(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество открытых инцидентов на границе периода.
     */
    public CountAnalyticsResponse openAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.openAtTo(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Перцентили длительности завершённых инцидентов.
     */
    public DurationAnalyticsResponse completionDuration(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.completionDuration(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, DurationAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Динамика событий с пустыми интервалами.
     */
    public TrendAnalyticsResponse trend(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.trend(period.from(), period.to(),
                buckets.start(), buckets.months(), buckets.seconds(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, TrendAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение исторического среза по статусам.
     */
    public GroupedAnalyticsResponse byStatus(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byStatus(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение инцидентов по типам событий.
     */
    public GroupedAnalyticsResponse byEventType(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byEventType(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение исторического среза по видам.
     */
    public GroupedAnalyticsResponse byAlien(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byAlien(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Наиболее частые локации с группой остальных.
     */
    public GroupedAnalyticsResponse byLocation(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byLocation(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Десять давно не изменявшихся инцидентов и их полное количество.
     */
    public StaleIncidentAnalyticsResponse stale(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.stale(period.from(), period.to(),
                eventType == null ? null : eventType.name(), alienId, responsibleUserId);
        var response = mapper.readValue(json, StaleIncidentAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }
}
