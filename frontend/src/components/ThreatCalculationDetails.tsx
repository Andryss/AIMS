import React, { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { IncidentEventType, IncidentThreatCalculationResponse } from '../types';
import { useAnchoredOverlay } from '../hooks/useAnchoredOverlay';

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
  const anchor = useRef<HTMLSpanElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const tooltipId = useId();

  const { position, cancelTimers, scheduleOpen: delayOpen, scheduleClose: delayClose } = useAnchoredOverlay({
    open, anchor, panel, width: CARD_WIDTH, margin: VIEWPORT_MARGIN, gap: ANCHOR_GAP,
    openDelay: OPEN_DELAY_MS, closeDelay: CLOSE_DELAY_MS,
  });
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
    const onOutside = (event: MouseEvent) => {
      if (!anchor.current?.contains(event.target as Node) && !panel.current?.contains(event.target as Node)) { close(); }
    };
    document.addEventListener('mousedown', onOutside);
    return () => {
      document.removeEventListener('mousedown', onOutside);
    };
  }, [open, close]);

  const scheduleOpen = () => {
    delayOpen(() => setOpen(true));
  };
  const scheduleClose = () => {
    if (anchor.current?.contains(document.activeElement)) { return; }
    delayClose(close);
  };
  const labels: Record<IncidentThreatCalculationResponse['contributions'][number]['criterion'], string> = {
    ALIEN_THREAT: 'Угроза вида', DEFAULT_THREAT: 'Базовая угроза',
    EVENT_TYPE: EVENT_TYPE_LABELS[eventType], VICTIMS: 'Пострадавшие',
  };
  const sum = calculation.contributions.reduce((value, contribution) => value + contribution.adjustment, 0);

  return <span ref={anchor} onMouseEnter={scheduleOpen} onMouseLeave={scheduleClose} onFocus={scheduleOpen}
    onBlur={() => delayClose(close)}>
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
