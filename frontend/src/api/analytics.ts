import type { CountAnalyticsResponse, DetentionAnalyticsQuery, DurationAnalyticsResponse, EquipmentAnalyticsQuery, EquipmentInventoryAnalyticsResponse, ExpectedDeliveriesAnalyticsResponse, GroupedAnalyticsResponse, IncidentAnalyticsQuery, OpenEquipmentRequestsAnalyticsResponse, OutstandingEquipmentAnalyticsResponse, StaleIncidentAnalyticsResponse, TrendAnalyticsResponse, UpcomingReleasesAnalyticsResponse } from '../types';
import { requestJson, queryString } from './http';

export function getIncidentCreatedCount(token: string, query: IncidentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/incidents/statistics/created-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentCompletedCount(token: string, query: IncidentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/incidents/statistics/completed-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentTotalAtTo(token: string, query: IncidentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/incidents/statistics/total-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentCompletedAtTo(token: string, query: IncidentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/incidents/statistics/completed-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentOpenAtTo(token: string, query: IncidentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/incidents/statistics/open-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentCompletionDuration(token: string, query: IncidentAnalyticsQuery): Promise<DurationAnalyticsResponse> {
  return requestJson<DurationAnalyticsResponse>(`/incidents/statistics/completion-duration?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentTrend(token: string, query: IncidentAnalyticsQuery): Promise<TrendAnalyticsResponse> {
  return requestJson<TrendAnalyticsResponse>(`/incidents/statistics/trend?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentByStatus(token: string, query: IncidentAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/incidents/statistics/by-status?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentByEventType(token: string, query: IncidentAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/incidents/statistics/by-event-type?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentByThreatLevel(token: string, query: IncidentAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/incidents/statistics/by-threat-level?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentByVictimCount(token: string, query: IncidentAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/incidents/statistics/by-victim-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentByAlien(token: string, query: IncidentAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/incidents/statistics/by-alien?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentByLocation(token: string, query: IncidentAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/incidents/statistics/by-location?${queryString(query)}`, { method: 'GET' }, token);
}

export function getIncidentStale(token: string, query: IncidentAnalyticsQuery): Promise<StaleIncidentAnalyticsResponse> {
  return requestJson<StaleIncidentAnalyticsResponse>(`/incidents/statistics/stale?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionCreatedCount(token: string, query: DetentionAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/detention-requests/statistics/created-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionAdmittedCount(token: string, query: DetentionAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/detention-requests/statistics/admitted-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionReleasedCount(token: string, query: DetentionAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/detention-requests/statistics/released-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionAwaitingAtTo(token: string, query: DetentionAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/detention-requests/statistics/awaiting-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionInCustodyAtTo(token: string, query: DetentionAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/detention-requests/statistics/in-custody-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionOverdueAtTo(token: string, query: DetentionAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/detention-requests/statistics/overdue-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionCustodyDuration(token: string, query: DetentionAnalyticsQuery): Promise<DurationAnalyticsResponse> {
  return requestJson<DurationAnalyticsResponse>(`/detention-requests/statistics/custody-duration?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionTrend(token: string, query: DetentionAnalyticsQuery): Promise<TrendAnalyticsResponse> {
  return requestJson<TrendAnalyticsResponse>(`/detention-requests/statistics/trend?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionByStatus(token: string, query: DetentionAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/detention-requests/statistics/by-status?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionByAlien(token: string, query: DetentionAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/detention-requests/statistics/by-alien?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionByAssignedTerm(token: string, query: DetentionAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/detention-requests/statistics/by-assigned-term?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionByRemainingTime(token: string, query: DetentionAnalyticsQuery): Promise<GroupedAnalyticsResponse> {
  return requestJson<GroupedAnalyticsResponse>(`/detention-requests/statistics/by-remaining-time?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionUpcomingReleases(token: string, query: DetentionAnalyticsQuery): Promise<UpcomingReleasesAnalyticsResponse> {
  return requestJson<UpcomingReleasesAnalyticsResponse>(`/detention-requests/statistics/upcoming-releases?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentReceivedCount(token: string, query: EquipmentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/equipment/statistics/received-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentIssuedCount(token: string, query: EquipmentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/equipment/statistics/issued-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentReturnedCount(token: string, query: EquipmentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/equipment/statistics/returned-count?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentTotalAtTo(token: string, query: EquipmentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/equipment/statistics/total-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentAvailableAtTo(token: string, query: EquipmentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/equipment/statistics/available-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentIssuedAtTo(token: string, query: EquipmentAnalyticsQuery): Promise<CountAnalyticsResponse> {
  return requestJson<CountAnalyticsResponse>(`/equipment/statistics/issued-at-to?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentTrend(token: string, query: EquipmentAnalyticsQuery): Promise<TrendAnalyticsResponse> {
  return requestJson<TrendAnalyticsResponse>(`/equipment/statistics/trend?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentInventoryByType(token: string, query: EquipmentAnalyticsQuery): Promise<EquipmentInventoryAnalyticsResponse> {
  return requestJson<EquipmentInventoryAnalyticsResponse>(`/equipment/statistics/inventory-by-type?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentOpenRequests(token: string, query: EquipmentAnalyticsQuery): Promise<OpenEquipmentRequestsAnalyticsResponse> {
  return requestJson<OpenEquipmentRequestsAnalyticsResponse>(`/equipment/statistics/open-requests?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentExpectedDeliveriesByType(token: string, query: EquipmentAnalyticsQuery): Promise<ExpectedDeliveriesAnalyticsResponse> {
  return requestJson<ExpectedDeliveriesAnalyticsResponse>(`/equipment/statistics/expected-deliveries-by-type?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipmentOutstandingByIncident(token: string, query: EquipmentAnalyticsQuery): Promise<OutstandingEquipmentAnalyticsResponse> {
  return requestJson<OutstandingEquipmentAnalyticsResponse>(`/equipment/statistics/outstanding-by-incident?${queryString(query)}`, { method: 'GET' }, token);
}
