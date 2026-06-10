import {
  Alien,
  AlienSearchResponse,
  AuthMeResponse,
  ChangeIncidentStatusRequest,
  CreateIncidentRequest,
  ErrorObject,
  FileUploadResponse,
  IncidentListResponse,
  IncidentResponse,
  IncidentStatus,
  NotificationListResponse,
  SignInRequest,
  SignInResponse,
  UnreadCountResponse,
} from '../types';

const API_PREFIX = '/api/v1';

export class ApiError extends Error {
  readonly status: number;
  readonly body: ErrorObject;

  constructor(status: number, body: ErrorObject) {
    super(body.humanMessage || body.message || `HTTP ${status}`);
    this.status = status;
    this.body = body;
  }
}

async function parseError(response: Response): Promise<ApiError> {
  let body: ErrorObject = {};
  try {
    body = (await response.json()) as ErrorObject;
  } catch {
    body = { humanMessage: response.statusText };
  }
  return new ApiError(response.status, body);
}

async function requestJson<T>(
  path: string,
  options: RequestInit = {},
  token?: string | null,
): Promise<T> {
  const headers = new Headers(options.headers);
  if (!headers.has('Content-Type') && options.body && !(options.body instanceof FormData)) {
    headers.set('Content-Type', 'application/json');
  }
  if (token) {
    headers.set('Authorization', `Bearer ${token}`);
  }

  const response = await fetch(`${API_PREFIX}${path}`, {
    ...options,
    headers,
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  if (!text) {
    return undefined as T;
  }

  return JSON.parse(text) as T;
}

export function signIn(payload: SignInRequest): Promise<SignInResponse> {
  return requestJson<SignInResponse>('/auth/signin', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getAuthMe(token: string): Promise<AuthMeResponse> {
  return requestJson<AuthMeResponse>('/auth/me', { method: 'GET' }, token);
}

export function uploadFile(token: string, file: File): Promise<FileUploadResponse> {
  const formData = new FormData();
  formData.append('file', file);
  return requestJson<FileUploadResponse>(
    '/files',
    {
      method: 'POST',
      body: formData,
    },
    token,
  );
}

export function uploadFiles(token: string, files: File[]): Promise<FileUploadResponse[]> {
  return Promise.all(files.map((file) => uploadFile(token, file)));
}

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

export function getIncident(token: string, id: number): Promise<IncidentResponse> {
  return requestJson<IncidentResponse>(`/incidents/${id}`, { method: 'GET' }, token);
}

export function changeIncidentStatus(
  token: string,
  id: number,
  status: IncidentStatus,
): Promise<IncidentResponse> {
  const payload: ChangeIncidentStatusRequest = { status };
  return requestJson<IncidentResponse>(
    `/incidents/${id}/status`,
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}

export function searchAliens(token: string, q: string): Promise<AlienSearchResponse> {
  const params = new URLSearchParams({ q });
  return requestJson<AlienSearchResponse>(
    `/aliens/search?${params.toString()}`,
    { method: 'GET' },
    token,
  );
}

export function getAlien(token: string, id: number): Promise<Alien> {
  return requestJson<Alien>(`/aliens/${id}`, { method: 'GET' }, token);
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

export async function downloadFile(
  token: string,
  fileId: number,
  fileName?: string,
): Promise<void> {
  const response = await fetch(`${API_PREFIX}/files/${fileId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });

  if (!response.ok) {
    throw await parseError(response);
  }

  const blob = await response.blob();
  const disposition = response.headers.get('Content-Disposition');
  const match = disposition?.match(/filename="?([^"]+)"?/i);
  const resolvedName = match?.[1] || fileName || `file-${fileId}`;

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = resolvedName;
  link.click();
  URL.revokeObjectURL(url);
}

export function listNotifications(
  token: string,
  page: number,
  size: number,
): Promise<NotificationListResponse> {
  return requestJson<NotificationListResponse>(
    `/notifications?page=${page}&size=${size}`,
    { method: 'GET' },
    token,
  );
}

export function getUnreadNotificationsCount(token: string): Promise<UnreadCountResponse> {
  return requestJson<UnreadCountResponse>('/notifications/unread-count', { method: 'GET' }, token);
}

export function markNotificationRead(token: string, id: number): Promise<void> {
  return requestJson<void>(
    `/notifications/${id}/read`,
    { method: 'PATCH' },
    token,
  );
}
