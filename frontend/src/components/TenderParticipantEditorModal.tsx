import React, { FormEvent, useEffect, useState } from 'react';
import * as api from '../api/client';
import { useMutation } from '../hooks/useMutation';
import { TenderParticipantResponse } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';

interface TenderParticipantEditorModalProps {
  token: string; open: boolean; tenderId: number; participant: TenderParticipantResponse | null;
  onClose: () => void; onSaved: (participant: TenderParticipantResponse) => void;
}

export function TenderParticipantEditorModal({ token, open, tenderId, participant, onClose, onSaved }: TenderParticipantEditorModalProps) {
  const [name, setName] = useState('');
  const [proposal, setProposal] = useState('');
  const mutation = useMutation(JSON.stringify([token, open, tenderId, participant?.id]));
  const { busy: saving, error, setError } = mutation;

  useEffect(() => {
    if (open) { setName(participant?.name ?? ''); setProposal(participant?.proposal ?? ''); setError(null); }
  }, [open, token, tenderId, participant, setError]);

  if (!open) { return null; }
  const close = () => { if (!mutation.isBusy()) { onClose(); } };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) { return; }
    if (!name.trim() || !proposal.trim()) { setError('Укажите организацию и предложение'); return; }
    const session = mutation.start();
    if (!session) { return; }
    const payload = { name: name.trim(), proposal: proposal.trim() };
    try {
      const saved = participant ? await api.updateTenderParticipant(token, tenderId, participant.id, payload)
        : await api.createTenderParticipant(token, tenderId, payload);
      if (!session.isCurrent()) { return; }
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      session.fail(err, 'Не удалось сохранить предложение');
    } finally {
      session.finish();
    }
  };

  return (
    <Modal open={open} title={participant ? 'Редактировать предложение' : 'Добавить участника'} onClose={close} busy={saving}>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      <form className="form" onSubmit={submit}>
        <FormField label="Организация" required><input value={name} maxLength={200} required disabled={saving}
          onChange={(event) => setName(event.target.value)} /></FormField>
        <FormField label="Предложение" required><textarea rows={6} value={proposal} maxLength={4000} required
          disabled={saving} onChange={(event) => setProposal(event.target.value)} /></FormField>
        <div className="modal-actions"><Button variant="secondary" disabled={saving} onClick={close}>Отмена</Button>
          <Button type="submit" loading={saving}>Сохранить</Button></div>
      </form>
    </Modal>
  );
}
