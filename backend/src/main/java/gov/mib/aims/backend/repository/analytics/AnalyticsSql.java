package gov.mib.aims.backend.repository.analytics;

/**
 * Общие SQL-части ограниченного графика и точных перцентилей.
 */
final class AnalyticsSql {

    private AnalyticsSql() {
    }

    static final String BUCKETS = """
            , bucket_config AS (
                SELECT make_interval(months => CAST(:bucketMonths AS integer),
                    secs => CAST(:bucketSeconds AS double precision)) AS step
            ), buckets AS (
                SELECT greatest(t AT TIME ZONE 'UTC', CAST(:from AS timestamptz)) AS start_at,
                    least((t + step) AT TIME ZONE 'UTC', CAST(:to AS timestamptz)) AS end_at
                FROM bucket_config,
                    generate_series(CAST(:bucketStart AS timestamptz) AT TIME ZONE 'UTC',
                        CAST(:to AS timestamptz) AT TIME ZONE 'UTC', step) t
                WHERE t AT TIME ZONE 'UTC' < :to
            )
            """;

    static final String PERCENTILES = """
            , percentiles AS (
                SELECT count(*) FILTER (WHERE seconds >= 0) AS "sampleCount",
                    count(*) FILTER (WHERE seconds IS NULL OR seconds < 0) AS "excludedSampleCount",
                    percentile_cont(0.5) WITHIN GROUP (ORDER BY seconds)
                        FILTER (WHERE seconds >= 0) AS "p50Seconds",
                    percentile_cont(0.9) WITHIN GROUP (ORDER BY seconds)
                        FILTER (WHERE seconds >= 0) AS "p90Seconds",
                    percentile_cont(0.95) WITHIN GROUP (ORDER BY seconds)
                        FILTER (WHERE seconds >= 0) AS "p95Seconds",
                    percentile_cont(0.99) WITHIN GROUP (ORDER BY seconds)
                        FILTER (WHERE seconds >= 0) AS "p99Seconds"
                FROM durations
            )
            """;
}
