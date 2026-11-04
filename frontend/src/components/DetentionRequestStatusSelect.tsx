import React, { useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { DETENTION_STATUS_LABELS } from '../detentionLabels';
import { detentionTransitions } from '../domain/detentionTransitions';
import { useMutation } from '../hooks/useMutation';
import { DetentionRequestResponse, DetentionRequestStatus } from '../types';
import { ConfirmReasonDialog } from './ui/ConfirmReasonDialog';
import { StatusChip } from './ui/StatusChip';
import { StatusSelect } from './ui/StatusSelect';

interface DetentionRequestStatusSelectProps {
  token: string; request: DetentionRequestResponse; permissions: string[]; currentUserId?: number;
  disabled?: boolean; onSavingChange?: (saving: boolean) => void;
  onChanged: (request: DetentionRequestResponse) => void;
}

export function DetentionRequestStatusSelect({ token, request, permissions, currentUserId, disabled = false,
  onSavingChange, onChanged }: DetentionRequestStatusSelectProps) {
  const [target, setTarget] = useState<DetentionRequestStatus | null>(null);
  const [comment, setComment] = useState('');
  const scopeKey = JSON.stringify([token, request.id, request.status]);
  const mutation = useMutation(scopeKey);
  const { busy: saving, error, setError } = mutation;
  const savingCallback = useRef(onSavingChange);
  savingCallback.current = onSavingChange;
  const options = detentionTransitions(request, permissions, currentUserId);
  useEffect(() => {
    setTarget(null); setComment('');
    return () => { savingCallback.current?.(false); };
  }, [scopeKey]);
  const close = () => { if (!mutation.isBusy()) { setTarget(null); } };

  const save = async (status: DetentionRequestStatus, reason?: string) => {
    if (disabled || !options.includes(status)) { return; }
    const session = mutation.start();
    if (!session) { return; }
    onSavingChange?.(true);
    try {
      const saved = await api.changeDetentionRequestStatus(token, request.id, { status, ...(reason ? { comment: reason } : {}) });
      if (!session.isCurrent()) { return; }
      onChanged(saved); setTarget(null);
    } catch (err: unknown) {
      session.fail(err, 'Не удалось изменить статус заявки');
      if (session.isCurrent() && status === 'RELEASED' && !reason && (err as { status?: number } | null)?.status === 400) {
        setTarget('RELEASED'); setComment('');
      }
    } finally {
      if (session.isCurrent()) { session.finish(); savingCallback.current?.(false); }
    }
  };

  return <div className="equipment-status">
    {options.length ? <StatusSelect label="Статус заявки" value={request.status} disabled={disabled} busy={saving}
      options={options} labels={DETENTION_STATUS_LABELS} onChange={(next) => {
        const earlyRelease = next === 'RELEASED' && (!request.plannedReleaseAt || Date.parse(request.plannedReleaseAt) > Date.now());
        if (next === 'CANCELLED' || next === 'REJECTED' || earlyRelease) { setTarget(next); setComment(''); setError(null); }
        else { void save(next); }
      }} /> : <StatusChip status={request.status.toLowerCase()} label={DETENTION_STATUS_LABELS[request.status]} />}
    {request.status === 'ACCEPTED' && <span className="text-muted">Ожидается прибытие</span>}
    {saving && <span role="status">Сохранение…</span>}
    {error && !target && <div className="alert alert--error" role="alert">{error}</div>}
    <ConfirmReasonDialog open={target != null && options.includes(target)} busy={saving} error={error}
      title={target === 'CANCELLED' ? 'Отменить заявку' : target === 'REJECTED' ? 'Отклонить заявку' : 'Досрочное освобождение'}
      reason={comment} onReasonChange={setComment} onClose={close}
      onConfirm={() => {
        if (!comment.trim()) { setError('Укажите причину'); return; }
        if (target) { void save(target, comment.trim()); }
      }} />
  </div>;
}
