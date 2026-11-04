import { act, renderHook } from '@testing-library/react';
import { MutationSession, useMutation } from './useMutation';

it('rejects duplicate starts before React renders busy state', () => {
  const { result } = renderHook(() => useMutation('token:card-1'));
  let first: MutationSession | null = null;
  let duplicate: MutationSession | null = null;
  act(() => { first = result.current.start(); duplicate = result.current.start(); });
  expect(first).not.toBeNull();
  expect(duplicate).toBeNull();
  expect(result.current.busy).toBe(true);
});

it('keeps a new scope busy and error-free when the previous request fails', () => {
  const { result, rerender } = renderHook(({ scope }) => useMutation(scope), { initialProps: { scope: 'token:card-1' } });
  let old!: MutationSession;
  act(() => { old = result.current.start()!; });
  rerender({ scope: 'next-token:card-2' });
  act(() => { result.current.start(); old.fail(new Error('Old error'), 'fallback'); old.finish(); });
  expect(result.current.busy).toBe(true);
  expect(result.current.error).toBeNull();
  expect(old.isCurrent()).toBe(false);
});

it('does not revive a request after leaving and returning to the same scope', () => {
  const { result, rerender } = renderHook(({ scope }) => useMutation(scope), { initialProps: { scope: 'A' } });
  let old!: MutationSession;
  act(() => { old = result.current.start()!; });
  rerender({ scope: 'B' });
  rerender({ scope: 'A' });
  expect(old.isCurrent()).toBe(false);
  expect(result.current.busy).toBe(false);
});

it('rejects a retained submit callback from a different scope', () => {
  const { result, rerender } = renderHook(({ scope }) => useMutation(scope), { initialProps: { scope: 'A' } });
  const oldStart = result.current.start;
  rerender({ scope: 'B' });
  let stale: MutationSession | null = null;
  act(() => { stale = oldStart(); });
  expect(stale).toBeNull();
  expect(result.current.busy).toBe(false);
});

it('does not let a finished session clear a retry or replace its error', () => {
  const { result } = renderHook(() => useMutation('A'));
  let first!: MutationSession;
  act(() => { first = result.current.start()!; });
  act(() => { first.fail(new Error('Save failed'), 'fallback'); first.finish(); });
  expect(result.current.error).toBe('Save failed');
  act(() => { result.current.start(); first.finish(); first.fail(new Error('Old failure'), 'fallback'); });
  expect(result.current.busy).toBe(true);
  expect(result.current.error).toBeNull();
});

it('invalidates a pending mutation when the owner unmounts', () => {
  const { result, unmount } = renderHook(() => useMutation('A'));
  let pending!: MutationSession;
  act(() => { pending = result.current.start()!; });
  unmount();
  expect(pending.isCurrent()).toBe(false);
});
