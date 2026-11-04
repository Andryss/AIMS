import { useCallback, useEffect, useRef, useState } from 'react';
import { Page } from '../types';

/** Only the structural page contract is shared; domain snapshots remain typed by the caller. */
export type HistoryPage<T> = Page<T>;

interface PagedHistoryOptions<T> {
  /** Include token and entity identity. A new scope always starts at page zero. */
  scopeKey: string;
  loadPage: (page: number) => Promise<HistoryPage<T>>;
  revision?: number;
  errorMessage?: string;
}

interface HistoryState<T> {
  key: string;
  scopeKey: string;
  result: HistoryPage<T> | null;
  previousEntry: T | undefined;
  loading: boolean;
  error: string | null;
}

export function usePagedHistory<T>({ scopeKey, loadPage, revision, errorMessage = 'Не удалось загрузить историю' }: PagedHistoryOptions<T>) {
  const [position, setPosition] = useState({ scopeKey, page: 0 });
  if (position.scopeKey !== scopeKey) {
    setPosition({ scopeKey, page: 0 });
  }
  const page = position.scopeKey === scopeKey ? position.page : 0;
  const requestKey = JSON.stringify([scopeKey, page, revision]);
  const [state, setState] = useState<HistoryState<T>>({
    key: '', scopeKey, result: null, previousEntry: undefined, loading: true, error: null,
  });
  const loader = useRef(loadPage);
  loader.current = loadPage;
  const setPage = useCallback((nextPage: number) => setPosition({ scopeKey, page: nextPage }), [scopeKey]);

  useEffect(() => {
    let cancelled = false;
    const load = loader.current;
    setState((previous) => ({ key: requestKey, scopeKey,
      result: previous.scopeKey === scopeKey ? previous.result : null,
      previousEntry: previous.scopeKey === scopeKey ? previous.previousEntry : undefined,
      loading: true, error: null }));
    async function fetchHistory() {
      try {
        const [result, previous] = await Promise.all([
          load(page), page > 0 ? load(page - 1) : Promise.resolve(null),
        ]);
        if (!cancelled) {
          setState({ key: requestKey, scopeKey, result, previousEntry: previous?.items[previous.items.length - 1], loading: false, error: null });
        }
      } catch (error: unknown) {
        if (!cancelled) {
          setState({ key: requestKey, scopeKey, result: null, previousEntry: undefined, loading: false,
            error: error instanceof Error ? error.message : errorMessage });
        }
      }
    }
    void fetchHistory();
    return () => { cancelled = true; };
  }, [requestKey, scopeKey, page, errorMessage]);

  // In-scope reloads retain references for name lookup; a new entity never sees another entity's snapshots.
  const current = state.key === requestKey ? state
    : { result: state.scopeKey === scopeKey ? state.result : null,
      previousEntry: state.scopeKey === scopeKey ? state.previousEntry : undefined, loading: true, error: null };
  return { page, setPage, result: current.result, previousEntry: current.previousEntry, loading: current.loading, error: current.error };
}
