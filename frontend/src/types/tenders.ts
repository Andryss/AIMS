import type { EquipmentResponse, EquipmentType } from './equipment';
import type { HistoryEntry, Page, PageQuery } from './common';

export type TenderStatus = 'NEED' | 'COLLECTING_OFFERS' | 'AWAITING_DELIVERY' | 'COMPLETED' | 'CANCELLED';

export type TenderParticipantStatus = 'PARTICIPATING' | 'WINNER';

export interface SaveTenderRequest {
  equipmentType: EquipmentType;
  quantity: number;
  conditions?: string | null;
  incidentId?: number | null;
}

export interface TenderResponse extends SaveTenderRequest {
  id: number;
  status: TenderStatus;
  createdByUserId: number;
  winnerParticipantId?: number | null;
  createdAt: string;
  updatedAt: string;
  completedAt?: string | null;
}

export interface TenderListQuery extends PageQuery {
  q?: string;
  status?: TenderStatus;
  equipmentType?: EquipmentType;
  incidentId?: number;
}

export type TenderListResponse = Page<TenderResponse>;

export interface ChangeTenderStatusRequest {
  status: TenderStatus;
  comment?: string;
}

export interface SaveTenderParticipantRequest {
  name: string;
  proposal: string;
}

export interface TenderParticipantResponse extends SaveTenderParticipantRequest {
  id: number;
  tenderId: number;
  status: TenderParticipantStatus;
  createdByUserId: number;
  createdAt: string;
  updatedAt: string;
}

export type TenderParticipantListResponse = Page<TenderParticipantResponse>;

export interface SelectTenderWinnerRequest { participantId: number; }

export interface ReceiveTenderRequest {
  equipment: Array<{ model: string; serialNumber: string }>;
  comment?: string;
}

export interface TenderReceiptResponse {
  tender: TenderResponse;
  equipment: EquipmentResponse[];
}

export interface TenderHistorySnapshot {
  tender: TenderResponse;
  participants: TenderParticipantResponse[];
  transitionComment?: string | null;
  receivedEquipmentIds: number[];
}

export type TenderHistoryEntry = HistoryEntry<TenderHistorySnapshot>;

export type TenderHistoryListResponse = Page<TenderHistoryEntry>;
