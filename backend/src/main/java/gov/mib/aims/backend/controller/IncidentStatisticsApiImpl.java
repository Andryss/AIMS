package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.IncidentStatisticsApi;
import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.IncidentEventTypeApi;
import gov.mib.aims.backend.generated.model.StaleIncidentAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.services.analytics.IncidentAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Статистика инцидентов по историческим снимкам.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ANALYTICS_READ')")
public class IncidentStatisticsApiImpl implements IncidentStatisticsApi {

    private final IncidentAnalyticsService incidentService;

    @Override
    public CountAnalyticsResponse getIncidentCreatedCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/created-count from={} to={}",
                from, to);

        return incidentService.createdCount(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public CountAnalyticsResponse getIncidentCompletedCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/completed-count from={} to={}",
                from, to);

        return incidentService.completedCount(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public CountAnalyticsResponse getIncidentTotalAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/total-at-to from={} to={}",
                from, to);

        return incidentService.totalAtTo(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public CountAnalyticsResponse getIncidentCompletedAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/completed-at-to from={} to={}",
                from, to);

        return incidentService.completedAtTo(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public CountAnalyticsResponse getIncidentOpenAtTo(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/open-at-to from={} to={}",
                from, to);

        return incidentService.openAtTo(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public DurationAnalyticsResponse getIncidentCompletionDuration(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/completion-duration from={} to={}",
                from, to);

        return incidentService.completionDuration(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public TrendAnalyticsResponse getIncidentTrend(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/trend from={} to={}",
                from, to);

        return incidentService.trend(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public GroupedAnalyticsResponse getIncidentByStatus(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/by-status from={} to={}",
                from, to);

        return incidentService.byStatus(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public GroupedAnalyticsResponse getIncidentByEventType(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/by-event-type from={} to={}",
                from, to);

        return incidentService.byEventType(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public GroupedAnalyticsResponse getIncidentByThreatLevel(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/by-threat-level from={} to={}",
                from, to);

        return incidentService.byThreatLevel(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public GroupedAnalyticsResponse getIncidentByVictimCount(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/by-victim-count from={} to={}",
                from, to);

        return incidentService.byVictimCount(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public GroupedAnalyticsResponse getIncidentByAlien(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/by-alien from={} to={}",
                from, to);

        return incidentService.byAlien(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public GroupedAnalyticsResponse getIncidentByLocation(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/by-location from={} to={}",
                from, to);

        return incidentService.byLocation(from, to, eventType, alienId, responsibleUserId);
    }

    @Override
    public StaleIncidentAnalyticsResponse getIncidentStale(OffsetDateTime from, OffsetDateTime to,
            IncidentEventTypeApi eventType, Long alienId, Long responsibleUserId) {
        log.info("GET /api/v1/incidents/statistics/stale from={} to={}",
                from, to);

        return incidentService.stale(from, to, eventType, alienId, responsibleUserId);
    }
}
