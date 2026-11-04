import * as api from '../api/client';
import { AnalyticsMeta, AnalyticsPeriodQuery, DurationPercentiles } from '../types';

export const analyticsPeriod = { from: '2025-05-01T00:00:00Z', to: '2025-05-02T00:00:00Z' };

export const analyticsMeta = (query: AnalyticsPeriodQuery = analyticsPeriod): AnalyticsMeta => ({
  ...query, generatedAt: query.to, bucketUnit: 'HOUR', bucketStep: 1, recordsWithoutSnapshot: 0,
});

export const emptyPercentiles = (): DurationPercentiles => ({ sampleCount: 0, excludedSampleCount: 0 });

export const analyticsPath = (dashboard = 'incidents') =>
  '/analytics?' + new URLSearchParams({ dashboard, ...analyticsPeriod }).toString();

/** Общие пустые ответы отдельных виджетов; каждый тест уточняет свою выборку. */
export function mockAnalyticsRequests() {
  [
    api.getIncidentCreatedCount,
    api.getIncidentCompletedCount,
    api.getIncidentTotalAtTo,
    api.getIncidentCompletedAtTo,
    api.getIncidentOpenAtTo,
    api.getEquipmentReceivedCount,
    api.getEquipmentIssuedCount,
    api.getEquipmentReturnedCount,
    api.getEquipmentTotalAtTo,
    api.getEquipmentAvailableAtTo,
    api.getEquipmentIssuedAtTo,
    api.getDetentionCreatedCount,
    api.getDetentionAdmittedCount,
    api.getDetentionReleasedCount,
    api.getDetentionAwaitingAtTo,
    api.getDetentionInCustodyAtTo,
    api.getDetentionOverdueAtTo,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), count: 3 }));
  });
  [
    api.getIncidentByStatus,
    api.getIncidentByEventType,
    api.getIncidentByThreatLevel,
    api.getIncidentByVictimCount,
    api.getIncidentByAlien,
    api.getIncidentByLocation,
    api.getDetentionByStatus,
    api.getDetentionByAlien,
    api.getDetentionByAssignedTerm,
    api.getDetentionByRemainingTime,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), groups: [{ key: 'UNKNOWN', count: 3 }] }));
  });
  [
    api.getIncidentTrend,
    api.getEquipmentTrend,
    api.getDetentionTrend,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), buckets: [{ from: query.from, to: query.to, values: { created: 0, completed: 0, received: 0, issued: 0, returned: 0, admitted: 0, released: 0 } }] }));
  });
  [
    api.getIncidentCompletionDuration,
    api.getDetentionCustodyDuration,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), percentiles: emptyPercentiles() }));
  });
  [
    api.getIncidentStale,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), totalCount: 0, items: [] }));
  });
  [
    api.getEquipmentInventoryByType,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), items: [{ equipmentType: 'NEURALYZER', available: 2, issued: 1, total: 3, issuedShare: 1 / 3 }, { equipmentType: 'NEUTRALIZER', available: 0, issued: 0, total: 0 }] }));
  });
  [
    api.getEquipmentOpenRequests,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), totalCount: 0, items: [] }));
  });
  [
    api.getEquipmentExpectedDeliveriesByType,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), items: [] }));
  });
  [
    api.getEquipmentOutstandingByIncident,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), totalCount: 0, items: [] }));
  });
  [
    api.getDetentionUpcomingReleases,
  ].forEach((load) => {
    (load as jest.Mock).mockImplementation((_: string, query: AnalyticsPeriodQuery) =>
      Promise.resolve({ meta: analyticsMeta(query), totalCount: 0, items: [] }));
  });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 3, login: 'agent' }] });
}
