import React, { FormEvent, useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useMutation } from '../hooks/useMutation';
import { TenderReceiptResponse, TenderResponse } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';

const MAX_INITIAL_ROWS = 10;
interface ReceiptRow { key: number; model: string; serialNumber: string; }
interface TenderReceiptModalProps {
  token: string; open: boolean; tender: TenderResponse;
  onClose: () => void; onSaved: (response: TenderReceiptResponse) => void;
}

export function TenderReceiptModal({ token, open, tender, onClose, onSaved }: TenderReceiptModalProps) {
  const nextKey = useRef(0);
  const [rows, setRows] = useState<ReceiptRow[]>([]);
  const [comment, setComment] = useState('');
  const mutation = useMutation(JSON.stringify([token, open, tender.id, tender.quantity]));
  const { busy: saving, error, setError } = mutation;

  useEffect(() => {
    if (open) {
      setRows(Array.from({ length: Math.min(tender.quantity, MAX_INITIAL_ROWS) }, () =>
        ({ key: nextKey.current++, model: '', serialNumber: '' })));
      setComment('');
      setError(null);
    }
  }, [open, token, tender.id, tender.quantity, setError]);

  if (!open) { return null; }
  const close = () => { if (!mutation.isBusy()) { onClose(); } };
  const editRow = (key: number, field: 'model' | 'serialNumber', value: string) => {
    setRows((previous) => previous.map((row) => row.key === key ? { ...row, [field]: value } : row));
  };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) { return; }
    if (rows.length !== tender.quantity) { setError(`Необходимо указать ${tender.quantity} экземпляров`); return; }
    const equipment = rows.map(({ model, serialNumber }) => ({ model: model.trim(), serialNumber: serialNumber.trim() }));
    if (equipment.some((item) => !item.model || !item.serialNumber)) { setError('Заполните модель и серийный номер каждого экземпляра'); return; }
    if (new Set(equipment.map((item) => item.serialNumber.toLowerCase())).size !== equipment.length) {
      setError('Серийные номера не должны повторяться'); return;
    }
    const session = mutation.start();
    if (!session) { return; }
    try {
      const response = await api.receiveTender(token, tender.id, { equipment, ...(comment.trim() ? { comment: comment.trim() } : {}) });
      if (!session.isCurrent()) { return; }
      onSaved(response);
      onClose();
    } catch (err: unknown) {
      session.fail(err, 'Не удалось принять поставку');
    } finally {
      session.finish();
    }
  };

  return (
    <Modal open={open} title="Приёмка поставки" onClose={close} busy={saving}>
      <p>{EQUIPMENT_TYPE_LABELS[tender.equipmentType]} · Необходимо принять: {tender.quantity}</p>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      <form className="form" onSubmit={submit}>
        {rows.map((row, index) => <fieldset key={row.key} className="tender-receipt-row" disabled={saving}>
          <legend>Экземпляр {index + 1}</legend>
          <FormField label={`Модель ${index + 1}`}><input required maxLength={200} value={row.model}
            onChange={(event) => editRow(row.key, 'model', event.target.value)} /></FormField>
          <FormField label={`Серийный номер ${index + 1}`}><input required maxLength={100} value={row.serialNumber}
            onChange={(event) => editRow(row.key, 'serialNumber', event.target.value)} /></FormField>
          <Button variant="ghost" aria-label={`Удалить экземпляр ${index + 1}`} disabled={saving}
            onClick={() => setRows((previous) => previous.filter((item) => item.key !== row.key))}>Удалить</Button>
        </fieldset>)}
        <Button variant="outline" disabled={saving || rows.length >= tender.quantity}
          onClick={() => setRows((previous) => [...previous, { key: nextKey.current++, model: '', serialNumber: '' }])}>
          Добавить экземпляр
        </Button>
        <FormField label="Комментарий"><textarea rows={3} maxLength={4000} value={comment} disabled={saving}
          onChange={(event) => setComment(event.target.value)} /></FormField>
        <div className="modal-actions"><Button variant="secondary" onClick={close} disabled={saving}>Отмена</Button>
          <Button type="submit" loading={saving}>Принять поставку</Button></div>
      </form>
    </Modal>
  );
}
