import { CardHeader } from './ui/Card';
import React, { FormEvent, useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { DETENTION_DEFAULT_DURATION } from '../detentionLabels';
import { useMutation } from '../hooks/useMutation';
import { Alien, DetentionRequestResponse } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';

interface DetentionRequestEditorModalProps {
  token: string; open: boolean; incidentId: number; initialAlienId?: number | null;
  request?: DetentionRequestResponse; canReadAliens?: boolean;
  onClose: () => void; onSaved: (request: DetentionRequestResponse) => void;
}

export function DetentionRequestEditorModal({ token, open, incidentId, initialAlienId, request,
  canReadAliens = true, onClose, onSaved }: DetentionRequestEditorModalProps) {
  const initial = useRef({ request, initialAlienId });
  initial.current = { request, initialAlienId };
  const requestId = request?.id;
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [days, setDays] = useState(String(DETENTION_DEFAULT_DURATION));
  const [comment, setComment] = useState('');
  const [alienId, setAlienId] = useState<number | undefined>();
  const [alienName, setAlienName] = useState('');
  const [query, setQuery] = useState('');
  const [aliens, setAliens] = useState<Alien[]>([]);
  const [searching, setSearching] = useState(false);
  const [speciesError, setSpeciesError] = useState<string | null>(null);
  const mutation = useMutation(JSON.stringify([token, open, incidentId, request?.id]));
  const { busy: saving, error, setError } = mutation;

  useEffect(() => {
    if (open) {
      const value = initial.current.request;
      setName(value?.subjectName ?? ''); setDescription(value?.subjectDescription ?? '');
      setDays(String(value?.durationDays ?? DETENTION_DEFAULT_DURATION)); setComment('');
      setAlienId(value ? value.alienId ?? undefined : initial.current.initialAlienId ?? undefined);
      setAlienName(''); setQuery(''); setAliens([]); setError(null); setSpeciesError(null);
    }
  }, [open, token, requestId, incidentId, setError]);

  useEffect(() => {
    if (!open || alienId == null || !canReadAliens) { return; }
    let cancelled = false;
    api.getAlien(token, alienId).then((value) => { if (!cancelled) { setAlienName(value.name); } })
      .catch(() => { if (!cancelled) { setAlienName(`Вид #${alienId}`); } });
    return () => { cancelled = true; };
  }, [open, token, alienId, canReadAliens]);

  useEffect(() => {
    setAliens([]); setSpeciesError(null);
    if (!open || !canReadAliens || !query.trim()) { setSearching(false); return; }
    let cancelled = false;
    setSearching(true);
    const timer = window.setTimeout(() => {
      api.searchAliens(token, query.trim()).then((value) => { if (!cancelled) { setAliens(value.items); } })
        .catch((err: unknown) => { if (!cancelled) { setSpeciesError(err instanceof Error ? err.message : 'Не удалось найти вид'); } })
        .finally(() => { if (!cancelled) { setSearching(false); } });
    }, 300);
    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [open, canReadAliens, query, token]);

  if (!open) { return null; }
  const changedDuration = request != null && Number(days) !== request.durationDays;
  const close = () => { if (!mutation.isBusy()) { onClose(); } };
  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) { return; }
    const durationDays = Number(days);
    if (!name.trim() || !description.trim() || !Number.isInteger(durationDays) || durationDays < 1
      || durationDays > 2147483647 || !Number.isSafeInteger(incidentId) || incidentId < 1) {
      setError('Заполните данные субъекта и укажите целый положительный срок'); return;
    }
    if (changedDuration && !comment.trim()) { setError('Укажите причину изменения срока'); return; }
    const session = mutation.start();
    if (!session) { return; }
    const payload = { incidentId, subjectName: name.trim(), subjectDescription: description.trim(), durationDays,
      ...(alienId != null ? { alienId } : {}),
      ...(changedDuration ? { comment: comment.trim() } : {}) };
    try {
      const saved = request ? await api.updateDetentionRequest(token, request.id, payload)
        : await api.createDetentionRequest(token, payload);
      if (!session.isCurrent()) { return; }
      onClose(); onSaved(saved);
    } catch (err: unknown) {
      session.fail(err, 'Не удалось сохранить заявку');
    } finally {
      session.finish();
    }
  };

  return (
    <Modal open={open} title={request ? 'Редактировать заявку на заключение' : 'Создать заявку на заключение'} onClose={close} busy={saving}>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      <form className="form" onSubmit={submit}>
        <FormField label="Инцидент-основание"><input value={`Инцидент #${incidentId}`} disabled /></FormField>
        <FormField label="Имя или обозначение" required><input required maxLength={200} value={name} disabled={saving}
          onChange={(event) => setName(event.target.value)} /></FormField>
        <FormField label="Описание субъекта" required><textarea required rows={4} maxLength={4000} value={description}
          disabled={saving} onChange={(event) => setDescription(event.target.value)} /></FormField>
        <div className="field"><span className="field__label">Вид</span>
          {alienId != null && <CardHeader><span>{alienName || `Вид #${alienId}`}</span>
            {canReadAliens && <Button variant="ghost" disabled={saving} aria-label="Убрать вид"
              onClick={() => { setAlienId(undefined); setAlienName(''); }}>×</Button>}</CardHeader>}
          {canReadAliens && <input type="search" className="field__control" aria-label="Поиск вида" placeholder="Найти вид в базе знаний"
            value={query} disabled={saving} onChange={(event) => setQuery(event.target.value)} />}
          {speciesError && <div className="alert alert--error" role="alert">{speciesError}</div>}
          {searching && <span role="status">Поиск…</span>}
          {query.trim() && !searching && !speciesError && aliens.length === 0 && <p className="text-muted">Виды не найдены</p>}
          {aliens.map((alien) => <Button key={alien.id} variant="ghost" disabled={saving} aria-label={`Выбрать ${alien.name}`}
            onClick={() => { setAlienId(alien.id); setAlienName(alien.name); setQuery(''); }}>{alien.name}</Button>)}
        </div>
        <FormField label="Срок заключения, суток" required><input type="number" min={1} max={2147483647} step={1}
          required value={days} disabled={saving} onChange={(event) => setDays(event.target.value)} /></FormField>
        {changedDuration && <FormField label="Причина изменения срока" required><textarea required rows={3} maxLength={4000}
          value={comment} disabled={saving} onChange={(event) => setComment(event.target.value)} /></FormField>}
        <div className="modal-actions"><Button variant="secondary" disabled={saving} onClick={close}>Отмена</Button>
          <Button type="submit" loading={saving}>Сохранить</Button></div>
      </form>
    </Modal>
  );
}
