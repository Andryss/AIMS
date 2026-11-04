import type { ErrorObject } from '../types';

export const API_PREFIX = '/api/v1';

export class ApiError extends Error {
  readonly status: number;
  readonly body: ErrorObject;

  constructor(status: number, body: ErrorObject) {
    super(body.humanMessage || body.message || `HTTP ${status}`);
    this.status = status;
    this.body = body;
  }
}

export async function parseError(response: Response): Promise<ApiError> {
  let body: ErrorObject = {};
  try {
    body = (await response.json()) as ErrorObject;
  } catch {
    body = { humanMessage: response.statusText };
  }
  return new ApiError(response.status, body);
}

export async function requestJson<T>(
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

export function queryString(query: object): string {
  const params = new URLSearchParams();
  Object.entries(query).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim()) {
      params.set(key, String(value).trim());
    }
  });

  return params.toString();
}
