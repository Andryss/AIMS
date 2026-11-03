import { useEffect, useState } from 'react';
import { AnalyticsPeriodQuery } from '../types';

/** Каждый виджет запрашивается отдельно и скрывает результат предыдущего периода или пользователя. */
export function useAnalyticsResource<T, Q extends AnalyticsPeriodQuery>(
  token: string, query: Q, load: (token: string, query: Q) => Promise<T>,
) {
  const queryKey = JSON.stringify(query);
  const key = `${token}:${queryKey}`;
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState<{ key: string; data: T | null; loading: boolean; error: string | null }>({
    key: '', data: null, loading: true, error: null,
  });

  useEffect(() => {
    let cancelled = false;
    setState({ key, data: null, loading: true, error: null });
    Promise.resolve().then(() => load(token, JSON.parse(queryKey) as Q))
      .then((data) => {
        if (data == null) { throw new Error('Пустой ответ сервера'); }
        if (!cancelled) { setState({ key, data, loading: false, error: null }); }
      })
      .catch((err: unknown) => {
        if (!cancelled) { setState({ key, data: null, loading: false,
          error: err instanceof Error ? err.message : 'Не удалось загрузить показатель' }); }
      });

    return () => { cancelled = true; };
  }, [key, token, queryKey, load, revision]);

  const current = state.key === key ? state : { data: null, loading: true, error: null };
  return { ...current, retry: () => setRevision((value) => value + 1) };
}
