import React, { FormEvent, useEffect, useId, useRef, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { TenderReceiptResponse, TenderResponse } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

const MAX_INITIAL_ROWS = 10;
interface ReceiptRow { key: number; model: string; serialNumber: string; }
interface TenderReceiptModalProps {
  token: string; open: boolean; tender: TenderResponse;
  onClose: () => void; onSaved: (response: TenderReceiptResponse) => void;
}

export function TenderReceiptModal({ token, open, tender, onClose, onSaved }: TenderReceiptModalProps) {
  const titleId = useId();
  const trapRef = useFocusTrap(open);
  const scope = useRef(0);
  const busy = useRef(false);
  const nextKey = useRef(0);
  const [rows, setRows] = useState<ReceiptRow[]>([]);
  const [comment, setComment] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    scope.current += 1;
    busy.current = false;
    setSaving(false);
    if (open) {
      setRows(Array.from({ length: Math.min(tender.quantity, MAX_INITIAL_ROWS) }, () =>
        ({ key: nextKey.current++, model: '', serialNumber: '' })));
      setComment('');
      setError(null);
    }
    return () => { scope.current += 1; };
  }, [open, token, tender.id, tender.quantity]);

  useEffect(() => {
    if (!open) { return; }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape' && !busy.current) { onClose(); } };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [open, onClose]);

  if (!open) { return null; }
  const close = () => { if (!busy.current) { onClose(); } };
  const editRow = (key: number, field: 'model' | 'serialNumber', value: string) => {
    setRows((previous) => previous.map((row) => row.key === key ? { ...row, [field]: value } : row));
  };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy.current) { return; }
    if (rows.length !== tender.quantity) { setError(`Необходимо указать ${tender.quantity} экземпляров`); return; }
    const equipment = rows.map(({ model, serialNumber }) => ({ model: model.trim(), serialNumber: serialNumber.trim() }));
    if (equipment.some((item) => !item.model || !item.serialNumber)) { setError('Заполните модель и серийный номер каждого экземпляра'); return; }
    if (new Set(equipment.map((item) => item.serialNumber.toLowerCase())).size !== equipment.length) {
      setError('Серийные номера не должны повторяться'); return;
    }
    const version = scope.current;
    busy.current = true;
    setSaving(true);
    setError(null);
    try {
      const response = await api.receiveTender(token, tender.id, { equipment, ...(comment.trim() ? { comment: comment.trim() } : {}) });
      if (scope.current !== version) { return; }
      onSaved(response);
      onClose();
    } catch (err: unknown) {
      if (scope.current === version) { setError(err instanceof Error ? err.message : 'Не удалось принять поставку'); }
    } finally {
      if (scope.current === version) { busy.current = false; setSaving(false); }
    }
  };

  return <div className="modal-overlay" role="presentation" onClick={close}>
    <div className="modal" ref={trapRef} role="dialog" aria-modal="true" aria-labelledby={titleId}
      onClick={(event) => event.stopPropagation()}>
      <div className="modal__header"><h2 id={titleId}>Приёмка поставки</h2>
        <Button variant="ghost" aria-label="Закрыть" disabled={saving} onClick={close}>×</Button></div>
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
    </div>
  </div>;
}
