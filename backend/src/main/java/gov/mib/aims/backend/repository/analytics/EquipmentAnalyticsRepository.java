package gov.mib.aims.backend.repository.analytics;

import gov.mib.aims.backend.entity.EquipmentEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

/**
 * Исторические агрегаты склада и связанных заявок без чтения современных остатков.
 */
public interface EquipmentAnalyticsRepository extends Repository<EquipmentEntity, Long> {

    String HISTORY = """
            WITH history AS (
                SELECT h.entity_type, h.entity_id, h.changed_at AS at, h.id AS history_id,
                    CASE h.entity_type WHEN 'EQUIPMENT' THEN h.snapshot->'equipment'
                        WHEN 'EQUIPMENT_MOVEMENT' THEN h.snapshot->'movement'
                        WHEN 'TENDER' THEN h.snapshot->'tender' ELSE h.snapshot END AS s,
                    h.snapshot->>'event' AS event, CAST(h.snapshot->>'movementId' AS bigint) AS movement_id
                FROM entity_history h WHERE h.changed_at < :to
                    AND h.entity_type IN ('EQUIPMENT', 'EQUIPMENT_MOVEMENT', 'TENDER', 'INCIDENT')
            ), latest AS (
                SELECT DISTINCT ON (entity_type, entity_id) * FROM history
                ORDER BY entity_type, entity_id, at DESC, history_id DESC
            ), stock AS (
                SELECT * FROM latest WHERE entity_type = 'EQUIPMENT'
                    AND (CAST(:equipmentType AS text) IS NULL OR s->>'type' = :equipmentType)
            ), event_candidates AS (
                SELECT DISTINCT ON (entity_id,
                    CASE WHEN event IN ('REGISTERED', 'RECEIVED') THEN 'RECEIVED' ELSE event END,
                    coalesce(movement_id, 0)) *
                FROM history WHERE entity_type = 'EQUIPMENT'
                    AND event IN ('REGISTERED', 'RECEIVED', 'ISSUED', 'RETURNED')
                ORDER BY entity_id,
                    CASE WHEN event IN ('REGISTERED', 'RECEIVED') THEN 'RECEIVED' ELSE event END,
                    coalesce(movement_id, 0), at, history_id
            ), events AS (
                SELECT e.* FROM event_candidates e
                LEFT JOIN LATERAL (
                    SELECT m.snapshot->'movement' AS s FROM entity_history m
                    WHERE m.entity_type = 'EQUIPMENT_MOVEMENT'
                        AND m.entity_id = e.movement_id AND m.changed_at <= e.at
                    ORDER BY m.changed_at DESC, m.id DESC LIMIT 1
                ) m ON true
                WHERE e.at >= :from
                    AND (CAST(:equipmentType AS text) IS NULL OR e.s->>'type' = :equipmentType)
                    AND (CAST(:responsibleUserId AS bigint) IS NULL
                        OR CAST(m.s->>'responsibleUserId' AS bigint) = :responsibleUserId)
                    AND (CAST(:incidentId AS bigint) IS NULL OR CAST(m.s->>'incidentId' AS bigint) = :incidentId)
            ), open_movements AS (
                SELECT * FROM latest WHERE entity_type = 'EQUIPMENT_MOVEMENT'
                    AND s->>'status' IN ('PENDING', 'PROCESSING')
                    AND (CAST(:responsibleUserId AS bigint) IS NULL
                        OR CAST(s->>'responsibleUserId' AS bigint) = :responsibleUserId)
                    AND (CAST(:incidentId AS bigint) IS NULL OR CAST(s->>'incidentId' AS bigint) = :incidentId)
            ), request_items AS (
                SELECT m.entity_id, m.s->>'operation' AS operation, e.s->>'type' AS type
                FROM open_movements m CROSS JOIN LATERAL jsonb_array_elements(m.s->'equipment') item
                    JOIN stock e ON e.entity_id = CAST(item->>'id' AS bigint)
            ), request_groups AS (
                SELECT type, operation, count(DISTINCT entity_id) AS request_count, count(*) AS units
                FROM request_items GROUP BY type, operation
            ), deliveries AS (
                SELECT s FROM latest WHERE entity_type = 'TENDER' AND s->>'status' = 'AWAITING_DELIVERY'
                    AND (CAST(:equipmentType AS text) IS NULL OR s->>'equipmentType' = :equipmentType)
            ), outstanding_types AS (
                SELECT CAST(m.s->>'incidentId' AS bigint) AS incident_id,
                    CAST(m.s->>'responsibleUserId' AS bigint) AS responsible_id,
                    coalesce(i.s->>'location', 'Инцидент #' || (m.s->>'incidentId')) AS location,
                    e.s->>'type' AS type, count(*) AS units
                FROM stock e JOIN latest m ON m.entity_type = 'EQUIPMENT_MOVEMENT'
                    AND m.entity_id = CAST(e.s->>'currentIssueMovementId' AS bigint)
                LEFT JOIN latest i ON i.entity_type = 'INCIDENT'
                    AND i.entity_id = CAST(m.s->>'incidentId' AS bigint)
                WHERE e.s->>'status' = 'ISSUED'
                    AND (CAST(:responsibleUserId AS bigint) IS NULL
                        OR CAST(m.s->>'responsibleUserId' AS bigint) = :responsibleUserId)
                    AND (CAST(:incidentId AS bigint) IS NULL OR CAST(m.s->>'incidentId' AS bigint) = :incidentId)
                GROUP BY m.s->>'incidentId', m.s->>'responsibleUserId', i.s->>'location', e.s->>'type'
            ), outstanding AS (
                SELECT incident_id, responsible_id, max(location) AS location,
                    jsonb_object_agg(type, units) AS units_by_type, sum(units) AS units
                FROM outstanding_types GROUP BY incident_id, responsible_id
            ), inventory AS (
                SELECT key AS type, count(entity_id) AS total,
                    count(entity_id) FILTER (WHERE s->>'status' = 'AVAILABLE') AS available,
                    count(entity_id) FILTER (WHERE s->>'status' = 'ISSUED') AS issued
                FROM (VALUES ('NEURALYZER'), ('NEUTRALIZER')) codes(key)
                    LEFT JOIN stock ON stock.s->>'type' = key
                WHERE CAST(:equipmentType AS text) IS NULL OR key = :equipmentType
                GROUP BY key
            )
            """;

    String RESPONSE_START = """
            SELECT CAST(jsonb_build_object(
                'meta', jsonb_build_object('recordsWithoutSnapshot', (
                    SELECT sum(n) FROM (
                        SELECT count(*) AS n FROM equipment_item e WHERE e.created_at < :to
                            AND NOT EXISTS (SELECT 1 FROM history h
                                WHERE h.entity_type = 'EQUIPMENT' AND h.entity_id = e.id)
                        UNION ALL
                        SELECT count(*) FROM equipment_movement m WHERE m.created_at < :to
                            AND NOT EXISTS (SELECT 1 FROM history h
                                WHERE h.entity_type = 'EQUIPMENT_MOVEMENT' AND h.entity_id = m.id)
                        UNION ALL
                        SELECT count(*) FROM tender t WHERE t.created_at < :to
                            AND NOT EXISTS (SELECT 1 FROM history h
                                WHERE h.entity_type = 'TENDER' AND h.entity_id = t.id)
                    ) missing
                ))
            """;

    /**
     * Количество поступивших экземпляров за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE event IN ('REGISTERED', 'RECEIVED'))
            ) AS text)
            """, nativeQuery = true)
    String receivedCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Количество выданных экземпляров за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE event = 'ISSUED')
            ) AS text)
            """, nativeQuery = true)
    String issuedCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Количество возвращённых экземпляров за период.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM events WHERE event = 'RETURNED')
            ) AS text)
            """, nativeQuery = true)
    String returnedCount(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Общее количество на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM stock)
            ) AS text)
            """, nativeQuery = true)
    String totalAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Количество доступных экземпляров на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM stock WHERE s->>'status' = 'AVAILABLE')
            ) AS text)
            """, nativeQuery = true)
    String availableAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Количество выданных экземпляров на границе периода.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'count', (SELECT count(*) FROM stock WHERE s->>'status' = 'ISSUED')
            ) AS text)
            """, nativeQuery = true)
    String issuedAtTo(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Динамика событий с пустыми интервалами.
     */
    @Query(value = HISTORY + AnalyticsSql.BUCKETS + RESPONSE_START + """
                , 'buckets', (SELECT jsonb_agg(jsonb_build_object(
                    'from', start_at, 'to', end_at, 'values', jsonb_build_object(
                        'received', (SELECT count(*) FROM events WHERE event IN ('REGISTERED', 'RECEIVED')
                            AND at >= start_at AND at < end_at),
                        'issued', (SELECT count(*) FROM events WHERE event = 'ISSUED'
                            AND at >= start_at AND at < end_at),
                        'returned', (SELECT count(*) FROM events WHERE event = 'RETURNED'
                            AND at >= start_at AND at < end_at)
                    )) ORDER BY start_at) FROM buckets)
            ) AS text)
            """, nativeQuery = true)
    String trend(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("bucketStart") Instant bucketStart, @Param("bucketMonths") int bucketMonths,
                  @Param("bucketSeconds") int bucketSeconds, @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId );

    /**
     * Остатки оборудования по типам.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'items', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'equipmentType', type, 'available', available, 'issued', issued, 'total', total,
                    'issuedShare', CAST(issued AS double precision) / nullif(total, 0)) ORDER BY type)
                    FROM inventory), '[]')
            ) AS text)
            """, nativeQuery = true)
    String inventoryByType(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Открытые заявки по типам оборудования и операциям.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'items', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'equipmentType', type, 'operation', operation, 'requestCount', request_count, 'unitCount', units)
                    ORDER BY type, operation) FROM request_groups), '[]')
                , 'totalCount', (SELECT count(DISTINCT entity_id) FROM request_items)
            ) AS text)
            """, nativeQuery = true)
    String openRequests(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Ожидаемые поставки по типам оборудования.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'items', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'equipmentType', type, 'tenderCount', n, 'unitCount', units) ORDER BY type)
                    FROM (SELECT s->>'equipmentType' AS type, count(*) AS n,
                        sum(CAST(s->>'quantity' AS bigint)) AS units FROM deliveries
                        GROUP BY s->>'equipmentType') groups), '[]')
            ) AS text)
            """, nativeQuery = true)
    String expectedDeliveriesByType(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);

    /**
     * Активные выдачи по инцидентам и ответственным.
     */
    @Query(value = HISTORY + RESPONSE_START + """
                , 'items', coalesce((SELECT jsonb_agg(jsonb_build_object(
                    'incidentId', incident_id, 'location', location, 'responsibleUserId', responsible_id,
                    'unitsByType', units_by_type) ORDER BY units DESC, incident_id, responsible_id)
                    FROM (SELECT * FROM outstanding
                        ORDER BY units DESC, incident_id, responsible_id LIMIT 10) groups), '[]')
                , 'totalCount', (SELECT count(*) FROM outstanding)
            ) AS text)
            """, nativeQuery = true)
    String outstandingByIncident(@Param("from") Instant from, @Param("to") Instant to,
                  @Param("equipmentType") String equipmentType,
                  @Param("responsibleUserId") Long responsibleUserId, @Param("incidentId") Long incidentId);
}
