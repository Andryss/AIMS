import type { ChangeDetentionRequestStatusRequest, DetentionRequestHistoryListResponse, DetentionRequestListQuery, DetentionRequestListResponse, DetentionRequestResponse, SaveDetentionRequest } from '../types';
import { requestJson, queryString } from './http';

export function listDetentionRequests(token: string, query: DetentionRequestListQuery): Promise<DetentionRequestListResponse> {
  return requestJson<DetentionRequestListResponse>(`/detention-requests?${queryString(query)}`, { method: 'GET' }, token);
}

export function getDetentionRequest(token: string, id: number): Promise<DetentionRequestResponse> {
  return requestJson<DetentionRequestResponse>(`/detention-requests/${id}`, { method: 'GET' }, token);
}

export function createDetentionRequest(token: string, payload: SaveDetentionRequest): Promise<DetentionRequestResponse> {
  return requestJson<DetentionRequestResponse>('/detention-requests', { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function updateDetentionRequest(token: string, id: number, payload: SaveDetentionRequest): Promise<DetentionRequestResponse> {
  return requestJson<DetentionRequestResponse>(`/detention-requests/${id}`, { method: 'PUT', body: JSON.stringify(payload) }, token);
}

export function changeDetentionRequestStatus(token: string, id: number, payload: ChangeDetentionRequestStatusRequest): Promise<DetentionRequestResponse> {
  return requestJson<DetentionRequestResponse>(`/detention-requests/${id}/status`, { method: 'PATCH', body: JSON.stringify(payload) }, token);
}

export function listDetentionRequestHistory(token: string, id: number, page: number, size: number): Promise<DetentionRequestHistoryListResponse> {
  return requestJson<DetentionRequestHistoryListResponse>(`/detention-requests/${id}/history?page=${page}&size=${size}`,
    { method: 'GET' }, token);
}
