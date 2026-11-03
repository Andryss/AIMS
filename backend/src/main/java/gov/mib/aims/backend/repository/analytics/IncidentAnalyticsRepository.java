package gov.mib.aims.backend.repository.analytics;

import gov.mib.aims.backend.entity.IncidentEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Агрегаты инцидентов по историческим снимкам и переходам.
 */
public interface IncidentAnalyticsRepository extends Repository<IncidentEntity, Long> {

    String HISTORY = """
            WITH history AS (
                SELECT h.entity_id AS entity_id, h.changed_at AS at, h.id AS history_id, h.snapshot AS s,
                    lag(h.snapshot->>'status') OVER w AS previous_status,
                    row_number() OVER w AS revision
                FROM entity_history h
                WHERE h.entity_type = 'INCIDENT' AND h.changed_at < :to
                WINDOW w AS (PARTITION BY h.entity_id ORDER BY h.changed_at, h.id)
            ), latest AS (
                SELECT DISTINCT ON (entity_id) * FROM history
                ORDER BY entity_id, at DESC, history_id DESC
            ), selected AS (
                SELECT * FROM latest
                WHERE (CAST(:eventType AS text) IS NULL OR s->>'eventType' = :eventType)
                    AND (CAST(:alienId AS bigint) IS NULL OR CAST(s->>'alienId' AS bigint) = :alienId)
                    AND (CAST(:responsibleUserId AS bigint) IS NULL
                        OR CAST(s->>'responsibleUserId' AS bigint) = :responsibleUserId)
            ), events AS (
                SELECT h.*, (h.revision = 1 AND h.s->>'status' = 'DRAFT') AS created,
                    (h.previous_status IS NOT NULL AND h.previous_status <> 'EXECUTION_COMPLETED'
                        AND h.s->>'status' = 'EXECUTION_COMPLETED') AS completed
                FROM history h JOIN selected c USING (entity_id)
                WHERE h.at >= :from
            ), durations AS (
                SELECT extract(epoch FROM (e.at -
                    (CAST(b.s->>'createdAt' AS timestamp) AT TIME ZONE 'UTC'))) AS seconds
                FROM events e LEFT JOIN history b ON b.entity_id = e.entity_id
                    AND b.revision = 1 AND b.s->>'status' = 'DRAFT'
                WHERE e.completed
            ), stale AS (
                SELECT * FROM selected WHERE s->>'status' <> 'EXECUTION_COMPLETED'
                    AND at < CAST(:to AS timestamptz) - interval '72 hours'
            ), species AS (
                SELECT coalesce(s->>'alienId', 'UNKNOWN') AS key, max(a.name) AS name, count(*) AS count
                FROM selected LEFT JOIN alien a ON a.id = CAST(s->>'alienId' AS bigint)
                GROUP BY s->>'alienId'
            ), species_ranked AS (
                SELECT *, row_number() OVER (ORDER BY count DESC, key) AS rank
                FROM species WHERE key <> 'UNKNOWN'
            ), species_groups AS (
                SELECT key, name, count FROM species_ranked WHERE rank <= 10
                UNION ALL SELECT key, name, count FROM species WHERE key = 'UNKNOWN'
                UNION ALL SELECT 'OTHER', NULL, sum(count) FROM species_ranked WHERE rank > 10 HAVING count(*) > 0
            ), locations AS (
                SELECT s->>'location' AS key, count(*) AS count,
                    row_number() OVER (ORDER BY count(*) DESC, s->>'location') AS rank
                FROM selected GROUP BY s->>'location'
            ), location_groups AS (
                SELECT 'LOCATION:' || key AS key, key AS name, count FROM locations WHERE rank <= 10
                UNION ALL SELECT 'OTHER', NULL, sum(count) FROM locations WHERE rank > 10 HAVING count(*) > 0
            )
            """;

    String RESPONSE_START = """
            SELECT CAST(jsonb_build_object(
                'meta', jsonb_build_object('recordsWithoutSnapshot', (
                    SELECT count(*) FROM incident i
                    WHERE (i.created_at AT TIME ZONE 'UTC') < :to
                        AND NOT EXISTS (SELECT 1 FROM history h WHERE h.entity_id = i.id)
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
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Количество завершённых инцидентов за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE completed)
            ) AS text)
            """, nativeQuery = true)
    String completedCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Общее количество на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM selected)
            ) AS text)
            """, nativeQuery = true)
    String totalAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Количество завершённых инцидентов на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM selected WHERE s->>'status' = 'EXECUTION_COMPLETED')
            ) AS text)
            """, nativeQuery = true)
    String completedAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Количество открытых инцидентов на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM selected WHERE s->>'status' <> 'EXECUTION_COMPLETED')
            ) AS text)
            """, nativeQuery = true)
    String openAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Перцентили длительности завершённых инцидентов.
     */
    @Query(value = HISTORY + AnalyticsSql.PERCENTILES + RESPONSE_START + """
                , 'percentiles', (SELECT to_jsonb(p) FROM percentiles p)
            ) AS text)
            """, nativeQuery = true)
    String completionDuration(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Динамика событий с пустыми интервалами.
     */
    @Query(value = HISTORY + AnalyticsSql.BUCKETS + RESPONSE_START + """
                , 'buckets', (SELECT jsonb_agg(jsonb_build_object(
                    'from', start_at, 'to', end_at, 'values', jsonb_build_object(
                        'created', (SELECT count(*) FROM events WHERE created AND at >= start_at AND at < end_at),
                        'completed', (SELECT count(*) FROM events WHERE completed AND at >= start_at AND at < end_at)
                    )) ORDER BY start_at) FROM buckets)
            ) AS text)
            """, nativeQuery = true)
    String trend(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("bucketStart") Instant bucketStart, @Param("bucketMonths") int bucketMonths,
                  @Param("bucketSeconds") int bucketSeconds, @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId );

    /**
     * Распределение исторического среза по статусам.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', (SELECT jsonb_agg(jsonb_build_object('key', key, 'count', (
                    SELECT count(*) FROM selected WHERE s->>'status' = key
                ))) FROM (VALUES ('DRAFT'),
                    ('READY_FOR_ANALYSIS'),
                    ('READY_FOR_EXECUTION'),
                    ('CLARIFICATION_REQUIRED'),
                    ('PREPARATION_FOR_EXECUTION'),
                    ('PREPARED_FOR_EXECUTION'),
                    ('EXECUTING'),
                    ('EXECUTION_COMPLETED'),
                    ('REANALYSIS_REQUIRED')) codes(key))
            ) AS text)
            """, nativeQuery = true)
    String byStatus(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Распределение инцидентов по типам событий.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', (SELECT jsonb_agg(jsonb_build_object('key', key, 'count', (
                    SELECT count(*) FROM selected WHERE s->>'eventType' = key
                ))) FROM (VALUES ('UNIDENTIFIED_SIGHTING'),
                    ('CONTACT_SUSPECT'),
                    ('ILLEGAL_UFO_LANDING'),
                    ('MEMORY_ANOMALY'),
                    ('ALIEN_ARTIFACT'),
                    ('ALIEN_CAPTURE')) codes(key))
            ) AS text)
            """, nativeQuery = true)
    String byEventType(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

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
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Наиболее частые локации с группой остальных.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'groups', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'key', key, 'name', name, 'count', count) ORDER BY count DESC, key) FROM location_groups), '[]')
            ) AS text)
            """, nativeQuery = true)
    String byLocation(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);

    /**
     * Десять давно не изменявшихся инцидентов и их полное количество.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'items', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'incidentId', entity_id, 'location', s->>'location', 'status', s->>'status',
                    'responsibleUserId', CAST(s->>'responsibleUserId' AS bigint), 'lastChangedAt', at)
                    ORDER BY at, entity_id)
                    FROM (SELECT * FROM stale ORDER BY at, entity_id LIMIT 10) items), '[]')
                , 'totalCount', (SELECT count(*) FROM stale)
            ) AS text)
            """, nativeQuery = true)
    String stale(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("eventType") String eventType,
                  @Param("alienId") Long alienId, @Param("responsibleUserId") Long responsibleUserId);
}
