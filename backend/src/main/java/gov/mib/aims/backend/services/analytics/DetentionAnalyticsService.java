package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.generated.model.UpcomingReleasesAnalyticsResponse;
import gov.mib.aims.backend.repository.analytics.DetentionAnalyticsRepository;
import gov.mib.aims.backend.services.mapping.AnalyticsResponseMapper;
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
    private final AnalyticsReportExecutor reports;
    private final AnalyticsResponseMapper responseMapper;

    /**
     * Количество созданных записей за период.
     */
    public CountAnalyticsResponse createdCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.createdCount(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Количество прибытий под стражу за период.
     */
    public CountAnalyticsResponse admittedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.admittedCount(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Количество освобождений за период.
     */
    public CountAnalyticsResponse releasedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.releasedCount(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Количество ожидающих прибытия на границе периода.
     */
    public CountAnalyticsResponse awaitingAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.awaitingAtTo(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Количество находящихся под стражей на границе периода.
     */
    public CountAnalyticsResponse inCustodyAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.inCustodyAtTo(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Количество просроченных заключений на границе периода.
     */
    public CountAnalyticsResponse overdueAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.overdueAtTo(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Перцентили фактической длительности заключения.
     */
    public DurationAnalyticsResponse custodyDuration(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toDuration(reports.execute(from, to,
                query -> repository.custodyDuration(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Динамика событий с пустыми интервалами.
     */
    public TrendAnalyticsResponse trend(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toTrend(reports.execute(from, to,
                query -> repository.trend(query.from(), query.to(),
                        query.buckets().start(), query.buckets().months(), query.buckets().seconds(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Распределение исторического среза по статусам.
     */
    public GroupedAnalyticsResponse byStatus(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byStatus(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Распределение исторического среза по видам.
     */
    public GroupedAnalyticsResponse byAlien(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byAlien(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Распределение по назначенному сроку заключения.
     */
    public GroupedAnalyticsResponse byAssignedTerm(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byAssignedTerm(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Распределение по оставшемуся времени заключения.
     */
    public GroupedAnalyticsResponse byRemainingTime(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byRemainingTime(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }

    /**
     * Десять ближайших освобождений и их полное количество.
     */
    public UpcomingReleasesAnalyticsResponse upcomingReleases(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        var filters = new DetentionAnalyticsFilter(alienId, status, incidentId);

        return responseMapper.toUpcomingReleases(reports.execute(from, to,
                query -> repository.upcomingReleases(query.from(), query.to(),
                        filters.alienId(), filters.statusName(), filters.incidentId())));
    }
}
