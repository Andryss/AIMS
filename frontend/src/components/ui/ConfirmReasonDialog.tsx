import React from 'react';
import { Button } from './Button';
import { FormField } from './FormField';
import { Modal } from './Modal';

interface ConfirmReasonDialogProps {
  open: boolean;
  title: string;
  onClose: () => void;
  busy: boolean;
  reason: string;
  onReasonChange: (reason: string) => void;
  onConfirm: () => void;
  error?: string | null;
  label?: string;
  maxLength?: number | null;
  disableEmpty?: boolean;
}

export function ConfirmReasonDialog({ open, title, onClose, busy, reason, onReasonChange, onConfirm,
  error, label = 'Причина', maxLength = 4000, disableEmpty = false }: ConfirmReasonDialogProps) {
  return <Modal open={open} title={title} onClose={onClose} busy={busy} className="modal--narrow">
    {error && <div className="alert alert--error" role="alert" aria-live="polite">{error}</div>}
    <form className="form" onSubmit={(event) => { event.preventDefault(); if (!busy) { onConfirm(); } }}>
      <FormField label={label} required><textarea required rows={4} maxLength={maxLength ?? undefined} value={reason}
        disabled={busy} onChange={(event) => onReasonChange(event.target.value)} /></FormField>
      <div className="modal-actions"><Button variant="secondary" disabled={busy} onClick={onClose}>Отмена</Button>
        <Button type="submit" loading={busy} disabled={disableEmpty && !reason.trim()}>Подтвердить</Button></div>
    </form>
  </Modal>;
}
