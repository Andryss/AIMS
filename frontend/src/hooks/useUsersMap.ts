import { loadUsersMap } from '../utils/users';
import { useEffect, useState } from 'react';
import * as api from '../api/client';

/** Пакетная загрузка сотрудников для таблиц и истории, без запроса на каждый экземпляр. */
export function useUsersMapState(token: string, ids: Array<number | null | undefined>) {
  const key = Array.from(new Set(ids.filter((id): id is number => id != null))).sort((a, b) => a - b).join(',');
  const [loading, setLoading] = useState(false);
  const [users, setUsers] = useState<Map<number, string>>(new Map());

  useEffect(() => {
    let cancelled = false;
    setUsers(new Map());
    setLoading(true);
    const userIds = key ? key.split(',').map(Number) : [];

    loadUsersMap(token, userIds, api.batchUsers)
      .then((result) => { if (!cancelled) { setUsers(result); } })
      .catch(() => { if (!cancelled) { setUsers(new Map()); } })
      .finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, key]);

  return { users, setUsers, loading };
}

export function useUsersMap(token: string, ids: Array<number | null | undefined>): Map<number, string> {
  return useUsersMapState(token, ids).users;
}
