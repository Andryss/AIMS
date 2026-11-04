import type { ChangeCleanupStatusRequest, ChangeIncidentStatusRequest, CleanupReportResponse, CleanupStatus, CreateCleanupReportRequest, CreateIncidentCommentRequest, CreateIncidentRequest, IncidentAlienClassificationResponse, IncidentComment, IncidentCommentListResponse, IncidentHistoryListResponse, IncidentListResponse, IncidentResponse, IncidentStatus, IncidentThreatCalculationResponse, SetIncidentExecutorsRequest, SetIncidentResponsibleRequest, UpdateIncidentRequest } from '../types';
import { requestJson } from './http';

export function listIncidents(
  token: string,
  page: number,
  size: number,
): Promise<IncidentListResponse> {
  return requestJson<IncidentListResponse>(
    `/incidents?page=${page}&size=${size}`,
    { method: 'GET' },
    token,
  );
}

export function createIncident(
  token: string,
  payload: CreateIncidentRequest,
): Promise<IncidentResponse> {
  return requestJson<IncidentResponse>(
    '/incidents',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function updateIncident(token: string, id: number, payload: UpdateIncidentRequest): Promise<IncidentResponse> {
  return requestJson<IncidentResponse>(`/incidents/${id}`, { method: 'PUT', body: JSON.stringify(payload) }, token);
}

export function getIncident(token: string, id: number): Promise<IncidentResponse> {
  return requestJson<IncidentResponse>(`/incidents/${id}`, { method: 'GET' }, token);
}

export function changeIncidentStatus(
  token: string,
  id: number,
  status: IncidentStatus,
  comment?: string,
): Promise<IncidentResponse> {
  const payload: ChangeIncidentStatusRequest = { status };
  if (comment?.trim()) {
    payload.comment = comment.trim();
  }
  return requestJson<IncidentResponse>(
    `/incidents/${id}/status`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function listIncidentComments(
  token: string,
  incidentId: number,
  page = 0,
  size = 50,
): Promise<IncidentCommentListResponse> {
  return requestJson<IncidentCommentListResponse>(
    `/incidents/${incidentId}/comments?page=${page}&size=${size}`,
    { method: 'GET' },
    token,
  );
}

export function createIncidentComment(
  token: string,
  incidentId: number,
  text: string,
): Promise<IncidentComment> {
  const payload: CreateIncidentCommentRequest = { text };
  return requestJson<IncidentComment>(
    `/incidents/${incidentId}/comments`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function listIncidentHistory(
  token: string,
  incidentId: number,
  page = 0,
  size = 50,
): Promise<IncidentHistoryListResponse> {
  return requestJson<IncidentHistoryListResponse>(
    `/incidents/${incidentId}/history?page=${page}&size=${size}`,
    { method: 'GET' },
    token,
  );
}

export function putIncidentAlien(
  token: string,
  incidentId: number,
  alienId: number,
): Promise<IncidentResponse> {
  return requestJson<IncidentResponse>(
    `/incidents/${incidentId}/alien`,
    {
      method: 'PUT',
      body: JSON.stringify({ alienId }),
    },
    token,
  );
}

export function setIncidentResponsible(
  token: string,
  incidentId: number,
  userId: number | null,
): Promise<IncidentResponse> {
  const payload: SetIncidentResponsibleRequest = { userId };
  return requestJson<IncidentResponse>(
    `/incidents/${incidentId}/responsible`,
    {
      method: 'PUT',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function setIncidentExecutors(
  token: string,
  incidentId: number,
  userIds: number[],
): Promise<IncidentResponse> {
  const payload: SetIncidentExecutorsRequest = { userIds };
  return requestJson<IncidentResponse>(
    `/incidents/${incidentId}/executors`,
    {
      method: 'PUT',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function getCleanupReport(
  token: string,
  incidentId: number,
): Promise<CleanupReportResponse> {
  return requestJson<CleanupReportResponse>(
    `/incidents/${incidentId}/cleanup-report`,
    { method: 'GET' },
    token,
  );
}

export function createCleanupReport(
  token: string,
  incidentId: number,
  description: string,
  attachmentFileIds: number[],
): Promise<CleanupReportResponse> {
  const payload: CreateCleanupReportRequest = { description, attachmentFileIds };
  return requestJson<CleanupReportResponse>(
    `/incidents/${incidentId}/cleanup-report`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function changeCleanupStatus(
  token: string,
  incidentId: number,
  status: CleanupStatus,
): Promise<IncidentResponse> {
  const payload: ChangeCleanupStatusRequest = { status };
  return requestJson<IncidentResponse>(
    `/incidents/${incidentId}/cleanup-status`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function classifyIncidentAlien(token: string, id: number): Promise<IncidentAlienClassificationResponse> {
  return requestJson(`/incidents/${id}/alien-classification`, { method: 'POST' }, token);
}

export function calculateIncidentThreat(token: string, id: number): Promise<IncidentThreatCalculationResponse> {
  return requestJson<IncidentThreatCalculationResponse>(`/incidents/${id}/threat-calculation`, { method: 'POST' }, token);
}

export function setIncidentThreatLevel(token: string, id: number, threatLevel: number): Promise<IncidentResponse> {
  return requestJson<IncidentResponse>(`/incidents/${id}/threat-level`,
    { method: 'PUT', body: JSON.stringify({ threatLevel }) }, token);
}
