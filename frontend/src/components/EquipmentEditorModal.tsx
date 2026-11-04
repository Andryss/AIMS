import React, { FormEvent, useEffect, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useMutation } from '../hooks/useMutation';
import { EquipmentResponse, EquipmentType, SaveEquipmentRequest } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';

interface EquipmentEditorModalProps {
  token: string;
  open: boolean;
  equipment: EquipmentResponse | null;
  onClose: () => void;
  onSaved: (equipment: EquipmentResponse) => void;
}

export function EquipmentEditorModal({ token, open, equipment, onClose, onSaved }: EquipmentEditorModalProps) {
  const [type, setType] = useState<EquipmentType | ''>('');
  const [model, setModel] = useState('');
  const [serialNumber, setSerialNumber] = useState('');
  const mutation = useMutation(JSON.stringify([token, open, equipment?.id]));
  const { busy: submitting, error, setError } = mutation;

  useEffect(() => {
    if (open) {
      setType(equipment?.type ?? '');
      setModel(equipment?.model ?? '');
      setSerialNumber(equipment?.serialNumber ?? '');
      setError(null);
    }
  }, [open, token, equipment, setError]);

  if (!open) { return null; }

  const close = () => { if (!mutation.isBusy()) { onClose(); } };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) { return; }

    if (!type || !model.trim() || !serialNumber.trim()) {
      setError('Укажите тип, модель и серийный номер');
      return;
    }

    const payload: SaveEquipmentRequest = { type, model: model.trim(), serialNumber: serialNumber.trim() };
    const session = mutation.start();
    if (!session) { return; }

    try {
      const saved = equipment
        ? await api.updateEquipment(token, equipment.id, payload)
        : await api.createEquipment(token, payload);
      if (!session.isCurrent()) { return; }
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      session.fail(err, 'Не удалось сохранить оборудование');
    } finally {
      session.finish();
    }
  };

  return (
    <Modal open={open} title={equipment ? 'Редактировать оборудование' : 'Добавить оборудование'} onClose={close} busy={submitting}>
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
    </Modal>
  );
}
