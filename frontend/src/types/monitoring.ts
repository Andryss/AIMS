import type { IncidentEventType } from './incidents';
import type { Page } from './common';

export type MonitoringAlertStatus = 'NEW' | 'INCIDENT_CREATED';

export interface MonitoringAlert {
  id: number;
  externalEventId: string;
  sourceSystem: string;
  status: MonitoringAlertStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  mediaUrls: string[];
  incidentId?: number | null;
  receivedAt: string;
  createdAt: string;
}

export type MonitoringAlertListResponse = Page<MonitoringAlert>;
