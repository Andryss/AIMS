import type { IncidentEventType, IncidentStatus } from './incidents';
import type { EquipmentMovementOperation, EquipmentType } from './equipment';
import type { DetentionRequestStatus } from './detentions';

export interface AnalyticsPeriodQuery {
  from: string;
  to: string;
}

export interface IncidentAnalyticsQuery extends AnalyticsPeriodQuery {
  eventType?: IncidentEventType;
  alienId?: number;
  responsibleUserId?: number;
}

export interface EquipmentAnalyticsQuery extends AnalyticsPeriodQuery {
  equipmentType?: EquipmentType;
  responsibleUserId?: number;
  incidentId?: number;
}

export interface DetentionAnalyticsQuery extends AnalyticsPeriodQuery {
  alienId?: number;
  status?: DetentionRequestStatus;
  incidentId?: number;
}

export type AnalyticsBucketUnit = 'HOUR' | 'DAY' | 'MONTH';

export interface AnalyticsMeta {
  from: string;
  to: string;
  generatedAt: string;
  bucketUnit: AnalyticsBucketUnit;
  bucketStep: number;
  recordsWithoutSnapshot: number;
}

export interface DurationPercentiles {
  sampleCount: number;
  excludedSampleCount: number;
  p50Seconds?: number | null;
  p90Seconds?: number | null;
  p95Seconds?: number | null;
  p99Seconds?: number | null;
}

export interface AnalyticsCountGroup {
  key: string;
  count: number;
  name?: string;
}

export interface AnalyticsTrendBucket {
  from: string;
  to: string;
  values: Record<string, number>;
}

export interface StaleIncidentAnalyticsRow {
  incidentId: number;
  location: string;
  status: IncidentStatus;
  responsibleUserId?: number;
  lastChangedAt: string;
}

export interface EquipmentInventoryAnalyticsRow {
  equipmentType: EquipmentType;
  available: number;
  issued: number;
  total: number;
  issuedShare?: number | null;
}

export interface OpenEquipmentRequestAnalyticsRow {
  equipmentType: EquipmentType;
  operation: EquipmentMovementOperation;
  requestCount: number;
  unitCount: number;
}

export interface ExpectedDeliveryAnalyticsRow {
  equipmentType: EquipmentType;
  tenderCount: number;
  unitCount: number;
}

export interface OutstandingEquipmentAnalyticsRow {
  incidentId: number;
  location: string;
  responsibleUserId: number;
  unitsByType: Record<string, number>;
}

export interface UpcomingReleaseAnalyticsRow {
  requestId: number;
  subjectName: string;
  alienId?: number;
  alienName?: string;
  incidentId: number;
  plannedReleaseAt: string;
}

export interface CountAnalyticsResponse {
  meta: AnalyticsMeta;
  count: number;
}

export interface GroupedAnalyticsResponse {
  meta: AnalyticsMeta;
  groups: AnalyticsCountGroup[];
}

export interface TrendAnalyticsResponse {
  meta: AnalyticsMeta;
  buckets: AnalyticsTrendBucket[];
}

export interface DurationAnalyticsResponse {
  meta: AnalyticsMeta;
  percentiles: DurationPercentiles;
}

export interface StaleIncidentAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: StaleIncidentAnalyticsRow[];
}

export interface EquipmentInventoryAnalyticsResponse {
  meta: AnalyticsMeta;
  items: EquipmentInventoryAnalyticsRow[];
}

export interface OpenEquipmentRequestsAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: OpenEquipmentRequestAnalyticsRow[];
}

export interface ExpectedDeliveriesAnalyticsResponse {
  meta: AnalyticsMeta;
  items: ExpectedDeliveryAnalyticsRow[];
}

export interface OutstandingEquipmentAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: OutstandingEquipmentAnalyticsRow[];
}

export interface UpcomingReleasesAnalyticsResponse {
  meta: AnalyticsMeta;
  totalCount: number;
  items: UpcomingReleaseAnalyticsRow[];
}
