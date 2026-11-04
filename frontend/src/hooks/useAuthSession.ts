import { useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { TOKEN_STORAGE_KEY } from '../constants';
import { AuthMeResponse } from '../types';
import { clearFileCache } from '../utils/fileCache';

export function useAuthSession() {
  const [accessToken, setAccessToken] = useState<string | null>(() => localStorage.getItem(TOKEN_STORAGE_KEY));
  const [loadedProfile, setLoadedProfile] = useState<{ token: string; value: AuthMeResponse } | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const signInRequest = useRef(0);
  const profile = loadedProfile?.token === accessToken ? loadedProfile.value : null;

  useEffect(() => {
    if (!accessToken) { return; }
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      api.getAuthMe(accessToken).then((value) => {
        if (!cancelled) { setLoadedProfile({ token: accessToken, value }); }
      }),
      api.getUnreadNotificationsCount(accessToken).catch(() => ({ count: 0 })),
    ]).then(([, unread]) => {
      if (!cancelled) { setUnreadCount(unread.count); }
    }).catch((err: unknown) => {
      if (cancelled) { return; }
      setError(err instanceof Error ? err.message : 'Не удалось загрузить профиль');
      clearFileCache();
      setAccessToken(null);
      setLoadedProfile(null);
      setUnreadCount(0);
      localStorage.removeItem(TOKEN_STORAGE_KEY);
    }).finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [accessToken]);

  const signIn = async (login: string, password: string): Promise<boolean> => {
    const request = ++signInRequest.current;
    setLoading(true);
    setError(null);
    try {
      const response = await api.signIn({ login, password });
      if (request !== signInRequest.current) { return false; }
      setAccessToken(response.accessToken);
      localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
      return true;
    } catch (err: unknown) {
      if (request === signInRequest.current) { setError(err instanceof Error ? err.message : 'Ошибка входа'); }
      return false;
    } finally { if (request === signInRequest.current) { setLoading(false); } }
  };

  const signOut = () => {
    signInRequest.current += 1;
    clearFileCache();
    setAccessToken(null);
    setLoadedProfile(null);
    setUnreadCount(0);
    setLoading(false);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  };

  return { accessToken, profile, error, loading, unreadCount, setUnreadCount, signIn, signOut };
}
