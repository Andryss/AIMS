import type { AuthMeResponse, SignInRequest, SignInResponse } from '../types';
import { requestJson } from './http';

export function signIn(payload: SignInRequest): Promise<SignInResponse> {
  return requestJson<SignInResponse>('/auth/signin', {
    method: 'POST',
    body: JSON.stringify(payload),
  });
}

export function getAuthMe(token: string): Promise<AuthMeResponse> {
  return requestJson<AuthMeResponse>('/auth/me', { method: 'GET' }, token);
}
