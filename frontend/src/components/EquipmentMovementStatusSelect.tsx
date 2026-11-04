import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { MOVEMENT_STATUS_LABELS } from '../equipmentLabels';
import { equipmentMovementTransitions } from '../domain/equipmentMovementTransitions';
import { useMutation } from '../hooks/useMutation';
import { EquipmentMovementResponse, EquipmentMovementStatus } from '../types';
import { ConfirmReasonDialog } from './ui/ConfirmReasonDialog';
import { StatusChip } from './ui/StatusChip';
import { StatusSelect } from './ui/StatusSelect';

interface EquipmentMovementStatusSelectProps {
  token: string;
  movement: EquipmentMovementResponse;
  canCreate: boolean;
  canProcess: boolean;
  currentUserId?: number;
  disabled?: boolean;
  onChanged: (movement: EquipmentMovementResponse) => void;
}

export function EquipmentMovementStatusSelect({ token, movement, canCreate, canProcess, currentUserId,
  disabled, onChanged }: EquipmentMovementStatusSelectProps) {
  const [target, setTarget] = useState<EquipmentMovementStatus | null>(null);
  const [comment, setComment] = useState('');
  const scopeKey = JSON.stringify([token, movement.id, movement.status]);
  const mutation = useMutation(scopeKey);
  const { busy: saving, error, setError } = mutation;
  const options = equipmentMovementTransitions(movement, canCreate, canProcess, currentUserId, disabled);
  useEffect(() => { setTarget(null); setComment(''); }, [scopeKey]);
  const close = () => { if (!mutation.isBusy()) { setTarget(null); } };

  const save = async (status: EquipmentMovementStatus, reason?: string) => {
    if (!options.includes(status)) { return; }
    const session = mutation.start();
    if (!session) { return; }
    try {
      const updated = await api.changeEquipmentMovementStatus(token, movement.id,
        { status, ...(reason ? { comment: reason } : {}) });
      if (!session.isCurrent()) { return; }
      onChanged(updated);
      setTarget(null);
    } catch (err: unknown) {
      session.fail(err, 'Не удалось изменить статус заявки');
    } finally {
      session.finish();
    }
  };

  return <div className="equipment-status">
    {options.length > 0 ? <StatusSelect label="Статус заявки" disabled={disabled} busy={saving}
      value={movement.status} options={options} labels={MOVEMENT_STATUS_LABELS} onChange={(next) => {
        setError(null);
        if (next === 'CANCELLED' || next === 'REJECTED') { setComment(''); setTarget(next); }
        else { void save(next); }
      }} /> : <StatusChip status={movement.status.toLowerCase()} label={MOVEMENT_STATUS_LABELS[movement.status]} />}
    {saving && <span className="status-select__loading" role="status">Сохранение…</span>}
    {error && !target && <div className="alert alert--error" role="alert">{error}</div>}
    <ConfirmReasonDialog open={target != null} title={target === 'CANCELLED' ? 'Отменить заявку' : 'Отклонить заявку'}
      busy={saving} error={error} reason={comment} onReasonChange={setComment} onClose={close}
      onConfirm={() => {
        if (!comment.trim()) { setError('Укажите причину'); return; }
        if (target) { void save(target, comment.trim()); }
      }} />
  </div>;
}
