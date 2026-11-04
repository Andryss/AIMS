import { act, renderHook, waitFor } from '@testing-library/react';
import * as api from '../api/client';
import { TOKEN_STORAGE_KEY } from '../constants';
import { AuthMeResponse } from '../types';
import { useAuthSession } from './useAuthSession';

jest.mock('../api/client');

afterEach(() => { localStorage.clear(); jest.clearAllMocks(); });

it('opens a loaded profile while the optional notification counter is still pending', async () => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'token');
  (api.getAuthMe as jest.Mock).mockResolvedValue({ userId: 1, login: 'user', roles: ['ADMIN'], permissions: [] });
  (api.getUnreadNotificationsCount as jest.Mock).mockReturnValue(new Promise(() => {}));
  const { result } = renderHook(() => useAuthSession());
  await waitFor(() => expect(result.current.profile?.login).toBe('user'));
});

it('ignores a delayed profile after signing out', async () => {
  localStorage.setItem(TOKEN_STORAGE_KEY, 'old-token');
  let resolve!: (profile: AuthMeResponse) => void;
  (api.getAuthMe as jest.Mock).mockReturnValue(new Promise((done) => { resolve = done; }));
  (api.getUnreadNotificationsCount as jest.Mock).mockResolvedValue({ count: 4 });
  const { result } = renderHook(() => useAuthSession());
  act(() => result.current.signOut());
  await act(async () => resolve({ userId: 1, login: 'old-user', roles: ['ADMIN'], permissions: [] }));
  expect(result.current.accessToken).toBeNull();
  expect(result.current.profile).toBeNull();
  expect(result.current.unreadCount).toBe(0);
  expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
});
