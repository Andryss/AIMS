import React, { FormEvent, useEffect, useId, useRef, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, EQUIPMENT_TYPE_LABELS, outstandingEquipment } from '../equipmentLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentListResponse, EquipmentMovementResponse, EquipmentResponse, EquipmentType,
  SaveEquipmentMovementRequest, UserSummary } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { EmployeeSearch } from './EmployeeSearch';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
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
  const titleId = useId();
  const trapRef = useFocusTrap(open);
  const submittingRef = useRef(false);
  const returning = movement?.operation === 'RETURN' || sourceIssue != null;
  const [source, setSource] = useState<EquipmentMovementResponse | null>(sourceIssue);
  const [responsible, setResponsible] = useState<UserSummary | null>(null);
  const [selected, setSelected] = useState<Map<number, EquipmentResponse>>(new Map());
  const [comment, setComment] = useState('');
  const [input, setInput] = useState('');
  const [q, setQ] = useState('');
  const [type, setType] = useState<EquipmentType | ''>('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<EquipmentListResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
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
    setInput(''); setQ(''); setType(''); setPage(0);
    setResult(null); setError(null); setLoadError(null);
  }, [open, movement, sourceIssue]);

  useEffect(() => {
    const timer = window.setTimeout(() => { setQ(input.trim()); setPage(0); }, 300);
    return () => window.clearTimeout(timer);
  }, [input]);

  useEffect(() => {
    if (!open) { return; }
    let cancelled = false;
    setLoading(true);
    setLoadError(null);

    const request = returning && sourceId
      ? api.getEquipmentMovement(token, sourceId).then((response) => {
        if (!cancelled) { setSource(response); }
      })
      : returning ? Promise.resolve()
        : api.listEquipment(token, { q, type: type || undefined, status: 'AVAILABLE', page, size: EQUIPMENT_PAGE_SIZE })
          .then((response) => { if (!cancelled) { setResult(response); } });

    request.catch((err: unknown) => {
      if (!cancelled) { setLoadError(err instanceof Error ? err.message : 'Не удалось загрузить оборудование'); }
    }).finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, open, returning, sourceId, q, type, page, revision]);

  useEffect(() => {
    if (!open) { return; }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !submittingRef.current) { onClose(); }
    };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [open, onClose]);

  if (!open) { return null; }

  const available = returning ? (source ? outstandingEquipment(source.equipment, source.id) : []) : result?.items ?? [];
  const holderId = returning ? source?.responsibleUserId : responsible?.id;
  const close = () => { if (!submittingRef.current) { onClose(); } };
  const toggle = (item: EquipmentResponse) => setSelected((previous) => {
    const next = new Map(previous);
    if (next.has(item.id)) { next.delete(item.id); } else { next.set(item.id, item); }
    return next;
  });

  const submit = async (event: FormEvent) => {
    event.preventDefault();
    if (submittingRef.current) { return; }
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
    submittingRef.current = true;
    setSubmitting(true);
    setError(null);

    try {
      const saved = movement
        ? await api.updateEquipmentMovement(token, movement.id, payload)
        : await api.createEquipmentMovement(token, payload);
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Не удалось сохранить заявку');
    } finally {
      submittingRef.current = false;
      setSubmitting(false);
    }
  };

  const selectedUser = responsible ? { id: responsible.id,
    login: responsible.login.startsWith('#') ? formatUserLabel(responsible.id, users) : responsible.login } : null;

  return (
    <div className="modal-overlay" role="presentation" onClick={close}>
      <div ref={trapRef} className="modal modal--equipment" role="dialog" aria-modal="true"
        aria-labelledby={titleId} onClick={(event) => event.stopPropagation()}>
        <div className="modal__header">
          <h2 id={titleId}>{movement ? 'Редактировать заявку' : returning ? 'Заявка на возврат' : 'Заявка на выдачу'}</h2>
          <Button variant="ghost" onClick={close} disabled={submitting} aria-label="Закрыть">×</Button>
        </div>
        <p>Инцидент #{returning ? source?.incidentId ?? incidentId : incidentId}
          {returning && sourceId ? ` · По выдаче #${sourceId}` : ''}</p>
        {error && <div className="alert alert--error" role="alert">{error}</div>}
        <form className="form" onSubmit={submit}>
          {returning ? <div className="field"><span className="field__label">Ответственный</span>
            <UserChip login={formatUserLabel(source?.responsibleUserId, users)} /></div>
            : <EmployeeSearch token={token} label="Ответственный" required value={selectedUser}
              onChange={setResponsible} disabled={submitting} />}
          {!returning && <div className="equipment-filters">
            <div className="field equipment-search-field"><input className="field__control" type="search" aria-label="Поиск оборудования"
              placeholder="Модель или серийный номер" value={input} disabled={submitting}
              onChange={(event) => setInput(event.target.value)} /></div>
            <FormField label="Тип оборудования">
              <select value={type} disabled={submitting} onChange={(event) => {
                setType(event.target.value as EquipmentType | ''); setPage(0);
              }}><option value="">Все типы</option>
                {Object.entries(EQUIPMENT_TYPE_LABELS).map(([value, label]) => <option value={value} key={value}>{label}</option>)}
              </select>
            </FormField>
          </div>}
          <section aria-label="Выбранное оборудование" className="equipment-selection">
            <h3>Выбрано: {selected.size}</h3>
            {Array.from(selected.values()).map((item) => <div key={item.id} className="equipment-selection__item">
              <span>{item.model} · {item.serialNumber}</span>
              <Button variant="ghost" size="sm" disabled={submitting} aria-label={`Убрать ${item.serialNumber}`}
                onClick={() => toggle(item)}>×</Button>
            </div>)}
          </section>
          {loadError && <div className="alert alert--error" role="alert">{loadError}</div>}
          {loading && <LoadingBlock label="Загрузка доступного оборудования…" inline />}
          {!loading && !loadError && available.length === 0 && <EmptyState title={returning
            ? 'Нет оборудования для возврата' : 'Доступное оборудование не найдено'} />}
          {!loading && !loadError && available.length > 0 && <div className="incidents-table-wrapper">
            <table className="incidents-table"><thead><tr><th>Выбор</th><th>Модель</th><th>Серийный номер</th></tr></thead>
              <tbody>{available.map((item) => <tr key={item.id}>
                <td><input type="checkbox" aria-label={`Выбрать ${item.serialNumber}`} checked={selected.has(item.id)}
                  disabled={submitting} onChange={() => toggle(item)} /></td>
                <td>{item.model}</td><td>{item.serialNumber}</td>
              </tr>)}</tbody>
            </table>
          </div>}
          {!returning && result && <Pagination page={page} totalPages={result.totalPages}
            totalElements={result.totalElements} loading={loading || submitting} onChange={setPage} />}
          <Button variant="outline" onClick={() => setRevision((value) => value + 1)} disabled={loading || submitting}>
            Обновить доступность
          </Button>
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
      </div>
    </div>
  );
}
