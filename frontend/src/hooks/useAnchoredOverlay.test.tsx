import React, { useRef, useState } from 'react';
import { act, fireEvent, render, renderHook, screen } from '@testing-library/react';
import { useAnchoredOverlay } from './useAnchoredOverlay';

const viewport = { width: window.innerWidth, height: window.innerHeight };
function setViewport(width: number, height: number) {
  Object.defineProperty(window, 'innerWidth', { configurable: true, value: width });
  Object.defineProperty(window, 'innerHeight', { configurable: true, value: height });
}
function bounds(left: number, top: number, width: number, height: number): DOMRect {
  return { x: left, y: top, left, top, right: left + width, bottom: top + height, width, height, toJSON: () => ({}) };
}
function Overlay() {
  const [open, setOpen] = useState(false);
  const anchor = useRef<HTMLButtonElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const { scheduleOpen, scheduleClose, cancelTimers, position } = useAnchoredOverlay({
    open, anchor, panel, width: 320, openDelay: 500, closeDelay: 150,
  });
  return <>
    <button ref={anchor} onMouseEnter={() => scheduleOpen(() => setOpen(true))}
      onMouseLeave={() => scheduleClose(() => setOpen(false))}>Сведения</button>
    {open && <div ref={panel} role="tooltip" style={position} onMouseEnter={cancelTimers}
      onMouseLeave={() => scheduleClose(() => setOpen(false))}>Подробности</div>}
  </>;
}

beforeEach(() => { jest.useFakeTimers(); setViewport(1000, 800); });
afterEach(() => { jest.useRealTimers(); jest.restoreAllMocks(); setViewport(viewport.width, viewport.height); });

it('cancels a short hover and opens only after the complete delay', () => {
  render(<Overlay />);
  const trigger = screen.getByRole('button');
  fireEvent.mouseEnter(trigger);
  act(() => { jest.advanceTimersByTime(250); });
  fireEvent.mouseLeave(trigger);
  act(() => { jest.advanceTimersByTime(500); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  fireEvent.mouseEnter(trigger);
  act(() => { jest.advanceTimersByTime(499); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  act(() => { jest.advanceTimersByTime(1); });
  expect(screen.getByRole('tooltip')).toBeInTheDocument();
});

it('cancels closing when entering the panel and applies the close delay after leaving it', () => {
  render(<Overlay />);
  const trigger = screen.getByRole('button');
  fireEvent.mouseEnter(trigger);
  act(() => { jest.advanceTimersByTime(500); });
  const panel = screen.getByRole('tooltip');
  fireEvent.mouseLeave(trigger);
  act(() => { jest.advanceTimersByTime(100); });
  fireEvent.mouseEnter(panel);
  act(() => { jest.advanceTimersByTime(150); });
  expect(panel).toBeInTheDocument();
  fireEvent.mouseLeave(panel);
  act(() => { jest.advanceTimersByTime(149); });
  expect(panel).toBeInTheDocument();
  act(() => { jest.advanceTimersByTime(1); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it.each(['open', 'close'] as const)('cancels a pending %s action on unmount', (kind) => {
  let actions = 0;
  const { result, unmount } = renderHook(() => useAnchoredOverlay({ open: false,
    anchor: { current: null }, panel: { current: null }, width: 320, openDelay: 500, closeDelay: 150,
  }));
  act(() => {
    const schedule = kind === 'open' ? result.current.scheduleOpen : result.current.scheduleClose;
    schedule(() => { actions += 1; });
  });
  unmount();
  act(() => { jest.advanceTimersByTime(1000); });
  expect(actions).toBe(0);
  expect(jest.getTimerCount()).toBe(0);
});

it('places a constrained card above its anchor and repositions on viewport and scroll changes', () => {
  const anchor = { current: document.createElement('span') };
  const panel = { current: document.createElement('div') };
  let rect = bounds(900, 700, 60, 30);
  jest.spyOn(anchor.current, 'getBoundingClientRect').mockImplementation(() => rect);
  const { result, rerender } = renderHook(({ open }) => useAnchoredOverlay({ open, anchor, panel,
    width: 400, maxHeight: 480, margin: 12, gap: 8, openDelay: 500, closeDelay: 150,
  }), { initialProps: { open: true } });
  expect(result.current.position).toEqual({ width: 400, left: 588, bottom: 108, maxHeight: 480 });
  setViewport(360, 500);
  rect = bounds(300, 420, 40, 30);
  fireEvent(window, new Event('resize'));
  expect(result.current.position).toEqual({ width: 336, left: 12, bottom: 88, maxHeight: 400 });
  rect = bounds(300, 300, 40, 30);
  fireEvent(window, new Event('scroll'));
  expect(result.current.position).toEqual({ width: 336, left: 12, bottom: 208, maxHeight: 280 });
  rerender({ open: false });
  rect = bounds(20, 20, 40, 30);
  fireEvent(window, new Event('scroll'));
  expect(result.current.position).toEqual({ width: 336, left: 12, bottom: 208, maxHeight: 280 });
  rerender({ open: true });
  expect(result.current.position).toEqual({ width: 336, left: 12, top: 58, maxHeight: 430 });
});

it('uses the measured tooltip height and clamps its top and height to the viewport', () => {
  const anchor = { current: document.createElement('span') };
  const panel = { current: document.createElement('div') };
  jest.spyOn(anchor.current, 'getBoundingClientRect').mockReturnValue(bounds(950, 700, 30, 30));
  jest.spyOn(panel.current, 'getBoundingClientRect').mockReturnValue(bounds(0, 0, 320, 200));
  const { result } = renderHook(() => useAnchoredOverlay({ open: true, anchor, panel,
    width: 320, openDelay: 500, closeDelay: 150,
  }));
  expect(result.current.position).toEqual({ width: 320, left: 668, top: 492, maxHeight: 776 });
  setViewport(1000, 180);
  fireEvent(window, new Event('resize'));
  expect(result.current.position).toEqual({ width: 320, left: 668, top: 12, maxHeight: 156 });
});
