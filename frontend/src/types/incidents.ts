import type { HistoryEntry, Page } from './common';
import type { Alien } from './aliens';

export type IncidentEventType =
  | 'UNIDENTIFIED_SIGHTING'
  | 'CONTACT_SUSPECT'
  | 'ILLEGAL_UFO_LANDING'
  | 'MEMORY_ANOMALY'
  | 'ALIEN_ARTIFACT'
  | 'ALIEN_CAPTURE'
  | 'ATTACK_ON_PEOPLE'
  | 'PROPERTY_DAMAGE'
  | 'BIOLOGICAL_CONTAMINATION'
  | 'CRITICAL_INFRASTRUCTURE_DISRUPTION';

export type IncidentStatus =
  | 'DRAFT'
  | 'READY_FOR_ANALYSIS'
  | 'READY_FOR_EXECUTION'
  | 'PREPARATION_FOR_EXECUTION'
  | 'PREPARED_FOR_EXECUTION'
  | 'EXECUTING'
  | 'EXECUTION_COMPLETED'
  | 'CLARIFICATION_REQUIRED'
  | 'REANALYSIS_REQUIRED';

export type CleanupStatus = 'PREPARATION' | 'EXECUTION' | 'COMPLETED';

export interface CreateIncidentRequest {
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  victimCount?: number | null;
  attachmentFileIds: number[];
  monitoringAlertId?: number;
}

export interface UpdateIncidentRequest {
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  victimCount: number | null;
  additionalAttachmentFileIds: number[];
}

export interface CreateIncidentInitialValues {
  eventType?: IncidentEventType;
  location?: string;
  detectedAt?: string;
  description?: string;
  monitoringAlertId?: number;
  mediaUrls?: string[];
}

export interface IncidentResponse {
  id: number;
  status: IncidentStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  victimCount?: number | null;
  threatLevel?: number | null;
  attachmentFileIds: number[];
  alienId?: number;
  responsibleUserId?: number | null;
  executorUserIds: number[];
  cleanupStatus?: CleanupStatus | null;
  cleanupReportId?: number | null;
  monitoringAlertId?: number | null;
  createdAt: string;
  updatedAt: string;
}

export interface ChangeIncidentStatusRequest {
  status: IncidentStatus;
  comment?: string;
}

export interface ChangeCleanupStatusRequest {
  status: CleanupStatus;
}

export interface CreateCleanupReportRequest {
  description: string;
  attachmentFileIds: number[];
}

export interface CleanupReportResponse {
  id: number;
  incidentId: number;
  description: string;
  attachmentFileIds: number[];
  createdByUserId: number;
  createdAt: string;
}

export interface CreateIncidentCommentRequest {
  text: string;
}

export interface IncidentComment {
  id: number;
  incidentId: number;
  authorUserId: number;
  text: string;
  createdAt: string;
}

export type IncidentCommentListResponse = Page<IncidentComment>;

export interface IncidentHistorySnapshot {
  status: IncidentStatus;
  eventType: IncidentEventType;
  location: string;
  detectedAt: string;
  description: string;
  victimCount?: number | null;
  threatLevel?: number | null;
  attachmentFileIds: number[];
  alienId?: number | null;
  responsibleUserId?: number | null;
  executorUserIds: number[];
  cleanupStatus?: CleanupStatus | null;
  cleanupReportId?: number | null;
}

export type IncidentHistoryEntry = HistoryEntry<IncidentHistorySnapshot>;

export interface SetIncidentResponsibleRequest {
  userId: number | null;
}

export interface SetIncidentExecutorsRequest {
  userIds: number[];
}

export type IncidentHistoryListResponse = Page<IncidentHistoryEntry>;

export type IncidentListResponse = Page<IncidentResponse>;

export interface IncidentAlienClassificationResponse {
  status: 'MATCH' | 'NO_MATCH';
  confidence: number;
  alien: Alien | null;
}

export interface IncidentThreatCalculationResponse {
  calculatedThreatLevel: number;
  contributions: {
    criterion: 'ALIEN_THREAT' | 'DEFAULT_THREAT' | 'EVENT_TYPE' | 'VICTIMS';
    adjustment: number;
  }[];
}
