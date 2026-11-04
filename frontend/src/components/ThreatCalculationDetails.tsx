import React, { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { IncidentEventType, IncidentThreatCalculationResponse } from '../types';

const OPEN_DELAY_MS = 500;
const CLOSE_DELAY_MS = 150;
const CARD_WIDTH = 320;
const VIEWPORT_MARGIN = 12;
const ANCHOR_GAP = 8;

interface Props {
  calculation: IncidentThreatCalculationResponse;
  eventType: IncidentEventType;
  disabled?: boolean;
}

function formatAdjustment(value: number): string {
  if (value > 0) { return `+${value}`; }
  if (value < 0) { return `−${Math.abs(value)}`; }
  return '0';
}

export function ThreatCalculationDetails({ calculation, eventType, disabled = false }: Props) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<React.CSSProperties>({});
  const anchor = useRef<HTMLSpanElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const openTimer = useRef<number | undefined>(undefined);
  const closeTimer = useRef<number | undefined>(undefined);
  const tooltipId = useId();

  const cancelTimers = useCallback(() => {
    window.clearTimeout(openTimer.current);
    window.clearTimeout(closeTimer.current);
  }, []);
  const close = useCallback(() => { cancelTimers(); setOpen(false); }, [cancelTimers]);

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') { close(); } };
    document.addEventListener('keydown', onKey);
    return () => {
      cancelTimers();
      document.removeEventListener('keydown', onKey);
    };
  }, [cancelTimers, close]);
  useLayoutEffect(() => {
    if (!open) { return; }
    const place = () => {
      const rect = anchor.current?.getBoundingClientRect();
      if (!rect) { return; }
      const width = Math.min(CARD_WIDTH, window.innerWidth - VIEWPORT_MARGIN * 2);
      const height = panel.current?.getBoundingClientRect().height ?? 0;
      const below = window.innerHeight - rect.bottom - ANCHOR_GAP - VIEWPORT_MARGIN;
      const above = rect.top - ANCHOR_GAP - VIEWPORT_MARGIN;
      const top = height > below && above > below ? rect.top - ANCHOR_GAP - height : rect.bottom + ANCHOR_GAP;
      setPosition({ width, left: Math.max(VIEWPORT_MARGIN,
        Math.min(rect.left, window.innerWidth - width - VIEWPORT_MARGIN)),
      top: Math.max(VIEWPORT_MARGIN, Math.min(top, window.innerHeight - VIEWPORT_MARGIN - height)),
      maxHeight: Math.max(0, window.innerHeight - VIEWPORT_MARGIN * 2) });
    };
    const onOutside = (event: MouseEvent) => {
      if (!anchor.current?.contains(event.target as Node) && !panel.current?.contains(event.target as Node)) { close(); }
    };
    place();
    document.addEventListener('mousedown', onOutside);
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      document.removeEventListener('mousedown', onOutside);
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [open, close]);

  const scheduleOpen = () => {
    cancelTimers();
    openTimer.current = window.setTimeout(() => setOpen(true), OPEN_DELAY_MS);
  };
  const scheduleClose = () => {
    if (anchor.current?.contains(document.activeElement)) { return; }
    cancelTimers();
    closeTimer.current = window.setTimeout(close, CLOSE_DELAY_MS);
  };
  const labels: Record<IncidentThreatCalculationResponse['contributions'][number]['criterion'], string> = {
    ALIEN_THREAT: 'Угроза вида', DEFAULT_THREAT: 'Базовая угроза',
    EVENT_TYPE: EVENT_TYPE_LABELS[eventType], VICTIMS: 'Пострадавшие',
  };
  const sum = calculation.contributions.reduce((value, contribution) => value + contribution.adjustment, 0);

  return <span ref={anchor} onMouseEnter={scheduleOpen} onMouseLeave={scheduleClose} onFocus={scheduleOpen}
    onBlur={() => { cancelTimers(); closeTimer.current = window.setTimeout(close, CLOSE_DELAY_MS); }}>
    <button type="button" className="incident-threat__details-trigger" disabled={disabled}
      aria-label="Показать причины оценки" aria-describedby={open ? tooltipId : undefined}
      onClick={() => { cancelTimers(); setOpen((previous) => !previous); }}>
      <span aria-hidden="true">?</span>
    </button>
    {open && createPortal(<div ref={panel} id={tooltipId} role="tooltip" className="alien-hover-card threat-calculation-popover"
      style={position} onMouseEnter={cancelTimers} onMouseLeave={scheduleClose}>
      <dl className="threat-calculation-popover__reasons">
        {calculation.contributions.map(({ criterion, adjustment }) => <div key={criterion}>
          <dt>{labels[criterion]}</dt><dd>{formatAdjustment(adjustment)}</dd>
        </div>)}
      </dl>
      <div className="threat-calculation-popover__sum"><span>Сумма</span><strong>{sum}</strong></div>
      {sum !== calculation.calculatedThreatLevel && <div className="threat-calculation-popover__bounded text-muted">
        <span>В пределах 1–10</span><strong>{calculation.calculatedThreatLevel}</strong>
      </div>}
    </div>, document.body)}
  </span>;
}
