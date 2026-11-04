import { CSSProperties, RefObject, useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';

interface AnchoredOverlayOptions {
  open: boolean;
  anchor: RefObject<HTMLElement | null>;
  panel: RefObject<HTMLElement | null>;
  width: number;
  /** A constrained card uses available space; an uncapped tooltip uses its measured height. */
  maxHeight?: number;
  margin?: number;
  gap?: number;
  openDelay: number;
  closeDelay: number;
}

/** Owns scheduling and placement only; callers retain focus, keyboard and dismissal semantics. */
export function useAnchoredOverlay({ open, anchor, panel, width: preferredWidth, maxHeight,
  margin = 12, gap = 8, openDelay, closeDelay }: AnchoredOverlayOptions) {
  const [position, setPosition] = useState<CSSProperties>({});
  const openTimer = useRef<number | undefined>(undefined);
  const closeTimer = useRef<number | undefined>(undefined);
  const cancelTimers = useCallback(() => {
    window.clearTimeout(openTimer.current);
    window.clearTimeout(closeTimer.current);
    openTimer.current = undefined;
    closeTimer.current = undefined;
  }, []);
  const scheduleOpen = useCallback((action: () => void) => {
    cancelTimers();
    openTimer.current = window.setTimeout(() => { openTimer.current = undefined; action(); }, openDelay);
  }, [cancelTimers, openDelay]);
  const scheduleClose = useCallback((action: () => void) => {
    cancelTimers();
    closeTimer.current = window.setTimeout(() => { closeTimer.current = undefined; action(); }, closeDelay);
  }, [cancelTimers, closeDelay]);

  useEffect(() => cancelTimers, [cancelTimers]);
  useLayoutEffect(() => {
    if (!open) { return; }
    const place = () => {
      const rect = anchor.current?.getBoundingClientRect();
      if (!rect) { return; }
      const width = Math.min(preferredWidth, window.innerWidth - margin * 2);
      const left = Math.max(margin, Math.min(rect.left, window.innerWidth - width - margin));
      const below = window.innerHeight - rect.bottom - gap - margin;
      const above = rect.top - gap - margin;
      if (maxHeight !== undefined) {
        const useAbove = below < maxHeight && above > below;
        setPosition({ width, left,
          ...(useAbove ? { bottom: window.innerHeight - rect.top + gap } : { top: rect.bottom + gap }),
          maxHeight: Math.max(0, Math.min(maxHeight, useAbove ? above : below)) });
      } else {
        const height = panel.current?.getBoundingClientRect().height ?? 0;
        const top = height > below && above > below ? rect.top - gap - height : rect.bottom + gap;
        setPosition({ width, left, top: Math.max(margin, Math.min(top, window.innerHeight - margin - height)),
          maxHeight: Math.max(0, window.innerHeight - margin * 2) });
      }
    };
    place();
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [open, anchor, panel, preferredWidth, maxHeight, margin, gap]);

  return { position, cancelTimers, scheduleOpen, scheduleClose };
}
