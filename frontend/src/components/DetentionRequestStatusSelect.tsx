import React, { FormEvent, useEffect, useId, useRef, useState } from 'react';
import * as api from '../api/client';
import { DETENTION_STATUS_LABELS, detentionTransitions } from '../detentionLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { DetentionRequestResponse, DetentionRequestStatus } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { StatusChip } from './ui/StatusChip';

interface DetentionRequestStatusSelectProps {
  token: string; request: DetentionRequestResponse; permissions: string[]; currentUserId?: number;
  disabled?: boolean; onSavingChange?: (saving: boolean) => void;
  onChanged: (request: DetentionRequestResponse) => void;
}

export function DetentionRequestStatusSelect({ token, request, permissions, currentUserId, disabled = false,
  onSavingChange, onChanged }: DetentionRequestStatusSelectProps) {
  const titleId = useId();
  const [target, setTarget] = useState<DetentionRequestStatus | null>(null);
  const trapRef = useFocusTrap(target != null);
  const scope = useRef(0);
  const busy = useRef(false);
  const [comment, setComment] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const options = detentionTransitions(request, permissions, currentUserId);

  useEffect(() => {
    scope.current += 1; busy.current = false; setSaving(false); setTarget(null); setError(null);
    return () => { scope.current += 1; onSavingChange?.(false); };
  }, [token, request.id, request.status, onSavingChange]);

  useEffect(() => {
    if (target == null) { return; }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape' && !busy.current) { setTarget(null); } };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [target]);

  const save = async (status: DetentionRequestStatus, reason?: string) => {
    if (busy.current || disabled || !options.includes(status)) { return; }
    const version = scope.current;
    busy.current = true; setSaving(true); setError(null);
    onSavingChange?.(true);
    try {
      const saved = await api.changeDetentionRequestStatus(token, request.id, { status, ...(reason ? { comment: reason } : {}) });
      if (scope.current !== version) { return; }
      onChanged(saved); setTarget(null);
    } catch (err: unknown) {
      if (scope.current === version) {
        setError(err instanceof Error ? err.message : 'Не удалось изменить статус заявки');
        if (status === 'RELEASED' && !reason && (err as { status?: number } | null)?.status === 400) {
          setTarget('RELEASED'); setComment('');
        }
      }
    } finally {
      if (scope.current === version) { busy.current = false; setSaving(false); onSavingChange?.(false); }
    }
  };
  const confirm = (event: FormEvent) => {
    event.preventDefault();
    if (!comment.trim()) { setError('Укажите причину'); return; }
    if (target) { void save(target, comment.trim()); }
  };

  return <div className="equipment-status">
    {options.length ? <select aria-label="Статус заявки" value={request.status} disabled={saving || disabled}
      className={`status-select status-chip--${request.status.toLowerCase()}`} onChange={(event) => {
        const next = event.target.value as DetentionRequestStatus;
        if (!options.includes(next)) { return; }
        const earlyRelease = next === 'RELEASED' && (!request.plannedReleaseAt || Date.parse(request.plannedReleaseAt) > Date.now());
        if (next === 'CANCELLED' || next === 'REJECTED' || earlyRelease) { setTarget(next); setComment(''); setError(null); }
        else { void save(next); }
      }}>
      <option value={request.status}>{DETENTION_STATUS_LABELS[request.status]}</option>
      {options.map((status) => <option key={status} value={status}>{DETENTION_STATUS_LABELS[status]}</option>)}
    </select> : <StatusChip status={request.status.toLowerCase()} label={DETENTION_STATUS_LABELS[request.status]} />}
    {request.status === 'ACCEPTED' && <span className="text-muted">Ожидается прибытие</span>}
    {saving && <span role="status">Сохранение…</span>}
    {error && !target && <div className="alert alert--error" role="alert">{error}</div>}
    {target && options.includes(target) && <div className="modal-overlay" role="presentation" onClick={() => { if (!busy.current) { setTarget(null); } }}>
      <div className="modal modal--narrow" ref={trapRef} role="dialog" aria-modal="true" aria-labelledby={titleId}
        onClick={(event) => event.stopPropagation()}>
        <div className="modal__header"><h2 id={titleId}>{target === 'CANCELLED' ? 'Отменить заявку'
          : target === 'REJECTED' ? 'Отклонить заявку' : 'Досрочное освобождение'}</h2></div>
        {error && <div className="alert alert--error" role="alert">{error}</div>}
        <form className="form" onSubmit={confirm}>
          <FormField label="Причина" required><textarea required rows={4} maxLength={4000} value={comment}
            disabled={saving} onChange={(event) => setComment(event.target.value)} /></FormField>
          <div className="modal-actions"><Button variant="secondary" disabled={saving} onClick={() => setTarget(null)}>Отмена</Button>
            <Button type="submit" loading={saving}>Подтвердить</Button></div>
        </form>
      </div>
    </div>}
  </div>;
}
