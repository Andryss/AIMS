import {
  DetentionRequestResponse, TenderResponse, TenderParticipantResponse } from '../types';
import {
  Alien,
  EquipmentResponse,
  EquipmentMovementResponse,
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

export const mockEquipment = (overrides: Partial<EquipmentResponse> = {}): EquipmentResponse => ({
  id: 1,
  type: 'NEURALYZER',
  model: 'Нейралайзер Н-1',
  serialNumber: 'SN-001',
  status: 'AVAILABLE',
  holderUserId: null,
  currentIssueMovementId: null,
  createdAt: '2026-11-02T12:00:00Z',
  ...overrides,
});

export const mockEquipmentMovement = (
  overrides: Partial<EquipmentMovementResponse> = {},
): EquipmentMovementResponse => ({
  id: 7,
  operation: 'ISSUE',
  status: 'PENDING',
  incidentId: 1,
  responsibleUserId: 3,
  sourceIssueMovementId: null,
  createdByUserId: 1,
  comment: 'Обследование склада',
  createdAt: '2026-11-02T12:00:00Z',
  completedAt: null,
  equipment: [mockEquipment()],
  ...overrides,
});

export const mockTender = (overrides: Partial<TenderResponse> = {}): TenderResponse => ({
  id: 4, equipmentType: 'NEURALYZER', quantity: 2, conditions: 'Поставка за неделю', status: 'NEED',
  createdByUserId: 2, createdAt: '2026-11-03T12:00:00Z', updatedAt: '2026-11-03T12:00:00Z', ...overrides,
});

export const mockTenderParticipant = (overrides: Partial<TenderParticipantResponse> = {}): TenderParticipantResponse => ({
  id: 7, tenderId: 4, name: 'ООО Орбита', proposal: 'Поставка за пять дней', status: 'PARTICIPATING',
  createdByUserId: 2, createdAt: '2026-11-03T12:00:00Z', updatedAt: '2026-11-03T12:00:00Z', ...overrides,
});

export const mockDetentionRequest = (overrides: Partial<DetentionRequestResponse> = {}): DetentionRequestResponse => ({
  id: 7, incidentId: 8, subjectName: 'Субъект 7', subjectDescription: 'Задержан у вокзала', durationDays: 30,
  status: 'NEW', createdByUserId: 2, createdAt: '2026-11-03T12:00:00Z', updatedAt: '2026-11-03T12:00:00Z', ...overrides,
});
