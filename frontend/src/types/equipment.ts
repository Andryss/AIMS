import type { HistoryEntry, Page, PageQuery } from './common';

export type EquipmentType = 'NEURALYZER' | 'NEUTRALIZER';

export type EquipmentStatus = 'AVAILABLE' | 'ISSUED';

export type EquipmentMovementOperation = 'ISSUE' | 'RETURN';

export type EquipmentMovementStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'CANCELLED' | 'REJECTED';

export interface SaveEquipmentRequest {
  type: EquipmentType;
  model: string;
  serialNumber: string;
}

export interface EquipmentResponse extends SaveEquipmentRequest {
  id: number;
  status: EquipmentStatus;
  holderUserId?: number | null;
  currentIssueMovementId?: number | null;
  sourceTenderId?: number | null;
  createdAt: string;
}

export interface EquipmentListQuery extends PageQuery {
  q?: string;
  type?: EquipmentType;
  status?: EquipmentStatus;
  holderUserId?: number;
}

export type EquipmentListResponse = Page<EquipmentResponse>;

export type EquipmentHistoryEntry = HistoryEntry<{
    event: 'REGISTERED' | 'RECEIVED' | 'UPDATED' | 'ISSUED' | 'RETURNED';
    movementId?: number | null;
    equipment: EquipmentResponse;
  }>;

export type EquipmentHistoryListResponse = Page<EquipmentHistoryEntry>;

export interface SaveEquipmentMovementRequest {
  operation: EquipmentMovementOperation;
  incidentId: number;
  responsibleUserId: number;
  sourceIssueMovementId?: number | null;
  equipmentIds: number[];
  comment?: string;
}

export interface EquipmentMovementResponse {
  id: number;
  operation: EquipmentMovementOperation;
  status: EquipmentMovementStatus;
  incidentId: number;
  responsibleUserId: number;
  sourceIssueMovementId?: number | null;
  createdByUserId: number;
  comment?: string | null;
  createdAt: string;
  completedAt?: string | null;
  equipment: EquipmentResponse[];
}

export interface EquipmentMovementListQuery extends PageQuery {
  incidentId?: number;
  responsibleUserId?: number;
  status?: EquipmentMovementStatus;
  operation?: EquipmentMovementOperation;
  equipmentId?: number;
}

export type EquipmentMovementListResponse = Page<EquipmentMovementResponse>;

export interface ChangeEquipmentMovementStatusRequest {
  status: EquipmentMovementStatus;
  comment?: string;
}

export type EquipmentMovementHistoryEntry = HistoryEntry<{
    movement: EquipmentMovementResponse;
    transitionComment?: string | null;
  }>;

export type EquipmentMovementHistoryListResponse = Page<EquipmentMovementHistoryEntry>;
