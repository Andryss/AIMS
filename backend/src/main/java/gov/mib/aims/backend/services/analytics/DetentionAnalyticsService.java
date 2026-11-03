package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.generated.model.UpcomingReleasesAnalyticsResponse;
import gov.mib.aims.backend.repository.analytics.DetentionAnalyticsRepository;
import gov.mib.aims.backend.services.ChronoService;
import gov.mib.aims.backend.services.ObjectMapperWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

/**
 * Историческая аналитика заявок на заключение.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class DetentionAnalyticsService {

    private final DetentionAnalyticsRepository repository;
    private final ObjectMapperWrapper mapper;
    private final ChronoService chronoService;

    /**
     * Количество созданных записей за период.
     */
    public CountAnalyticsResponse createdCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.createdCount(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество прибытий под стражу за период.
     */
    public CountAnalyticsResponse admittedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.admittedCount(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество освобождений за период.
     */
    public CountAnalyticsResponse releasedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.releasedCount(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество ожидающих прибытия на границе периода.
     */
    public CountAnalyticsResponse awaitingAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.awaitingAtTo(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество находящихся под стражей на границе периода.
     */
    public CountAnalyticsResponse inCustodyAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.inCustodyAtTo(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Количество просроченных заключений на границе периода.
     */
    public CountAnalyticsResponse overdueAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.overdueAtTo(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, CountAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Перцентили фактической длительности заключения.
     */
    public DurationAnalyticsResponse custodyDuration(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.custodyDuration(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, DurationAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Динамика событий с пустыми интервалами.
     */
    public TrendAnalyticsResponse trend(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.trend(period.from(), period.to(),
                buckets.start(), buckets.months(), buckets.seconds(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, TrendAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение исторического среза по статусам.
     */
    public GroupedAnalyticsResponse byStatus(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byStatus(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение исторического среза по видам.
     */
    public GroupedAnalyticsResponse byAlien(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byAlien(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение по назначенному сроку заключения.
     */
    public GroupedAnalyticsResponse byAssignedTerm(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byAssignedTerm(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Распределение по оставшемуся времени заключения.
     */
    public GroupedAnalyticsResponse byRemainingTime(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.byRemainingTime(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, GroupedAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }

    /**
     * Десять ближайших освобождений и их полное количество.
     */
    public UpcomingReleasesAnalyticsResponse upcomingReleases(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        AnalyticsPeriod period = AnalyticsPeriod.of(from, to, chronoService);
        AnalyticsBuckets buckets = AnalyticsBuckets.forPeriod(period, chronoService);
        String json = repository.upcomingReleases(period.from(), period.to(),
                alienId, status == null ? null : status.name(), incidentId);
        var response = mapper.readValue(json, UpcomingReleasesAnalyticsResponse.class);

        return response.meta(period.meta(buckets, response.getMeta().getRecordsWithoutSnapshot(), chronoService));
    }
}
