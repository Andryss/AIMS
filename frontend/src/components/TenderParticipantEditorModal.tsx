import React, { FormEvent, useEffect, useId, useRef, useState } from 'react';
import * as api from '../api/client';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { TenderParticipantResponse } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

interface TenderParticipantEditorModalProps {
  token: string; open: boolean; tenderId: number; participant: TenderParticipantResponse | null;
  onClose: () => void; onSaved: (participant: TenderParticipantResponse) => void;
}

export function TenderParticipantEditorModal({ token, open, tenderId, participant, onClose, onSaved }: TenderParticipantEditorModalProps) {
  const titleId = useId();
  const trapRef = useFocusTrap(open);
  const scope = useRef(0);
  const busy = useRef(false);
  const [name, setName] = useState('');
  const [proposal, setProposal] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    scope.current += 1;
    busy.current = false;
    setSaving(false);
    if (open) { setName(participant?.name ?? ''); setProposal(participant?.proposal ?? ''); setError(null); }
    return () => { scope.current += 1; };
  }, [open, token, tenderId, participant]);

  useEffect(() => {
    if (!open) { return; }
    const escape = (event: KeyboardEvent) => { if (event.key === 'Escape' && !busy.current) { onClose(); } };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [open, onClose]);

  if (!open) { return null; }
  const close = () => { if (!busy.current) { onClose(); } };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (busy.current) { return; }
    if (!name.trim() || !proposal.trim()) { setError('Укажите организацию и предложение'); return; }
    const version = scope.current;
    busy.current = true;
    setSaving(true);
    setError(null);
    const payload = { name: name.trim(), proposal: proposal.trim() };
    try {
      const saved = participant ? await api.updateTenderParticipant(token, tenderId, participant.id, payload)
        : await api.createTenderParticipant(token, tenderId, payload);
      if (scope.current !== version) { return; }
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      if (scope.current === version) { setError(err instanceof Error ? err.message : 'Не удалось сохранить предложение'); }
    } finally {
      if (scope.current === version) { busy.current = false; setSaving(false); }
    }
  };

  return <div className="modal-overlay" role="presentation" onClick={close}>
    <div className="modal" ref={trapRef} role="dialog" aria-modal="true" aria-labelledby={titleId}
      onClick={(event) => event.stopPropagation()}>
      <div className="modal__header"><h2 id={titleId}>{participant ? 'Редактировать предложение' : 'Добавить участника'}</h2>
        <Button variant="ghost" aria-label="Закрыть" disabled={saving} onClick={close}>×</Button></div>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      <form className="form" onSubmit={submit}>
        <FormField label="Организация" required><input value={name} maxLength={200} required disabled={saving}
          onChange={(event) => setName(event.target.value)} /></FormField>
        <FormField label="Предложение" required><textarea rows={6} value={proposal} maxLength={4000} required
          disabled={saving} onChange={(event) => setProposal(event.target.value)} /></FormField>
        <div className="modal-actions"><Button variant="secondary" disabled={saving} onClick={close}>Отмена</Button>
          <Button type="submit" loading={saving}>Сохранить</Button></div>
      </form>
    </div>
  </div>;
}
