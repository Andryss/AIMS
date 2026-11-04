import type { Alien, AlienListResponse, AlienSearchResponse, SaveAlienRequest } from '../types';
import { requestJson } from './http';

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

export function listAliens(
  token: string,
  query: { q?: string; page: number; size: number },
): Promise<AlienListResponse> {
  const params = new URLSearchParams({ page: String(query.page), size: String(query.size) });
  if (query.q?.trim()) {
    params.set('q', query.q.trim());
  }
  return requestJson<AlienListResponse>(`/aliens?${params.toString()}`, { method: 'GET' }, token);
}

export function createAlien(token: string, payload: SaveAlienRequest): Promise<Alien> {
  return requestJson<Alien>('/aliens', { method: 'POST', body: JSON.stringify(payload) }, token);
}

export function updateAlien(token: string, id: number, payload: SaveAlienRequest): Promise<Alien> {
  return requestJson<Alien>(`/aliens/${id}`, { method: 'PUT', body: JSON.stringify(payload) }, token);
}
