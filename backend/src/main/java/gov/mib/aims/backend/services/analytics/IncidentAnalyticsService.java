package gov.mib.aims.backend.services.analytics;

import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.StaleIncidentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.repository.analytics.IncidentAnalyticsRepository;
import gov.mib.aims.backend.services.mapping.AnalyticsResponseMapper;
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
    private final AnalyticsReportExecutor reports;
    private final AnalyticsResponseMapper responseMapper;

    /**
     * Количество созданных записей за период.
     */
    public CountAnalyticsResponse createdCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.createdCount(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Количество завершённых инцидентов за период.
     */
    public CountAnalyticsResponse completedCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.completedCount(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Общее количество на границе периода.
     */
    public CountAnalyticsResponse totalAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.totalAtTo(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Количество завершённых инцидентов на границе периода.
     */
    public CountAnalyticsResponse completedAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.completedAtTo(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Количество открытых инцидентов на границе периода.
     */
    public CountAnalyticsResponse openAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toCount(reports.execute(from, to,
                query -> repository.openAtTo(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Перцентили длительности завершённых инцидентов.
     */
    public DurationAnalyticsResponse completionDuration(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toDuration(reports.execute(from, to,
                query -> repository.completionDuration(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Динамика событий с пустыми интервалами.
     */
    public TrendAnalyticsResponse trend(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toTrend(reports.execute(from, to,
                query -> repository.trend(query.from(), query.to(),
                        query.buckets().start(), query.buckets().months(), query.buckets().seconds(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Распределение исторического среза по статусам.
     */
    public GroupedAnalyticsResponse byStatus(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byStatus(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Распределение инцидентов по типам событий.
     */
    public GroupedAnalyticsResponse byEventType(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byEventType(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Распределение инцидентов по принятому уровню угрозы.
     */
    public GroupedAnalyticsResponse byThreatLevel(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byThreatLevel(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Распределение инцидентов по количеству пострадавших.
     */
    public GroupedAnalyticsResponse byVictimCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byVictimCount(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Распределение исторического среза по видам.
     */
    public GroupedAnalyticsResponse byAlien(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byAlien(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Наиболее частые локации с группой остальных.
     */
    public GroupedAnalyticsResponse byLocation(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toGrouped(reports.execute(from, to,
                query -> repository.byLocation(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }

    /**
     * Десять давно не изменявшихся инцидентов и их полное количество.
     */
    public StaleIncidentAnalyticsResponse stale(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        var filters = new IncidentAnalyticsFilter(eventType, alienId, responsibleUserId);

        return responseMapper.toStaleIncident(reports.execute(from, to,
                query -> repository.stale(query.from(), query.to(),
                        filters.eventTypeName(), filters.alienId(), filters.responsibleUserId())));
    }
}
