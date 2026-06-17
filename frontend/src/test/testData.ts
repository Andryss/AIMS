import {
  Alien,
  AuthMeResponse,
  IncidentListResponse,
  IncidentResponse,
  MonitoringAlert,
  MonitoringAlertListResponse,
  NotificationListResponse,
} from '../types';

export const mockIncident = (overrides: Partial<IncidentResponse> = {}): IncidentResponse => ({
  id: 1,
  status: 'DRAFT',
  eventType: 'UNIDENTIFIED_SIGHTING',
  location: 'Area 51',
  detectedAt: '2025-06-01T12:00:00Z',
  description: 'Test incident',
  attachmentFileIds: [10],
  executorUserIds: [],
  createdAt: '2025-06-01T12:00:00Z',
  updatedAt: '2025-06-01T12:00:00Z',
  ...overrides,
});

export const mockIncidentList = (
  overrides: Partial<IncidentListResponse> = {},
): IncidentListResponse => ({
  items: [mockIncident()],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
  ...overrides,
});

export const mockProfile = (overrides: Partial<AuthMeResponse> = {}): AuthMeResponse => ({
  userId: 1,
  login: 'operator',
  roles: ['OPERATOR'],
  permissions: ['INCIDENT_READ', 'INCIDENT_CREATE', 'INCIDENT_STATUS_CHANGE'],
  ...overrides,
});

export const mockAlien = (overrides: Partial<Alien> = {}): Alien => ({
  id: 1,
  name: 'Слизень',
  description: 'Небольшое слизистое существо',
  threatLevel: 3,
  ...overrides,
});

export const mockNotificationList = (
  overrides: Partial<NotificationListResponse> = {},
): NotificationListResponse => ({
  items: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
  ...overrides,
});

export const mockMonitoringAlert = (
  overrides: Partial<MonitoringAlert> = {},
): MonitoringAlert => ({
  id: 1,
  externalEventId: 'evt-test-001',
  sourceSystem: 'EXTERNAL_MONITORING_V1',
  status: 'NEW',
  eventType: 'UNIDENTIFIED_SIGHTING',
  location: 'Nevada desert sector 7',
  detectedAt: '2025-06-01T12:00:00Z',
  description: 'Thermal anomaly detected by external sensors',
  mediaUrls: ['https://example.com/evidence/photo1.jpg'],
  incidentId: null,
  receivedAt: '2025-06-01T12:05:00Z',
  createdAt: '2025-06-01T12:05:00Z',
  ...overrides,
});

export const mockMonitoringAlertList = (
  overrides: Partial<MonitoringAlertListResponse> = {},
): MonitoringAlertListResponse => ({
  items: [mockMonitoringAlert()],
  page: 0,
  size: 12,
  totalElements: 1,
  totalPages: 1,
  ...overrides,
});
