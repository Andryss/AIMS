import { formatUserLabel } from '../utils/users';
import React, { FormEvent, useEffect, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, outstandingEquipment } from '../equipmentLabels';
import { useMutation } from '../hooks/useMutation';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentListResponse, EquipmentMovementResponse, EquipmentResponse, SaveEquipmentMovementRequest, UserSummary } from '../types';

import { EmployeeSearch } from './EmployeeSearch';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

interface EquipmentMovementEditorModalProps {
  token: string;
  open: boolean;
  incidentId: number;
  movement?: EquipmentMovementResponse | null;
  sourceIssue?: EquipmentMovementResponse | null;
  onClose: () => void;
  onSaved: (movement: EquipmentMovementResponse) => void;
}

export function EquipmentMovementEditorModal({ token, open, incidentId, movement = null,
  sourceIssue = null, onClose, onSaved }: EquipmentMovementEditorModalProps) {
  const returning = movement?.operation === 'RETURN' || sourceIssue != null;
  const [source, setSource] = useState<EquipmentMovementResponse | null>(sourceIssue);
  const [responsible, setResponsible] = useState<UserSummary | null>(null);
  const [selected, setSelected] = useState<Map<number, EquipmentResponse>>(new Map());
  const [comment, setComment] = useState('');
  const [input, setInput] = useState('');
  const [q, setQ] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<EquipmentListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const mutation = useMutation(JSON.stringify([token, open, incidentId, movement?.id, sourceIssue?.id]));
  const { busy: submitting, error, setError } = mutation;
  const [revision, setRevision] = useState(0);
  const users = useUsersMap(token, [responsible?.id, source?.responsibleUserId]);
  const sourceId = sourceIssue?.id ?? movement?.sourceIssueMovementId;

  useEffect(() => {
    if (!open) { return; }
    const holderId = sourceIssue?.responsibleUserId ?? movement?.responsibleUserId;
    setResponsible(holderId ? { id: holderId, login: `#${holderId}` } : null);
    setSelected(new Map(movement?.equipment.map((item) => [item.id, item]) ?? []));
    setComment(movement?.comment ?? '');
    setSource(sourceIssue);
    setInput(''); setQ(''); setPage(0);
    setResult(null); setError(null); setLoadError(null);
  }, [open, token, incidentId, movement, sourceIssue, setError]);

  useEffect(() => {
    const timer = window.setTimeout(() => { setQ(input.trim()); setPage(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [input]);

  useEffect(() => {
    if (!open || !returning || !sourceId) { return; }
    let cancelled = false;
    setLoading(true);
    setLoadError(null);

    api.getEquipmentMovement(token, sourceId)
      .then((response) => { if (!cancelled) { setSource(response); } })
      .catch((err: unknown) => {
        if (!cancelled) { setLoadError(err instanceof Error ? err.message : 'Не удалось загрузить оборудование'); }
      }).finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, open, returning, sourceId, revision]);

  useEffect(() => {
    if (!open || returning) { return; }
    setResult(null);
    setLoadError(null);
    setLoading(false);
    if (input.trim().length < 2 || input.trim() !== q) { return; }
    let cancelled = false;
    setLoading(true);

    api.listEquipment(token, { q, status: 'AVAILABLE', page, size: EQUIPMENT_PAGE_SIZE })
      .then((response) => { if (!cancelled) { setResult(response); } })
      .catch((err: unknown) => {
        if (!cancelled) { setLoadError(err instanceof Error ? err.message : 'Не удалось загрузить оборудование'); }
      }).finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, open, returning, input, q, page, revision]);

  if (!open) { return null; }

  const available = returning ? (source ? outstandingEquipment(source.equipment, source.id) : []) : result?.items ?? [];
  const searching = input.trim().length >= 2 && input.trim() === q;
  const matches = returning ? available.filter((item) =>
    `${item.model} ${item.serialNumber}`.toLowerCase().includes(q.toLowerCase())) : available;
  const candidates = (returning ? matches.slice(page * EQUIPMENT_PAGE_SIZE, (page + 1) * EQUIPMENT_PAGE_SIZE) : matches)
    .filter((item) => !selected.has(item.id));
  const holderId = returning ? source?.responsibleUserId : responsible?.id;
  const close = () => { if (!mutation.isBusy()) { onClose(); } };
  const add = (item: EquipmentResponse) => setSelected((previous) => new Map(previous).set(item.id, item));
  const remove = (id: number) => setSelected((previous) => {
    const next = new Map(previous);
    next.delete(id);
    return next;
  });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) { return; }
    if (!holderId || selected.size === 0) {
      setError('Укажите ответственного и выберите оборудование');
      return;
    }
    if (returning && (!source || Array.from(selected.keys()).some((id) => !available.some((item) => item.id === id)))) {
      setError('Выбранное оборудование больше не числится по этой выдаче. Обновите доступность');
      return;
    }

    const payload: SaveEquipmentMovementRequest = {
      operation: returning ? 'RETURN' : 'ISSUE', incidentId: returning ? source!.incidentId : incidentId,
      responsibleUserId: holderId, sourceIssueMovementId: returning ? source!.id : null,
      equipmentIds: Array.from(selected.keys()), ...(comment.trim() ? { comment: comment.trim() } : {}),
    };
    const session = mutation.start();
    if (!session) { return; }

    try {
      const saved = movement
        ? await api.updateEquipmentMovement(token, movement.id, payload)
        : await api.createEquipmentMovement(token, payload);
      if (!session.isCurrent()) { return; }
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      session.fail(err, 'Не удалось сохранить заявку');
    } finally {
      session.finish();
    }
  };

  const selectedUser = responsible ? { id: responsible.id,
    login: responsible.login.startsWith('#') ? formatUserLabel(responsible.id, users) : responsible.login } : null;

  return (
    <Modal open={open} title={movement ? 'Редактировать заявку' : returning ? 'Заявка на возврат' : 'Заявка на выдачу'} onClose={close} busy={submitting} className="modal--equipment">
      <p>Инцидент #{returning ? source?.incidentId ?? incidentId : incidentId}
        {returning && sourceId ? ` · По выдаче #${sourceId}` : ''}</p>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      <form className="form" onSubmit={submit}>
        {returning ? <div className="field"><span className="field__label">Ответственный</span>
          <UserChip login={formatUserLabel(source?.responsibleUserId, users)} /></div>
          : <EmployeeSearch token={token} label="Ответственный" required value={selectedUser}
            onChange={setResponsible} disabled={submitting} />}
        <div className="field">
          <input className="field__control" type="search" aria-label="Поиск оборудования"
            placeholder="Найти по модели или серийному номеру" value={input} disabled={submitting}
            onChange={(event) => { setInput(event.target.value); setPage(0); }} />
        </div>
        {loadError && <div className="alert alert--error" role="alert">{loadError}</div>}
        {loading && <LoadingBlock label="Загрузка доступного оборудования…" inline />}
        {input.trim().length < 2 && <p className="field__help">Введите минимум 2 символа для поиска оборудования</p>}
        {searching && !loading && !loadError && candidates.length > 0 && (
          <ul className="equipment-search-results" aria-label="Найденное оборудование">
            {candidates.map((item) => <li key={item.id}>
              <Button variant="ghost" aria-label={`Добавить ${item.serialNumber}`} disabled={submitting}
                onClick={() => add(item)}>{item.model} · {item.serialNumber}</Button>
            </li>)}
          </ul>
        )}
        {searching && !loading && !loadError && candidates.length === 0 && <p className="field__help">
          {matches.length > 0 ? 'Найденное оборудование уже выбрано' : 'Доступное оборудование не найдено'}
        </p>}
        {searching && !loadError && (returning ? matches.length > EQUIPMENT_PAGE_SIZE : result && result.totalPages > 1) && (
          <Pagination page={page} totalPages={returning ? Math.ceil(matches.length / EQUIPMENT_PAGE_SIZE) : result!.totalPages}
            totalElements={returning ? matches.length : result!.totalElements} loading={loading || submitting} onChange={setPage} />
        )}
        <section aria-label="Выбранное оборудование" className="equipment-selection">
          <h3>Выбрано: {selected.size}</h3>
          {selected.size === 0 && <p className="field__help">Добавьте оборудование из результатов поиска</p>}
          {Array.from(selected.values()).map((item) => <div key={item.id} className="equipment-selection__item">
            <span>{item.model} · {item.serialNumber}</span>
            <Button variant="ghost" size="sm" disabled={submitting} aria-label={`Убрать ${item.serialNumber}`}
              onClick={() => remove(item.id)}>×</Button>
          </div>)}
        </section>
        {(error || loadError) && <Button variant="outline" onClick={() => setRevision((value) => value + 1)} disabled={loading || submitting}>
          Обновить доступность
        </Button>}
        <FormField label="Комментарий">
          <textarea value={comment} maxLength={4000} rows={3} disabled={submitting}
            onChange={(event) => setComment(event.target.value)} />
        </FormField>
        <div className="modal-actions">
          <Button variant="secondary" onClick={close} disabled={submitting}>Отмена</Button>
          <Button type="submit" loading={submitting} disabled={loading || Boolean(loadError)}>
            {movement ? 'Сохранить' : 'Создать заявку'}
          </Button>
        </div>
      </form>
    </Modal>
  );
}
