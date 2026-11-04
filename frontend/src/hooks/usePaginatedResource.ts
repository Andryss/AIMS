import { SetStateAction, useCallback, useEffect, useRef, useState } from 'react';

interface PaginatedResult {
  page: number;
  totalPages: number;
}

interface PaginatedResourceOptions<T extends PaginatedResult> {
  /** Include the account and every filter that changes the loaded resource. */
  scopeKey: string;
  page: number;
  load: (page: number) => Promise<T>;
  errorMessage: string;
  onPageCorrection: (page: number) => void;
}

interface ResourceState<T> {
  scopeKey: string;
  result: T | null;
  loading: boolean;
  error: string | null;
}

export function usePaginatedResource<T extends PaginatedResult>({
  scopeKey, page, load, errorMessage, onPageCorrection,
}: PaginatedResourceOptions<T>) {
  const [revision, setRevision] = useState(0);
  const [state, setState] = useState<ResourceState<T>>({
    scopeKey, result: null, loading: true, error: null,
  });
  const callbacks = useRef({ load, onPageCorrection, errorMessage });
  callbacks.current = { load, onPageCorrection, errorMessage };
  const requestKey = JSON.stringify([scopeKey, page, revision]);
  const currentRequestKey = useRef(requestKey);
  currentRequestKey.current = requestKey;

  useEffect(() => {
    let cancelled = false;
    const isCurrent = () => !cancelled && currentRequestKey.current === requestKey;
    setState((previous) => ({
      scopeKey, result: previous.scopeKey === scopeKey ? previous.result : null,
      loading: true, error: null,
    }));
    const { load: loadPage, onPageCorrection: correctPage, errorMessage: fallback } = callbacks.current;
    Promise.resolve().then(() => loadPage(page))
      .then((response) => {
        if (!isCurrent()) { return; }
        if (page > 0 && page >= Math.max(response.totalPages, 1)) {
          correctPage(Math.max(response.totalPages - 1, 0));
        } else {
          setState({ scopeKey, result: response, loading: false, error: null });
        }
      })
      .catch((error: unknown) => {
        if (isCurrent()) {
          setState((previous) => ({ ...previous, loading: false,
            error: error instanceof Error ? error.message : fallback }));
        }
      })
      .finally(() => {
        if (isCurrent()) { setState((previous) => ({ ...previous, loading: false })); }
      });
    return () => { cancelled = true; };
  }, [scopeKey, page, revision, requestKey]);

  const setResult = useCallback((update: SetStateAction<T | null>) => {
    setState((previous) => previous.scopeKey !== scopeKey ? previous : {
      ...previous, result: typeof update === 'function' ? update(previous.result) : update,
    });
  }, [scopeKey]);
  const reload = useCallback(() => setRevision((value) => value + 1), []);
  const current = state.scopeKey === scopeKey ? state : { result: null, loading: true, error: null };
  return { result: current.result, loading: current.loading, error: current.error, setResult, reload };
}
