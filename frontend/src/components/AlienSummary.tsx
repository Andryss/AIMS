import React, { useCallback, useEffect, useId, useLayoutEffect, useRef, useState } from 'react';
import { createPortal } from 'react-dom';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { Alien } from '../types';
import { AttachmentDownloadList } from './AttachmentDownloadList';

const OPEN_DELAY_MS = 500;
const CLOSE_DELAY_MS = 150;
const CARD_WIDTH = 400;
const CARD_MAX_HEIGHT = 480;
const VIEWPORT_MARGIN = 12;
const ANCHOR_GAP = 8;

interface AlienSummaryProps { token: string; alien: Alien; incidentId?: number; }

export function AlienSummary({ token, alien, incidentId }: AlienSummaryProps) {
  const [open, setOpen] = useState(false);
  const [position, setPosition] = useState<React.CSSProperties>({});
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const anchor = useRef<HTMLSpanElement>(null);
  const panel = useRef<HTMLDivElement>(null);
  const trigger = useRef<HTMLButtonElement>(null);
  const focusOnOpen = useRef(false);
  const suppressFocusOpen = useRef(false);
  const openTimer = useRef<number | undefined>(undefined);
  const closeTimer = useRef<number | undefined>(undefined);
  const generation = useRef(0);
  const dialogId = useId();

  const cancelTimers = useCallback(() => {
    window.clearTimeout(openTimer.current);
    window.clearTimeout(closeTimer.current);
  }, []);

  const close = useCallback(() => {
    cancelTimers();
    generation.current += 1;
    setOpen(false);
    setError(null);
    setDownloadingId(null);
    if (panel.current?.contains(document.activeElement)) {
      suppressFocusOpen.current = true;
      trigger.current?.focus({ preventScroll: true });
    }
  }, [cancelTimers]);

  useEffect(() => {
    close();
    return () => { cancelTimers(); generation.current += 1; };
  }, [alien.id, token, close, cancelTimers]);

  useLayoutEffect(() => {
    if (!open) { return; }
    const place = () => {
      const rect = anchor.current?.getBoundingClientRect();
      if (!rect) { return; }
      const width = Math.min(CARD_WIDTH, window.innerWidth - VIEWPORT_MARGIN * 2);
      const below = window.innerHeight - rect.bottom - ANCHOR_GAP - VIEWPORT_MARGIN;
      const above = rect.top - ANCHOR_GAP - VIEWPORT_MARGIN;
      const useAbove = below < CARD_MAX_HEIGHT && above > below;
      setPosition({ width, left: Math.max(VIEWPORT_MARGIN,
        Math.min(rect.left, window.innerWidth - width - VIEWPORT_MARGIN)),
      ...(useAbove ? { bottom: window.innerHeight - rect.top + ANCHOR_GAP }
        : { top: rect.bottom + ANCHOR_GAP }),
      maxHeight: Math.max(0, Math.min(CARD_MAX_HEIGHT, useAbove ? above : below)) });
    };
    const onKey = (event: KeyboardEvent) => { if (event.key === 'Escape') { close(); } };
    const onOutside = (event: MouseEvent) => {
      if (!anchor.current?.contains(event.target as Node) && !panel.current?.contains(event.target as Node)) { close(); }
    };
    place();
    if (focusOnOpen.current) {
      focusOnOpen.current = false;
      panel.current?.querySelector<HTMLButtonElement>('button')?.focus({ preventScroll: true });
    }
    document.addEventListener('keydown', onKey);
    document.addEventListener('mousedown', onOutside);
    window.addEventListener('resize', place);
    window.addEventListener('scroll', place, true);
    return () => {
      document.removeEventListener('keydown', onKey);
      document.removeEventListener('mousedown', onOutside);
      window.removeEventListener('resize', place);
      window.removeEventListener('scroll', place, true);
    };
  }, [open, close]);

  const scheduleOpen = () => {
    if (suppressFocusOpen.current) { suppressFocusOpen.current = false; return; }
    cancelTimers();
    openTimer.current = window.setTimeout(() => setOpen(true), OPEN_DELAY_MS);
  };
  const scheduleClose = () => {
    if (anchor.current?.contains(document.activeElement) || panel.current?.contains(document.activeElement)) { return; }
    cancelTimers();
    closeTimer.current = window.setTimeout(close, CLOSE_DELAY_MS);
  };
  const keepOpen = () => { cancelTimers(); };
  const onBlur = (event: React.FocusEvent) => {
    const target = event.relatedTarget as Node | null;
    if (!anchor.current?.contains(target) && !panel.current?.contains(target)) { scheduleClose(); }
  };
  const download = async (id: number, fileName?: string) => {
    const current = generation.current;
    setDownloadingId(id);
    try { await api.downloadFile(token, id, fileName ?? `attachment-${id}`); }
    catch (err) { if (current === generation.current) { setError(err instanceof Error ? err.message : 'Не удалось скачать фото'); } }
    finally { if (current === generation.current) { setDownloadingId(null); } }
  };

  return <span ref={anchor} className="alien-summary" onMouseEnter={scheduleOpen} onMouseLeave={scheduleClose}
    onFocus={(event) => { if (anchor.current?.contains(event.target as Node)) { scheduleOpen(); } }} onBlur={onBlur}>
    <button ref={trigger} type="button" className="alien-summary__trigger" aria-label={`Подробнее о виде ${alien.name}`}
      aria-expanded={open} aria-controls={open ? dialogId : undefined} aria-haspopup="dialog"
      onKeyDown={(event) => {
        if (event.key !== 'ArrowDown') { return; }
        event.preventDefault();
        cancelTimers();
        if (open) { panel.current?.querySelector<HTMLButtonElement>('button')?.focus({ preventScroll: true }); }
        else { focusOnOpen.current = true; setOpen(true); }
      }}
      onClick={(event) => {
        cancelTimers();
        if (open) { close(); }
        else { focusOnOpen.current = event.detail === 0; setOpen(true); }
      }}>
      <strong>{alien.name}</strong><span className="text-muted">(угроза {alien.threatLevel})</span>
    </button>
    <Link to={`/aliens/${alien.id}`} state={incidentId == null ? undefined : { incidentId }}
      className="btn btn--outline icon-button alien-summary__link" title="Открыть карточку вида"
      aria-label="Открыть карточку вида">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
        strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <path d="M14 3h7v7M21 3 10 14M10 3H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-5" />
      </svg>
    </Link>
    {open && createPortal(<div ref={panel} id={dialogId} role="dialog" aria-label={`Вид: ${alien.name}`}
      className="alien-hover-card" style={position} onMouseEnter={keepOpen} onMouseLeave={scheduleClose}
      onFocus={keepOpen} onBlur={onBlur}>
      <div className="alien-hover-card__heading"><h3>{alien.name}</h3>
        <button type="button" className="btn btn--ghost btn--sm" aria-label="Закрыть сведения о виде" onClick={close}>×</button>
      </div>
      <dl className="alien-hover-card__details">
        <div><dt>ID</dt><dd>#{alien.id}</dd></div>
        <div><dt>Уровень угрозы</dt><dd>{alien.threatLevel}</dd></div>
      </dl>
      {(alien.photoFileIds?.length ?? 0) > 0 && <section aria-label="Фотографии вида">
        <AttachmentDownloadList token={token} fileIds={alien.photoFileIds ?? []}
          downloadingId={downloadingId} onDownload={download} />
      </section>}
      <p className="text-pre-wrap alien-hover-card__description">{alien.description}</p>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
    </div>, document.body)}
  </span>;
}
