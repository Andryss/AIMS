import type { ChangeEquipmentMovementStatusRequest, EquipmentHistoryListResponse, EquipmentListQuery, EquipmentListResponse, EquipmentMovementHistoryListResponse, EquipmentMovementListQuery, EquipmentMovementListResponse, EquipmentMovementResponse, EquipmentResponse, SaveEquipmentMovementRequest, SaveEquipmentRequest } from '../types';
import { requestJson, queryString } from './http';

export function listEquipment(token: string, query: EquipmentListQuery): Promise<EquipmentListResponse> {
  return requestJson<EquipmentListResponse>(`/equipment?${queryString(query)}`, { method: 'GET' }, token);
}

export function getEquipment(token: string, id: number): Promise<EquipmentResponse> {
  return requestJson<EquipmentResponse>(`/equipment/${id}`, { method: 'GET' }, token);
}

export function createEquipment(token: string, payload: SaveEquipmentRequest): Promise<EquipmentResponse> {
  return requestJson<EquipmentResponse>('/equipment', { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function updateEquipment(token: string, id: number, payload: SaveEquipmentRequest): Promise<EquipmentResponse> {
  return requestJson<EquipmentResponse>(`/equipment/${id}`, { method: 'PUT', body: JSON.stringify(payload) }, token);
}

export function listEquipmentHistory(
  token: string, id: number, page: number, size: number,
): Promise<EquipmentHistoryListResponse> {
  return requestJson<EquipmentHistoryListResponse>(`/equipment/${id}/history?page=${page}&size=${size}`,
    { method: 'GET' }, token);
}

export function listEquipmentMovements(
  token: string, query: EquipmentMovementListQuery,
): Promise<EquipmentMovementListResponse> {
  return requestJson<EquipmentMovementListResponse>(`/equipment-movements?${queryString(query)}`,
    { method: 'GET' }, token);
}

export function getEquipmentMovement(token: string, id: number): Promise<EquipmentMovementResponse> {
  return requestJson<EquipmentMovementResponse>(`/equipment-movements/${id}`, { method: 'GET' }, token);
}

export function createEquipmentMovement(
  token: string, payload: SaveEquipmentMovementRequest,
): Promise<EquipmentMovementResponse> {
  return requestJson<EquipmentMovementResponse>('/equipment-movements',
    { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function updateEquipmentMovement(
  token: string, id: number, payload: SaveEquipmentMovementRequest,
): Promise<EquipmentMovementResponse> {
  return requestJson<EquipmentMovementResponse>(`/equipment-movements/${id}`,
    { method: 'PUT', body: JSON.stringify(payload) }, token);
}

export function changeEquipmentMovementStatus(
  token: string, id: number, payload: ChangeEquipmentMovementStatusRequest,
): Promise<EquipmentMovementResponse> {
  return requestJson<EquipmentMovementResponse>(`/equipment-movements/${id}/status`,
    { method: 'PATCH', body: JSON.stringify(payload) }, token);
}

export function listEquipmentMovementHistory(
  token: string, id: number, page: number, size: number,
): Promise<EquipmentMovementHistoryListResponse> {
  return requestJson<EquipmentMovementHistoryListResponse>(`/equipment-movements/${id}/history?page=${page}&size=${size}`,
    { method: 'GET' }, token);
}
