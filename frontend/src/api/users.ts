import type { BatchUsersRequest, BatchUsersResponse, RoleName, UserSearchResponse } from '../types';
import { requestJson } from './http';

export function searchUsers(
  token: string,
  q: string,
  role?: RoleName,
): Promise<UserSearchResponse> {
  const params = new URLSearchParams({ q });
  if (role) {
    params.set('role', role);
  }
  return requestJson<UserSearchResponse>(
    `/users/search?${params.toString()}`,
    { method: 'GET' },
    token,
  );
}

export function batchUsers(token: string, ids: number[]): Promise<BatchUsersResponse> {
  const payload: BatchUsersRequest = { ids };
  return requestJson<BatchUsersResponse>(
    '/users/batch',
    {
      method: 'POST',
      body: JSON.stringify(payload),
    },
    token,
  );
}
