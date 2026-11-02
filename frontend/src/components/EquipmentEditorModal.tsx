import React, { FormEvent, useEffect, useId, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { EquipmentResponse, EquipmentType, SaveEquipmentRequest } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

interface EquipmentEditorModalProps {
  token: string;
  open: boolean;
  equipment: EquipmentResponse | null;
  onClose: () => void;
  onSaved: (equipment: EquipmentResponse) => void;
}

export function EquipmentEditorModal({ token, open, equipment, onClose, onSaved }: EquipmentEditorModalProps) {
  const titleId = useId();
  const trapRef = useFocusTrap(open);
  const [type, setType] = useState<EquipmentType | ''>('');
  const [model, setModel] = useState('');
  const [serialNumber, setSerialNumber] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setType(equipment?.type ?? '');
      setModel(equipment?.model ?? '');
      setSerialNumber(equipment?.serialNumber ?? '');
      setError(null);
    }
  }, [open, equipment]);

  useEffect(() => {
    if (!open) { return; }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !submitting) { onClose(); }
    };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [open, submitting, onClose]);

  if (!open) { return null; }

  const close = () => { if (!submitting) { onClose(); } };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (submitting) { return; }

    if (!type || !model.trim() || !serialNumber.trim()) {
      setError('Укажите тип, модель и серийный номер');
      return;
    }

    const payload: SaveEquipmentRequest = { type, model: model.trim(), serialNumber: serialNumber.trim() };
    setSubmitting(true);
    setError(null);

    try {
      const saved = equipment
        ? await api.updateEquipment(token, equipment.id, payload)
        : await api.createEquipment(token, payload);
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Не удалось сохранить оборудование');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" role="presentation" onClick={close}>
      <div className="modal" ref={trapRef} role="dialog" aria-modal="true" aria-labelledby={titleId}
        onClick={(event) => event.stopPropagation()}>
        <div className="modal__header">
          <h2 id={titleId}>{equipment ? 'Редактировать оборудование' : 'Добавить оборудование'}</h2>
          <Button variant="ghost" aria-label="Закрыть" onClick={close} disabled={submitting}>×</Button>
        </div>
        {error && <div className="alert alert--error" role="alert">{error}</div>}
        <form className="form" onSubmit={submit}>
          <FormField label="Тип оборудования" required>
            <select value={type} onChange={(event) => setType(event.target.value as EquipmentType | '')}
              required disabled={submitting}>
              <option value="">Выберите тип</option>
              {Object.entries(EQUIPMENT_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
            </select>
          </FormField>
          <FormField label="Модель / название" required>
            <input value={model} onChange={(event) => setModel(event.target.value)} maxLength={200}
              required disabled={submitting} />
          </FormField>
          <FormField label="Серийный номер" required>
            <input value={serialNumber} onChange={(event) => setSerialNumber(event.target.value)} maxLength={100}
              required disabled={submitting} />
          </FormField>
          <div className="modal-actions">
            <Button variant="secondary" disabled={submitting} onClick={close}>Отмена</Button>
            <Button type="submit" loading={submitting}>Сохранить</Button>
          </div>
        </form>
      </div>
    </div>
  );
}
