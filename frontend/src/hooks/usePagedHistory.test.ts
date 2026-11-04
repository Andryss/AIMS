import { act, renderHook, waitFor } from '@testing-library/react';
import { HistoryPage, usePagedHistory } from './usePagedHistory';

type Entry = { id: number; snapshot: string };
const historyPage = (items: Entry[], page = 0): HistoryPage<Entry> => ({
  items, page, size: 10, totalPages: 3, totalElements: 21,
});
function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (error: unknown) => void;
  const promise = new Promise<T>((yes, no) => { resolve = yes; reject = no; });
  return { promise, resolve, reject };
}

it('provides the last preceding snapshot at a page boundary and clears it on page zero', async () => {
  const pages = [historyPage([{ id: 1, snapshot: 'created' }, { id: 10, snapshot: 'issued' }]),
    historyPage([{ id: 11, snapshot: 'returned' }], 1)];
  const { result } = renderHook(() => usePagedHistory({ scopeKey: 'equipment:1', loadPage: async (page) => pages[page] }));
  await waitFor(() => expect(result.current.loading).toBe(false));
  expect(result.current.previousEntry).toBeUndefined();
  act(() => result.current.setPage(1));
  await waitFor(() => expect(result.current.result?.items[0].id).toBe(11));
  expect(result.current.previousEntry).toEqual({ id: 10, snapshot: 'issued' });
  act(() => result.current.setPage(0));
  await waitFor(() => expect(result.current.result?.items[0].id).toBe(1));
  expect(result.current.previousEntry).toBeUndefined();
});

it('resets a new scope to page zero and never exposes the preceding scope while loading', async () => {
  const pending = deferred<HistoryPage<Entry>>();
  const requested: string[] = [];
  const { result, rerender } = renderHook(({ scope }) => usePagedHistory({ scopeKey: scope,
    loadPage: async (page) => {
      requested.push(`${scope}:${page}`);
      return scope === 'new-token:2' ? pending.promise : historyPage([{ id: page + 1, snapshot: scope }], page);
    },
  }), { initialProps: { scope: 'old-token:1' } });
  await waitFor(() => expect(result.current.loading).toBe(false));
  act(() => result.current.setPage(2));
  await waitFor(() => expect(result.current.result?.items[0].id).toBe(3));
  rerender({ scope: 'new-token:2' });
  expect(result.current.page).toBe(0);
  expect(result.current.loading).toBe(true);
  expect(result.current.result).toBeNull();
  expect(result.current.previousEntry).toBeUndefined();
  await act(async () => pending.resolve(historyPage([{ id: 20, snapshot: 'new entity' }])));
  expect(result.current.result?.items[0].snapshot).toBe('new entity');
  expect(requested.filter((item) => item.startsWith('new-token:2'))).toEqual(['new-token:2:0']);
});

it('ignores an old scope response that completes after the new history', async () => {
  const stale = deferred<HistoryPage<Entry>>();
  const { result, rerender } = renderHook(({ scope }) => usePagedHistory({ scopeKey: scope,
    loadPage: async () => scope === 'old' ? stale.promise : historyPage([{ id: 2, snapshot: 'current' }]),
  }), { initialProps: { scope: 'old' } });
  rerender({ scope: 'new' });
  await waitFor(() => expect(result.current.result?.items[0].snapshot).toBe('current'));
  await act(async () => stale.resolve(historyPage([{ id: 1, snapshot: 'obsolete' }])));
  expect(result.current.result?.items[0].snapshot).toBe('current');
  expect(result.current.error).toBeNull();
  expect(result.current.loading).toBe(false);
});

it('ignores a failed old revision after a refreshed history has loaded', async () => {
  const stale = deferred<HistoryPage<Entry>>();
  const { result, rerender } = renderHook(({ revision }) => usePagedHistory({ scopeKey: 'equipment:1', revision,
    loadPage: async () => revision === 0 ? stale.promise : historyPage([{ id: 2, snapshot: 'refreshed' }]),
  }), { initialProps: { revision: 0 } });
  rerender({ revision: 1 });
  await waitFor(() => expect(result.current.result?.items[0].snapshot).toBe('refreshed'));
  await act(async () => stale.reject(new Error('obsolete failure')));
  expect(result.current.error).toBeNull();
  expect(result.current.result?.items[0].snapshot).toBe('refreshed');
});

it('reports an error when the preceding snapshot cannot be loaded', async () => {
  const networkFailure: unknown = 'offline';
  const { result } = renderHook(() => usePagedHistory({ scopeKey: 'equipment:1', errorMessage: 'История недоступна',
    loadPage: async (page) => { if (page === 0) { throw networkFailure; } return historyPage([{ id: 11, snapshot: 'returned' }], page); },
  }));
  await waitFor(() => expect(result.current.error).toBe('История недоступна'));
  act(() => result.current.setPage(1));
  await waitFor(() => expect(result.current.loading).toBe(false));
  expect(result.current.result).toBeNull();
  expect(result.current.previousEntry).toBeUndefined();
  expect(result.current.error).toBe('История недоступна');
});

it('retains same-entity snapshots during a page request so reference labels stay available', async () => {
  const next = deferred<HistoryPage<Entry>>();
  const { result } = renderHook(() => usePagedHistory({ scopeKey: 'equipment:1',
    loadPage: async (page) => page === 0 ? historyPage([{ id: 10, snapshot: 'issued' }]) : next.promise,
  }));
  await waitFor(() => expect(result.current.result?.items[0].id).toBe(10));
  act(() => result.current.setPage(1));
  expect(result.current.loading).toBe(true);
  expect(result.current.result?.items[0]).toEqual({ id: 10, snapshot: 'issued' });
  await act(async () => next.resolve(historyPage([{ id: 11, snapshot: 'returned' }], 1)));
  expect(result.current.result?.items[0].snapshot).toBe('returned');
});

it('starts at page zero when returning to an entity whose history was previously paged', async () => {
  const { result, rerender } = renderHook(({ scope }) => usePagedHistory({ scopeKey: scope,
    loadPage: async (page) => historyPage([{ id: page + 1, snapshot: scope }], page),
  }), { initialProps: { scope: 'first' } });
  await waitFor(() => expect(result.current.loading).toBe(false));
  act(() => result.current.setPage(2));
  await waitFor(() => expect(result.current.result?.items[0].id).toBe(3));
  rerender({ scope: 'second' });
  await waitFor(() => expect(result.current.result?.items[0].snapshot).toBe('second'));
  rerender({ scope: 'first' });
  expect(result.current.page).toBe(0);
  await waitFor(() => expect(result.current.result?.items[0]).toEqual({ id: 1, snapshot: 'first' }));
});
