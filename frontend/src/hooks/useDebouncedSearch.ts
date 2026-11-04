import { useCallback, useEffect, useRef, useState } from 'react';

/** Keep a list input in sync with URL navigation and commit only settled edits. */
export function useDebouncedQuery(query: string, onChange: (query: string) => void, delay = 300) {
  const [input, setInput] = useState(query);
  const change = useRef(onChange);
  change.current = onChange;
  useEffect(() => setInput(query), [query]);
  useEffect(() => {
    if (input.trim() === query) { return; }
    const timer = window.setTimeout(() => change.current(input.trim()), delay);
    return () => window.clearTimeout(timer);
  }, [input, query, delay]);
  return { input, setInput };
}

interface DebouncedSearchOptions<T> {
  scopeKey: string;
  query: string;
  load: (query: string) => Promise<T[]>;
  errorMessage: string;
  enabled?: boolean;
  minLength?: number;
  delay?: number;
  retainResults?: boolean;
}

const EMPTY_RESULTS: never[] = [];

/** The scope includes the account and any constraints, such as picker exclusions. */
export function useDebouncedSearch<T>({
  scopeKey, query, load, errorMessage, enabled = true, minLength = 2, delay = 300, retainResults = false,
}: DebouncedSearchOptions<T>) {
  const eligible = enabled && query.trim().length >= minLength;
  const requestKey = JSON.stringify([scopeKey, query, eligible, minLength, delay]);
  const activeKey = useRef(requestKey);
  activeKey.current = requestKey;
  const callbacks = useRef({ load, errorMessage });
  callbacks.current = { load, errorMessage };
  const [state, setState] = useState<{
    scopeKey: string; requestKey: string; results: T[]; loading: boolean; error: string | null;
  }>({ scopeKey, requestKey, results: [], loading: false, error: null });

  useEffect(() => {
    let cancelled = false;
    const isCurrent = () => !cancelled && activeKey.current === requestKey;
    setState((previous) => ({
      scopeKey, requestKey, loading: false, error: null,
      results: eligible && retainResults && previous.scopeKey === scopeKey ? previous.results : [],
    }));
    if (!eligible) { return; }
    const { load: search, errorMessage: fallback } = callbacks.current;
    const timer = window.setTimeout(() => {
      setState((previous) => ({ ...previous, loading: true }));
      Promise.resolve().then(() => search(query.trim()))
        .then((results) => {
          if (isCurrent()) { setState({ scopeKey, requestKey, results, loading: false, error: null }); }
        })
        .catch((error: unknown) => {
          if (isCurrent()) { setState({ scopeKey, requestKey, results: [], loading: false,
            error: error instanceof Error ? error.message : fallback }); }
        });
    }, delay);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [scopeKey, query, eligible, delay, minLength, requestKey, retainResults]);

  const clearResults = useCallback(() => setState((previous) => ({ ...previous, results: [] })), []);
  const current = state.requestKey === requestKey;
  return {
    results: eligible && state.scopeKey === scopeKey && (current || retainResults) ? state.results : EMPTY_RESULTS,
    loading: current && state.loading,
    error: current ? state.error : null,
    clearResults,
  };
}
