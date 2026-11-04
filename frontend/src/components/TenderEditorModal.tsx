import { positiveId } from '../utils/params';
import React, { FormEvent, useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useMutation } from '../hooks/useMutation';
import { EquipmentType, TenderResponse } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';

interface TenderEditorModalProps {
  token: string; open: boolean; tender: TenderResponse | null; hasOffers?: boolean; incidentId?: number;
  onClose: () => void; onSaved: (tender: TenderResponse) => void;
}

export function TenderEditorModal({ token, open, tender, hasOffers, incidentId, onClose, onSaved }: TenderEditorModalProps) {
  const initialTender = useRef(tender);
  initialTender.current = tender;
  const tenderId = tender?.id;
  const [type, setType] = useState<EquipmentType | ''>('');
  const [quantity, setQuantity] = useState('1');
  const [conditions, setConditions] = useState('');
  const [incident, setIncident] = useState('');
  const mutation = useMutation(JSON.stringify([token, open, tender?.id, incidentId]));
  const { busy: saving, error, setError } = mutation;

  useEffect(() => {
    if (open) {
      const value = initialTender.current;
      setType(value?.equipmentType ?? '');
      setQuantity(String(value?.quantity ?? 1));
      setConditions(value?.conditions ?? '');
      setIncident(String(value?.incidentId ?? incidentId ?? ''));
      setError(null);
    }
  }, [open, token, tenderId, incidentId, setError]);

  if (!open) { return null; }
  const close = () => { if (!mutation.isBusy()) { onClose(); } };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) { return; }
    const count = Number(quantity);
    const origin = tender ? tender.incidentId : positiveId(incident);
    if (!type || !Number.isInteger(count) || count < 1 || count > 2147483647 || (!tender && incident && !origin)) {
      setError('Укажите тип, целое положительное количество и корректный номер инцидента');
      return;
    }
    const session = mutation.start();
    if (!session) { return; }
    const payload = { equipmentType: type, quantity: count, conditions: conditions.trim(),
      ...(origin != null ? { incidentId: origin } : {}) };
    try {
      const saved = tender ? await api.updateTender(token, tender.id, payload) : await api.createTender(token, payload);
      if (!session.isCurrent()) { return; }
      onClose();
      onSaved(saved);
    } catch (err: unknown) {
      session.fail(err, 'Не удалось сохранить тендер');
    } finally {
      session.finish();
    }
  };

  return (
    <Modal open={open} title={tender ? 'Редактировать тендер' : 'Создать тендер'} onClose={close} busy={saving}>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      <form className="form" aria-label={tender ? 'Редактировать тендер' : 'Создать тендер'} onSubmit={submit}>
        <FormField label="Тип оборудования" required>
          <select required value={type} disabled={saving || Boolean(tender && hasOffers)}
            onChange={(event) => setType(event.target.value as EquipmentType | '')}>
            <option value="">Выберите тип</option>
            {Object.entries(EQUIPMENT_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </FormField>
        <FormField label="Количество оборудования" required>
          <input type="number" min={1} max={2147483647} step={1} required value={quantity}
            disabled={saving || Boolean(tender && hasOffers)} onChange={(event) => setQuantity(event.target.value)} />
        </FormField>
        {tender && hasOffers && <p className="text-muted">Тип и количество зафиксированы полученными предложениями.</p>}
        <FormField label="Условия закупки"><textarea rows={4} maxLength={4000} value={conditions}
          disabled={saving} onChange={(event) => setConditions(event.target.value)} /></FormField>
        <FormField label="Инцидент-основание" help="Необязательно для закупки на общий склад">
          <input type="number" min={1} step={1} value={incident} disabled={saving || tender != null}
            onChange={(event) => setIncident(event.target.value)} />
        </FormField>
        <div className="modal-actions"><Button variant="secondary" onClick={close} disabled={saving}>Отмена</Button>
          <Button type="submit" loading={saving}>Сохранить</Button></div>
      </form>
    </Modal>
  );
}
