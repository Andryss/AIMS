import type { MonitoringAlert, MonitoringAlertListResponse, MonitoringAlertStatus } from '../types';
import { requestJson } from './http';

export function listMonitoringAlerts(
  token: string,
  page: number,
  size: number,
  status?: MonitoringAlertStatus,
): Promise<MonitoringAlertListResponse> {
  const params = new URLSearchParams({
    page: String(page),
    size: String(size),
  });
  if (status) {
    params.set('status', status);
  }
  return requestJson<MonitoringAlertListResponse>(
    `/monitoring/alerts?${params.toString()}`,
    { method: 'GET' },
    token,
  );
}

export function getMonitoringAlert(token: string, id: number): Promise<MonitoringAlert> {
  return requestJson<MonitoringAlert>(`/monitoring/alerts/${id}`, { method: 'GET' }, token);
}
