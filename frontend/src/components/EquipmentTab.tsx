import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, EQUIPMENT_STATUS_LABELS, EQUIPMENT_TYPE_LABELS, positiveId } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentListResponse, EquipmentStatus, EquipmentType } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { EmployeeSearch } from './EmployeeSearch';
import { EquipmentEditorModal } from './EquipmentEditorModal';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { StatusChip } from './ui/StatusChip';
import { UserChip } from './UserChip';

interface EquipmentTabProps { token: string; canWrite: boolean; }

export function EquipmentTab({ token, canWrite }: EquipmentTabProps) {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const requestedPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const rawType = params.get('type') ?? '';
  const rawStatus = params.get('status') ?? '';
  const type = Object.keys(EQUIPMENT_TYPE_LABELS).includes(rawType) ? rawType as EquipmentType : undefined;
  const status = Object.keys(EQUIPMENT_STATUS_LABELS).includes(rawStatus) ? rawStatus as EquipmentStatus : undefined;
  const holderUserId = positiveId(params.get('holderUserId'));
  const [input, setInput] = useState(q);
  const [result, setResult] = useState<EquipmentListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [revision, setRevision] = useState(0);
  const users = useUsersMap(token, [holderUserId, ...(result?.items.map((item) => item.holderUserId) ?? [])]);

  const changeFilter = (key: string, value: string) => {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (value) { next.set(key, value); } else { next.delete(key); }
      next.delete('page');
      return next;
    });
  };

  useEffect(() => setInput(q), [q]);

  useEffect(() => {
    if (input.trim() === q) { return; }
    const timer = window.setTimeout(() => {
      setParams((previous) => {
        const next = new URLSearchParams(previous);
        if (input.trim()) { next.set('q', input.trim()); } else { next.delete('q'); }
        next.delete('page');
        return next;
      });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [input, q, setParams]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    api.listEquipment(token, { q, type, status, holderUserId, page, size: EQUIPMENT_PAGE_SIZE })
      .then((response) => {
        if (cancelled) { return; }
        if (page >= Math.max(response.totalPages, 1) && page > 0) {
          setParams((previous) => {
            const next = new URLSearchParams(previous);
            next.set('page', String(Math.max(response.totalPages - 1, 0)));
            return next;
          }, { replace: true });
        } else {
          setResult(response);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить склад'); }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, q, type, status, holderUserId, page, revision, setParams]);

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header">
        <h2>Склад</h2>
        {canWrite && <Button onClick={() => setEditorOpen(true)}>Добавить оборудование</Button>}
      </div>
      <div className="equipment-filters">
        <div className="field equipment-search-field">
          <input type="search" className="field__control" aria-label="Поиск оборудования" value={input}
            placeholder="Модель или серийный номер" onChange={(event) => setInput(event.target.value)} />
        </div>
        <FormField label="Тип оборудования">
          <select value={type ?? ''} onChange={(event) => changeFilter('type', event.target.value)}>
            <option value="">Все типы</option>
            {Object.entries(EQUIPMENT_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </FormField>
        <FormField label="Состояние">
          <select value={status ?? ''} onChange={(event) => changeFilter('status', event.target.value)}>
            <option value="">Все состояния</option>
            {Object.entries(EQUIPMENT_STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </FormField>
        <EmployeeSearch token={token} label="Держатель"
          value={holderUserId ? { id: holderUserId, login: formatUserLabel(holderUserId, users) } : null}
          onChange={(user) => changeFilter('holderUserId', user ? String(user.id) : '')} />
      </div>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка склада…" skeleton rows={4} />}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="Оборудование не найдено" />}
      {!loading && !error && result != null && result.items.length > 0 && (
        <div className="incidents-table-wrapper">
          <table className="incidents-table">
            <thead><tr><th>Тип</th><th>Модель</th><th>Серийный номер</th><th>Состояние</th><th>Держатель</th></tr></thead>
            <tbody>{result.items.map((item) => {
              const path = `/equipment/${item.id}?${params.toString()}`;
              return <tr key={item.id} className="incidents-table__row" onClick={() => navigate(path)}>
                <td>{EQUIPMENT_TYPE_LABELS[item.type]}</td>
                <td><Link to={path} className="knowledge-base__name"
                  onClick={(event) => event.stopPropagation()}>{item.model}</Link></td>
                <td>{item.serialNumber}</td>
                <td><StatusChip status={item.status.toLowerCase()} label={EQUIPMENT_STATUS_LABELS[item.status]} /></td>
                <td>{item.holderUserId != null
                  ? <UserChip login={formatUserLabel(item.holderUserId, users)} size={24} /> : '—'}</td>
              </tr>;
            })}</tbody>
          </table>
        </div>
      )}
      {!error && result && <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements}
        loading={loading} onChange={(nextPage) => setParams((previous) => {
          const next = new URLSearchParams(previous);
          next.set('page', String(nextPage));
          return next;
        })} />}
      <EquipmentEditorModal token={token} open={editorOpen && canWrite} equipment={null}
        onClose={() => setEditorOpen(false)} onSaved={() => setRevision((value) => value + 1)} />
    </section>
  );
}
