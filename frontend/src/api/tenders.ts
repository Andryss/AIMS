import type { ChangeTenderStatusRequest, ReceiveTenderRequest, SaveTenderParticipantRequest, SaveTenderRequest, SelectTenderWinnerRequest, TenderHistoryListResponse, TenderListQuery, TenderListResponse, TenderParticipantListResponse, TenderParticipantResponse, TenderReceiptResponse, TenderResponse } from '../types';
import { requestJson, queryString } from './http';

export function listTenders(token: string, query: TenderListQuery): Promise<TenderListResponse> {
  return requestJson<TenderListResponse>(`/tenders?${queryString(query)}`, { method: 'GET' }, token);
}

export function getTender(token: string, id: number): Promise<TenderResponse> {
  return requestJson<TenderResponse>(`/tenders/${id}`, { method: 'GET' }, token);
}

export function createTender(token: string, payload: SaveTenderRequest): Promise<TenderResponse> {
  return requestJson<TenderResponse>('/tenders', { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function updateTender(token: string, id: number, payload: SaveTenderRequest): Promise<TenderResponse> {
  return requestJson<TenderResponse>(`/tenders/${id}`, { method: 'PUT', body: JSON.stringify(payload) }, token);
}

export function changeTenderStatus(token: string, id: number, payload: ChangeTenderStatusRequest): Promise<TenderResponse> {
  return requestJson<TenderResponse>(`/tenders/${id}/status`, { method: 'PATCH', body: JSON.stringify(payload) }, token);
}

export function listTenderParticipants(token: string, id: number, page: number, size: number): Promise<TenderParticipantListResponse> {
  return requestJson<TenderParticipantListResponse>(`/tenders/${id}/participants?page=${page}&size=${size}`,
    { method: 'GET' }, token);
}

export function createTenderParticipant(token: string, id: number, payload: SaveTenderParticipantRequest): Promise<TenderParticipantResponse> {
  return requestJson<TenderParticipantResponse>(`/tenders/${id}/participants`,
    { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function updateTenderParticipant(token: string, id: number, participantId: number,
  payload: SaveTenderParticipantRequest): Promise<TenderParticipantResponse> {
  return requestJson<TenderParticipantResponse>(`/tenders/${id}/participants/${participantId}`,
    { method: 'PUT', body: JSON.stringify(payload) }, token);
}

export function selectTenderWinner(token: string, id: number, payload: SelectTenderWinnerRequest): Promise<TenderResponse> {
  return requestJson<TenderResponse>(`/tenders/${id}/winner`, { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function receiveTender(token: string, id: number, payload: ReceiveTenderRequest): Promise<TenderReceiptResponse> {
  return requestJson<TenderReceiptResponse>(`/tenders/${id}/receipt`, { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function listTenderHistory(token: string, id: number, page: number, size: number): Promise<TenderHistoryListResponse> {
  return requestJson<TenderHistoryListResponse>(`/tenders/${id}/history?page=${page}&size=${size}`,
    { method: 'GET' }, token);
}
