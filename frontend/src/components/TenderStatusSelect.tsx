import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { tenderTransitions } from '../domain/tenderTransitions';
import { useMutation } from '../hooks/useMutation';
import { TENDER_STATUS_LABELS } from '../tenderLabels';
import { TenderResponse, TenderStatus } from '../types';
import { ConfirmReasonDialog } from './ui/ConfirmReasonDialog';
import { StatusChip } from './ui/StatusChip';
import { StatusSelect } from './ui/StatusSelect';

interface TenderStatusSelectProps {
  token: string; tender: TenderResponse; permissions: string[]; currentUserId?: number;
  onChanged: (tender: TenderResponse) => void;
}

export function TenderStatusSelect({ token, tender, permissions, currentUserId, onChanged }: TenderStatusSelectProps) {
  const [cancelling, setCancelling] = useState(false);
  const [comment, setComment] = useState('');
  const scopeKey = JSON.stringify([token, tender.id, tender.status]);
  const mutation = useMutation(scopeKey);
  const { busy: saving, error, setError } = mutation;
  const options = tenderTransitions(tender, permissions, currentUserId);
  useEffect(() => { setCancelling(false); setComment(''); }, [scopeKey]);
  const close = () => { if (!mutation.isBusy()) { setCancelling(false); } };
  const save = async (status: TenderStatus, reason?: string) => {
    if (!options.includes(status)) { return; }
    const session = mutation.start();
    if (!session) { return; }
    try {
      const saved = await api.changeTenderStatus(token, tender.id, { status, ...(reason ? { comment: reason } : {}) });
      if (!session.isCurrent()) { return; }
      onChanged(saved);
      setCancelling(false);
    } catch (err: unknown) {
      session.fail(err, 'Не удалось изменить статус тендера');
    } finally {
      session.finish();
    }
  };

  return <div className="equipment-status">
    {options.length ? <StatusSelect label="Статус тендера" busy={saving} value={tender.status} options={options}
      labels={TENDER_STATUS_LABELS} onChange={(next) => {
        if (next === 'CANCELLED') { setComment(''); setError(null); setCancelling(true); }
        else { void save(next); }
      }} /> : <StatusChip status={tender.status.toLowerCase()} label={TENDER_STATUS_LABELS[tender.status]} />}
    {saving && <span role="status">Сохранение…</span>}
    {error && !cancelling && <div className="alert alert--error" role="alert">{error}</div>}
    <ConfirmReasonDialog open={cancelling} title="Отменить тендер" busy={saving} reason={comment}
      error={error} onReasonChange={setComment} onClose={close}
      onConfirm={() => {
        if (!comment.trim()) { setError('Укажите причину'); return; }
        void save('CANCELLED', comment.trim());
      }} />
  </div>;
}
