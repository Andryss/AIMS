import React, { FormEvent, useEffect, useId, useRef, useState } from 'react';
import * as api from '../api/client';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { TENDER_STATUS_LABELS } from '../tenderLabels';
import { TenderResponse, TenderStatus } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { StatusChip } from './ui/StatusChip';

interface TenderStatusSelectProps {
  token: string; tender: TenderResponse; permissions: string[]; currentUserId?: number;
  onChanged: (tender: TenderResponse) => void;
}

export function TenderStatusSelect({ token, tender, permissions, currentUserId, onChanged }: TenderStatusSelectProps) {
  const titleId = useId();
  const [cancelling, setCancelling] = useState(false);
  const trapRef = useFocusTrap(cancelling);
  const busy = useRef(false);
  const scope = useRef(0);
  const [comment, setComment] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const manager = permissions.includes('TENDER_MANAGE');
  const ownNeed = permissions.includes('TENDER_EDIT') && tender.status === 'NEED' && tender.createdByUserId === currentUserId;
  const options: TenderStatus[] = [];
  if (manager && tender.status === 'NEED') { options.push('COLLECTING_OFFERS'); }
  if ((manager || ownNeed) && tender.status !== 'COMPLETED' && tender.status !== 'CANCELLED') { options.push('CANCELLED'); }

  useEffect(() => {
    scope.current += 1;
    return () => { scope.current += 1; };
  }, [token, tender.id]);

  useEffect(() => {
    if (!cancelling) { return; }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape' && !busy.current) { setCancelling(false); } };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [cancelling]);

  const save = async (status: TenderStatus, reason?: string) => {
    if (busy.current || !options.includes(status)) { return; }
    const version = scope.current;
    busy.current = true;
    setSaving(true);
    setError(null);
    try {
      const saved = await api.changeTenderStatus(token, tender.id, { status, ...(reason ? { comment: reason } : {}) });
      if (scope.current !== version) { return; }
      onChanged(saved);
      setCancelling(false);
    } catch (err: unknown) {
      if (scope.current === version) { setError(err instanceof Error ? err.message : 'Не удалось изменить статус тендера'); }
    } finally {
      if (scope.current === version) { busy.current = false; setSaving(false); }
    }
  };
  const confirm = (event: FormEvent) => {
    event.preventDefault();
    if (!comment.trim()) { setError('Укажите причину'); return; }
    void save('CANCELLED', comment.trim());
  };

  return <div className="equipment-status">
    {options.length ? <select aria-label="Статус тендера" disabled={saving} value={tender.status}
      className={`status-select status-chip--${tender.status.toLowerCase()}`} onChange={(event) => {
        const next = event.target.value as TenderStatus;
        if (!options.includes(next)) { return; }
        if (next === 'CANCELLED') { setComment(''); setError(null); setCancelling(true); }
        else { void save(next); }
      }}>
      <option value={tender.status}>{TENDER_STATUS_LABELS[tender.status]}</option>
      {options.map((status) => <option key={status} value={status}>{TENDER_STATUS_LABELS[status]}</option>)}
    </select> : <StatusChip status={tender.status.toLowerCase()} label={TENDER_STATUS_LABELS[tender.status]} />}
    {saving && <span role="status">Сохранение…</span>}
    {error && !cancelling && <div className="alert alert--error" role="alert">{error}</div>}
    {cancelling && <div className="modal-overlay" role="presentation" onClick={() => { if (!busy.current) { setCancelling(false); } }}>
      <div className="modal modal--narrow" ref={trapRef} role="dialog" aria-modal="true" aria-labelledby={titleId}
        onClick={(event) => event.stopPropagation()}>
        <div className="modal__header"><h2 id={titleId}>Отменить тендер</h2></div>
        {error && <div className="alert alert--error" role="alert">{error}</div>}
        <form className="form" onSubmit={confirm}>
          <FormField label="Причина" required><textarea rows={4} maxLength={4000} required value={comment}
            disabled={saving} onChange={(event) => setComment(event.target.value)} /></FormField>
          <div className="modal-actions"><Button variant="secondary" disabled={saving} onClick={() => setCancelling(false)}>Отмена</Button>
            <Button type="submit" loading={saving}>Подтвердить</Button></div>
        </form>
      </div>
    </div>}
  </div>;
}
