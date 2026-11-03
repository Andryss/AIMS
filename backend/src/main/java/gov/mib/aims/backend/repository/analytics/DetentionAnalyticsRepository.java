package gov.mib.aims.backend.repository.analytics;

import gov.mib.aims.backend.entity.DetentionRequestEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Агрегаты заявок, исторических сроков и фактического заключения.
 */
public interface DetentionAnalyticsRepository extends Repository<DetentionRequestEntity, Long> {

    String HISTORY = """
            WITH history AS (
                SELECT h.entity_id, h.changed_at AS at, h.id AS history_id, h.snapshot->'request' AS s,
                    lag(h.snapshot->'request'->>'status') OVER w AS previous_status,
                    row_number() OVER w AS revision
                FROM entity_history h
                WHERE h.entity_type = 'DETENTION_REQUEST' AND h.changed_at < :to
                WINDOW w AS (PARTITION BY h.entity_id ORDER BY h.changed_at, h.id)
            ), latest AS (
                SELECT DISTINCT ON (entity_id) * FROM history
                ORDER BY entity_id, at DESC, history_id DESC
            ), selected AS (
                SELECT *, CAST(s->>'admittedAt' AS timestamptz)
                    + make_interval(secs => CAST(s->>'durationDays' AS double precision) * 86400) AS deadline
                FROM latest
                WHERE (CAST(:status AS text) IS NULL OR s->>'status' = :status)
                    AND (CAST(:alienId AS bigint) IS NULL OR CAST(s->>'alienId' AS bigint) = :alienId)
                    AND (CAST(:incidentId AS bigint) IS NULL OR CAST(s->>'incidentId' AS bigint) = :incidentId)
            ), events AS (
                SELECT h.*, (h.revision = 1 AND h.s->>'status' = 'NEW') AS created,
                    (h.previous_status = 'ACCEPTED' AND h.s->>'status' = 'IN_CUSTODY') AS admitted,
                    (h.previous_status = 'IN_CUSTODY' AND h.s->>'status' = 'RELEASED') AS released
                FROM history h JOIN selected c USING (entity_id) WHERE h.at >= :from
            ), durations AS (
                SELECT extract(epoch FROM (CAST(s->>'releasedAt' AS timestamptz)
                    - CAST(s->>'admittedAt' AS timestamptz))) AS seconds
                FROM events WHERE released
            ), species AS (
                SELECT coalesce(s->>'alienId', 'UNKNOWN') AS key, max(a.name) AS name, count(*) AS count
                FROM selected LEFT JOIN alien a ON a.id = CAST(s->>'alienId' AS bigint)
                WHERE s->>'status' = 'IN_CUSTODY' GROUP BY s->>'alienId'
            ), species_ranked AS (
                SELECT *, row_number() OVER (ORDER BY count DESC, key) AS rank
                FROM species WHERE key <> 'UNKNOWN'
            ), species_groups AS (
                SELECT key, name, count FROM species_ranked WHERE rank <= 10
                UNION ALL SELECT key, name, count FROM species WHERE key = 'UNKNOWN'
                UNION ALL SELECT 'OTHER', NULL, sum(count) FROM species_ranked
                    WHERE rank > 10 HAVING count(*) > 0
            ), assigned_terms AS (
                SELECT CASE WHEN CAST(s->>'durationDays' AS integer) <= 30 THEN 'UP_TO_30'
                    WHEN CAST(s->>'durationDays' AS integer) <= 90 THEN 'FROM_31_TO_90'
                    ELSE 'OVER_90' END AS key
                FROM selected WHERE s->>'status' IN ('ACCEPTED', 'IN_CUSTODY', 'RELEASED')
            ), remaining_terms AS (
                SELECT CASE WHEN deadline <= :to THEN 'OVERDUE'
                    WHEN deadline <= CAST(:to AS timestamptz) + interval '168 hours' THEN 'UP_TO_7'
                    WHEN deadline <= CAST(:to AS timestamptz) + interval '720 hours' THEN 'FROM_7_TO_30'
                    ELSE 'OVER_30' END AS key
                FROM selected WHERE s->>'status' = 'IN_CUSTODY'
            ), upcoming AS (
                SELECT * FROM selected WHERE s->>'status' = 'IN_CUSTODY' AND deadline > :to
            )
            """;

    String RESPONSE_START = """
            SELECT CAST(jsonb_build_object(
                'meta', jsonb_build_object('recordsWithoutSnapshot', (
                    SELECT count(*) FROM detention_request r WHERE r.created_at < :to
                        AND NOT EXISTS (SELECT 1 FROM history h WHERE h.entity_id = r.id)
                ))
            """;

    /**
     * Количество созданных записей за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE created)
            ) AS text)
            """, nativeQuery = true)
    String createdCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Количество прибытий под стражу за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE admitted)
            ) AS text)
            """, nativeQuery = true)
    String admittedCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Количество освобождений за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE released)
            ) AS text)
            """, nativeQuery = true)
    String releasedCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Количество ожидающих прибытия на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM selected WHERE s->>'status' = 'ACCEPTED')
            ) AS text)
            """, nativeQuery = true)
    String awaitingAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Количество находящихся под стражей на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM selected WHERE s->>'status' = 'IN_CUSTODY')
            ) AS text)
            """, nativeQuery = true)
    String inCustodyAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Количество просроченных заключений на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM remaining_terms WHERE key = 'OVERDUE')
            ) AS text)
            """, nativeQuery = true)
    String overdueAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Перцентили фактической длительности заключения.
     */
    @Query(value = HISTORY + AnalyticsSql.PERCENTILES + RESPONSE_START + """
                , 'percentiles', (SELECT to_jsonb(p) FROM percentiles p)
            ) AS text)
            """, nativeQuery = true)
    String custodyDuration(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Динамика событий с пустыми интервалами.
     */
    @Query(value = HISTORY + AnalyticsSql.BUCKETS + RESPONSE_START + """
                , 'buckets', (SELECT jsonb_agg(jsonb_build_object(
                    'from', start_at, 'to', end_at, 'values', jsonb_build_object(
                        'created', (SELECT count(*) FROM events WHERE created AND at >= start_at AND at < end_at),
                        'admitted', (SELECT count(*) FROM events WHERE admitted AND at >= start_at AND at < end_at),
                        'released', (SELECT count(*) FROM events WHERE released AND at >= start_at AND at < end_at)
                    )) ORDER BY start_at) FROM buckets)
            ) AS text)
            """, nativeQuery = true)
    String trend(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("bucketStart") Instant bucketStart, @Param("bucketMonths") int bucketMonths,
                  @Param("bucketSeconds") int bucketSeconds, @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId );

    /**
     * Распределение исторического среза по статусам.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', (SELECT jsonb_agg(jsonb_build_object('key', key, 'count', (
                    SELECT count(*) FROM selected WHERE s->>'status' = key
                ))) FROM (VALUES ('NEW'), ('ACCEPTED'), ('IN_CUSTODY'),
                    ('RELEASED'), ('REJECTED'), ('CANCELLED')) codes(key))
            ) AS text)
            """, nativeQuery = true)
    String byStatus(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Распределение исторического среза по видам.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'key', key, 'name', name, 'count', count) ORDER BY count DESC, key)
                    FROM species_groups), '[]')
            ) AS text)
            """, nativeQuery = true)
    String byAlien(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Распределение по назначенному сроку заключения.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', (SELECT jsonb_agg(jsonb_build_object('key', key, 'count', (
                    SELECT count(*) FROM assigned_terms a WHERE a.key = codes.key
                ))) FROM (VALUES ('UP_TO_30'), ('FROM_31_TO_90'), ('OVER_90')) codes(key))
            ) AS text)
            """, nativeQuery = true)
    String byAssignedTerm(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Распределение по оставшемуся времени заключения.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', (SELECT jsonb_agg(jsonb_build_object('key', key, 'count', (
                    SELECT count(*) FROM remaining_terms r WHERE r.key = codes.key
                ))) FROM (VALUES ('OVERDUE'), ('UP_TO_7'), ('FROM_7_TO_30'), ('OVER_30')) codes(key))
            ) AS text)
            """, nativeQuery = true)
    String byRemainingTime(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);

    /**
     * Десять ближайших освобождений и их полное количество.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'items', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'requestId', entity_id, 'subjectName', s->>'subjectName',
                    'alienId', CAST(s->>'alienId' AS bigint), 'alienName', a.name,
                    'incidentId', CAST(s->>'incidentId' AS bigint), 'plannedReleaseAt', deadline)
                    ORDER BY deadline, entity_id)
                    FROM (SELECT * FROM upcoming ORDER BY deadline, entity_id LIMIT 10) items
                        LEFT JOIN alien a ON a.id = CAST(items.s->>'alienId' AS bigint)), '[]')
                , 'totalCount', (SELECT count(*) FROM upcoming)
            ) AS text)
            """, nativeQuery = true)
    String upcomingReleases(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("alienId") Long alienId,
                  @Param("status") String status, @Param("incidentId") Long incidentId);
}
