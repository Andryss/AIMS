import type { HistoryEntry, Page, PageQuery } from './common';

export type DetentionRequestStatus = 'NEW' | 'ACCEPTED' | 'IN_CUSTODY' | 'RELEASED' | 'REJECTED' | 'CANCELLED';

export interface DetentionRequestResponse {
  id: number;
  incidentId: number;
  subjectName: string;
  subjectDescription: string;
  alienId?: number | null;
  durationDays: number;
  status: DetentionRequestStatus;
  createdByUserId: number;
  reviewedByUserId?: number | null;
  createdAt: string;
  updatedAt: string;
  admittedAt?: string | null;
  plannedReleaseAt?: string | null;
  releasedAt?: string | null;
}

export interface SaveDetentionRequest {
  incidentId: number;
  subjectName: string;
  subjectDescription: string;
  alienId?: number | null;
  durationDays: number;
  comment?: string;
}

export interface ChangeDetentionRequestStatusRequest {
  status: DetentionRequestStatus;
  comment?: string;
}

export interface DetentionRequestListQuery extends PageQuery {
  q?: string;
  incidentId?: number;
  status?: DetentionRequestStatus;
}

export type DetentionRequestListResponse = Page<DetentionRequestResponse>;

export interface DetentionRequestHistorySnapshot {
  request: DetentionRequestResponse;
  transitionComment?: string | null;
}

export type DetentionRequestHistoryEntry = HistoryEntry<DetentionRequestHistorySnapshot>;

export type DetentionRequestHistoryListResponse = Page<DetentionRequestHistoryEntry>;
