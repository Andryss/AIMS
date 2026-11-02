import { useEffect, useState } from 'react';
import * as api from '../api/client';
import { loadUsersMap } from '../utils/incidentUsers';

/** Пакетная загрузка сотрудников для таблиц и истории, без запроса на каждый экземпляр. */
export function useUsersMap(token: string, ids: Array<number | null | undefined>): Map<number, string> {
  const key = Array.from(new Set(ids.filter((id): id is number => id != null))).sort((a, b) => a - b).join(',');
  const [users, setUsers] = useState<Map<number, string>>(new Map());

  useEffect(() => {
    let cancelled = false;
    setUsers(new Map());
    const userIds = key ? key.split(',').map(Number) : [];

    loadUsersMap(token, userIds, api.batchUsers)
      .then((result) => { if (!cancelled) { setUsers(result); } })
      .catch(() => { if (!cancelled) { setUsers(new Map()); } });

    return () => { cancelled = true; };
  }, [token, key]);

  return users;
}
