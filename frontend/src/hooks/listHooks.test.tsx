import React from 'react';
import { act, renderHook, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { useDebouncedQuery, useDebouncedSearch } from './useDebouncedSearch';
import { useListSearchParams } from './useListSearchParams';
import { usePaginatedResource } from './usePaginatedResource';

afterEach(() => jest.useRealTimers());

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason: unknown) => void;
  const promise = new Promise<T>((resolvePromise, rejectPromise) => {
    resolve = resolvePromise;
    reject = rejectPromise;
  });
  return { promise, resolve, reject };
}

it('normalizes invalid pages and preserves unrelated filters when changing page or filter', () => {
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter initialEntries={['/?page=-5&foreign=keep&type=old']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>{children}</MemoryRouter>
  );
  const { result } = renderHook(() => useListSearchParams(), { wrapper });
  expect(result.current.page).toBe(0);
  act(() => result.current.setPage(3));
  expect(result.current.params.toString()).toBe('page=3&foreign=keep&type=old');
  act(() => result.current.setFilter('status', 'AVAILABLE'));
  expect(result.current.page).toBe(0);
  expect(result.current.params.toString()).toBe('foreign=keep&type=old&status=AVAILABLE');
  act(() => result.current.setFilter('type', ''));
  expect(result.current.params.toString()).toBe('foreign=keep&status=AVAILABLE');
});

it('trims a debounced URL query and cancels the pending edit on external navigation', () => {
  jest.useFakeTimers();
  const changes: string[] = [];
  const { result, rerender, unmount } = renderHook(({ query }) => (
    useDebouncedQuery(query, (value) => changes.push(value))
  ), { initialProps: { query: 'old' } });
  act(() => result.current.setInput('  new  '));
  act(() => jest.advanceTimersByTime(299));
  expect(changes).toEqual([]);
  act(() => jest.advanceTimersByTime(1));
  expect(changes).toEqual(['new']);
  act(() => result.current.setInput('obsolete'));
  rerender({ query: 'from-history' });
  act(() => jest.advanceTimersByTime(300));
  expect(result.current.input).toBe('from-history');
  expect(changes).toEqual(['new']);
  unmount();
  jest.useRealTimers();
});

it('searches only after the debounce and ignores a result from the previous scope', async () => {
  jest.useFakeTimers();
  const old = deferred<string[]>();
  const current = deferred<string[]>();
  const searches: string[] = [];
  const { result, rerender, unmount } = renderHook(({ token, query }) => useDebouncedSearch({
    scopeKey: token,
    query,
    load: (value) => {
      searches.push(`${token}:${value}`);
      return token === 'old' ? old.promise : current.promise;
    },
    errorMessage: 'search failed',
  }), { initialProps: { token: 'old', query: ' a ' } });
  act(() => jest.advanceTimersByTime(300));
  expect(searches).toEqual([]);
  rerender({ token: 'old', query: '  agent  ' });
  act(() => jest.advanceTimersByTime(299));
  expect(searches).toEqual([]);
  await act(async () => jest.advanceTimersByTime(1));
  expect(searches).toEqual(['old:agent']);
  rerender({ token: 'new', query: 'agent' });
  await act(async () => jest.advanceTimersByTime(300));
  await act(async () => current.resolve(['current agent']));
  await act(async () => old.resolve(['obsolete agent']));
  expect(result.current.results).toEqual(['current agent']);
  expect(result.current.loading).toBe(false);
  unmount();
  jest.useRealTimers();
});

it('clears ineligible search results and ignores an obsolete rejection', async () => {
  jest.useFakeTimers();
  const old = deferred<string[]>();
  const { result, rerender, unmount } = renderHook(({ query }) => useDebouncedSearch({
    scopeKey: 'token', query, load: () => old.promise, errorMessage: 'search failed',
  }), { initialProps: { query: 'agent' } });
  act(() => jest.advanceTimersByTime(300));
  rerender({ query: 'a' });
  await act(async () => old.reject(new Error('old failure')));
  expect(result.current.results).toEqual([]);
  expect(result.current.error).toBeNull();
  expect(result.current.loading).toBe(false);
  unmount();
  jest.useRealTimers();
});

it('ignores a page response and failure after the resource scope changes', async () => {
  const old = deferred<{ page: number; totalPages: number; items: string[] }>();
  const current = deferred<{ page: number; totalPages: number; items: string[] }>();
  const { result, rerender } = renderHook(({ scopeKey }) => usePaginatedResource({
    scopeKey, page: 0, load: () => scopeKey === 'old' ? old.promise : current.promise,
    errorMessage: 'load failed', onPageCorrection: () => undefined,
  }), { initialProps: { scopeKey: 'old' } });
  rerender({ scopeKey: 'new' });
  await act(async () => current.resolve({ page: 0, totalPages: 1, items: ['new'] }));
  await act(async () => old.reject(new Error('old failure')));
  expect(result.current.result?.items).toEqual(['new']);
  expect(result.current.error).toBeNull();
  expect(result.current.loading).toBe(false);
});

it('corrects an out-of-range URL page before publishing its empty response', async () => {
  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <MemoryRouter initialEntries={['/?page=4&q=kept']}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>{children}</MemoryRouter>
  );
  const { result } = renderHook(() => {
    const route = useListSearchParams();
    const resource = usePaginatedResource({
      scopeKey: 'token', page: route.page,
      load: async (page) => ({ page, totalPages: 2, items: page === 1 ? ['last page'] : [] }),
      errorMessage: 'load failed',
      onPageCorrection: (page) => route.setPage(page, { replace: true }),
    });
    return { route, resource };
  }, { wrapper });
  await waitFor(() => expect(result.current.resource.result?.items).toEqual(['last page']));
  expect(result.current.route.page).toBe(1);
  expect(result.current.route.params.get('q')).toBe('kept');
});
