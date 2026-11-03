package gov.mib.aims.backend.controller;

import gov.mib.aims.backend.generated.api.DetentionStatisticsApi;
import gov.mib.aims.backend.generated.model.CountAnalyticsResponse;
import gov.mib.aims.backend.generated.model.DetentionRequestStatusApi;
import gov.mib.aims.backend.generated.model.DurationAnalyticsResponse;
import gov.mib.aims.backend.generated.model.GroupedAnalyticsResponse;
import gov.mib.aims.backend.generated.model.TrendAnalyticsResponse;
import gov.mib.aims.backend.generated.model.UpcomingReleasesAnalyticsResponse;
import gov.mib.aims.backend.services.analytics.DetentionAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

/**
 * Статистика заявок на заключение.
 */
@Slf4j
@Validated
@RestController
@RequiredArgsConstructor
public class DetentionStatisticsApiImpl implements DetentionStatisticsApi {

    private final DetentionAnalyticsService detentionService;

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public CountAnalyticsResponse getDetentionCreatedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/created-count from={} to={}",
                from, to);

        return detentionService.createdCount(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public CountAnalyticsResponse getDetentionAdmittedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/admitted-count from={} to={}",
                from, to);

        return detentionService.admittedCount(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public CountAnalyticsResponse getDetentionReleasedCount(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/released-count from={} to={}",
                from, to);

        return detentionService.releasedCount(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public CountAnalyticsResponse getDetentionAwaitingAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/awaiting-at-to from={} to={}",
                from, to);

        return detentionService.awaitingAtTo(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public CountAnalyticsResponse getDetentionInCustodyAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/in-custody-at-to from={} to={}",
                from, to);

        return detentionService.inCustodyAtTo(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public CountAnalyticsResponse getDetentionOverdueAtTo(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/overdue-at-to from={} to={}",
                from, to);

        return detentionService.overdueAtTo(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public DurationAnalyticsResponse getDetentionCustodyDuration(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/custody-duration from={} to={}",
                from, to);

        return detentionService.custodyDuration(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public TrendAnalyticsResponse getDetentionTrend(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/trend from={} to={}",
                from, to);

        return detentionService.trend(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public GroupedAnalyticsResponse getDetentionByStatus(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/by-status from={} to={}",
                from, to);

        return detentionService.byStatus(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public GroupedAnalyticsResponse getDetentionByAlien(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/by-alien from={} to={}",
                from, to);

        return detentionService.byAlien(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public GroupedAnalyticsResponse getDetentionByAssignedTerm(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/by-assigned-term from={} to={}",
                from, to);

        return detentionService.byAssignedTerm(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public GroupedAnalyticsResponse getDetentionByRemainingTime(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/by-remaining-time from={} to={}",
                from, to);

        return detentionService.byRemainingTime(from, to, alienId, status, incidentId);
    }

    @Override
    @PreAuthorize("hasAuthority('ANALYTICS_READ')")
    public UpcomingReleasesAnalyticsResponse getDetentionUpcomingReleases(OffsetDateTime from, OffsetDateTime to,
            Long alienId, DetentionRequestStatusApi status, Long incidentId) {
        log.info("GET /api/v1/detention-requests/statistics/upcoming-releases from={} to={}",
                from, to);

        return detentionService.upcomingReleases(from, to, alienId, status, incidentId);
    }
}
