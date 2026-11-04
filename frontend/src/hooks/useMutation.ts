import { useCallback, useLayoutEffect, useRef, useState } from 'react';

export interface MutationSession {
  isCurrent: () => boolean;
  fail: (error: unknown, fallback: string) => void;
  finish: () => void;
}

/** Guards a single write within an explicit token/entity/dialog scope. */
export function useMutation(scopeKey: string) {
  const scope = useRef({ key: scopeKey, generation: 0, busy: false, mounted: true });
  const [state, setState] = useState({ key: scopeKey, busy: false, error: null as string | null });
  if (scope.current.key !== scopeKey) {
    scope.current = { key: scopeKey, generation: scope.current.generation + 1, busy: false, mounted: true };
  }
  useLayoutEffect(() => {
    scope.current.mounted = true;
    setState({ key: scopeKey, busy: false, error: null });
    return () => {
      scope.current.mounted = false;
      scope.current.generation += 1;
      scope.current.busy = false;
    };
  }, [scopeKey]);

  const setError = useCallback((error: string | null) => {
    if (scope.current.key !== scopeKey || !scope.current.mounted) { return; }
    setState((previous) => ({ key: scopeKey, busy: previous.key === scopeKey && previous.busy, error }));
  }, [scopeKey]);
  const start = (): MutationSession | null => {
    if (scope.current.key !== scopeKey || scope.current.busy || !scope.current.mounted) { return null; }
    const generation = ++scope.current.generation;
    scope.current.busy = true;
    setState({ key: scopeKey, busy: true, error: null });
    const isCurrent = () => scope.current.mounted && scope.current.key === scopeKey
      && scope.current.generation === generation;
    return {
      isCurrent,
      fail: (error, fallback) => { if (isCurrent()) { setError(error instanceof Error ? error.message : fallback); } },
      finish: () => {
        if (isCurrent()) { scope.current.busy = false; setState((previous) => ({ ...previous, busy: false })); }
      },
    };
  };
  return { busy: state.key === scopeKey && state.busy, error: state.key === scopeKey ? state.error : null,
    setError, start, isBusy: () => scope.current.busy };
}
