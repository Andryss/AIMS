import { useCallback } from 'react';
import { NavigateOptions, useSearchParams } from 'react-router-dom';
import { parsePage } from '../utils/params';

/** Lists share URL mechanics while retaining their own domain filters. */
export function useListSearchParams() {
  const [params, setParams] = useSearchParams();
  const page = parsePage(params.get('page'));

  const setFilter = useCallback((key: string, value: string) => {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (value) { next.set(key, value); } else { next.delete(key); }
      next.delete('page');
      return next;
    });
  }, [setParams]);

  const setPage = useCallback((nextPage: number, options?: NavigateOptions) => {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      next.set('page', String(nextPage));
      return next;
    }, options);
  }, [setParams]);

  return { params, setParams, page, setFilter, setPage };
}
