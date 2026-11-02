import React, { FormEvent, useEffect, useId, useRef, useState } from 'react';
import * as api from '../api/client';
import { MOVEMENT_STATUS_LABELS } from '../equipmentLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { EquipmentMovementResponse, EquipmentMovementStatus } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { StatusChip } from './ui/StatusChip';

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
  const [saving, setSaving] = useState(false);
  const busyRef = useRef(false);
  const [error, setError] = useState<string | null>(null);
  const titleId = useId();
  const trapRef = useFocusTrap(target != null);
  const active = movement.status === 'PENDING' || movement.status === 'PROCESSING';
  const options: EquipmentMovementStatus[] = [];
  if (active && !disabled) {
    if (canProcess) { options.push(movement.status === 'PENDING' ? 'PROCESSING' : 'COMPLETED'); }
    if (canProcess || (canCreate && movement.createdByUserId === currentUserId)) { options.push('CANCELLED'); }
    if (canProcess) { options.push('REJECTED'); }
  }

  useEffect(() => {
    if (!target) { return; }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busyRef.current) { setTarget(null); }
    };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [target]);

  const save = async (status: EquipmentMovementStatus, reason?: string) => {
    if (busyRef.current) { return; }
    busyRef.current = true;
    setSaving(true);
    setError(null);

    try {
      const updated = await api.changeEquipmentMovementStatus(token, movement.id,
        { status, ...(reason ? { comment: reason } : {}) });
      onChanged(updated);
      setTarget(null);
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Не удалось изменить статус заявки');
    } finally {
      busyRef.current = false;
      setSaving(false);
    }
  };

  const confirm = (event: FormEvent) => {
    event.preventDefault();
    if (!comment.trim()) { setError('Укажите причину'); return; }
    if (target) { void save(target, comment.trim()); }
  };

  return (
    <div className="equipment-status">
      {options.length > 0 ? <select aria-label="Статус заявки" disabled={saving || disabled}
        className={`status-select status-chip--${movement.status.toLowerCase()}`} value={movement.status}
        onChange={(event) => {
          const next = event.target.value as EquipmentMovementStatus;
          if (!options.includes(next)) { return; }
          setError(null);
          if (next === 'CANCELLED' || next === 'REJECTED') { setComment(''); setTarget(next); }
          else { void save(next); }
        }}>
        <option value={movement.status}>{MOVEMENT_STATUS_LABELS[movement.status]}</option>
        {options.map((status) => <option key={status} value={status}>{MOVEMENT_STATUS_LABELS[status]}</option>)}
      </select> : <StatusChip status={movement.status.toLowerCase()} label={MOVEMENT_STATUS_LABELS[movement.status]} />}
      {saving && <span className="status-select__loading" role="status">Сохранение…</span>}
      {error && !target && <div className="alert alert--error" role="alert">{error}</div>}
      {target && <div className="modal-overlay" role="presentation" onClick={() => { if (!saving) { setTarget(null); } }}>
        <div ref={trapRef} className="modal modal--narrow" role="dialog" aria-modal="true" aria-labelledby={titleId}
          onClick={(event) => event.stopPropagation()}>
          <div className="modal__header"><h2 id={titleId}>{target === 'CANCELLED' ? 'Отменить заявку' : 'Отклонить заявку'}</h2></div>
          {error && <div className="alert alert--error" role="alert">{error}</div>}
          <form className="form" onSubmit={confirm}>
            <FormField label="Причина" required><textarea value={comment} required maxLength={4000} rows={4}
              disabled={saving} onChange={(event) => setComment(event.target.value)} /></FormField>
            <div className="modal-actions">
              <Button variant="secondary" disabled={saving} onClick={() => setTarget(null)}>Отмена</Button>
              <Button type="submit" loading={saving}>Подтвердить</Button>
            </div>
          </form>
        </div>
      </div>}
    </div>
  );
}
