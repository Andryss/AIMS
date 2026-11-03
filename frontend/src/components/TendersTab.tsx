import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, EQUIPMENT_TYPE_LABELS, positiveId, formatEquipmentDate } from '../equipmentLabels';
import { TENDER_STATUS_LABELS } from '../tenderLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { TenderListResponse, TenderStatus, EquipmentType } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { TenderEditorModal } from './TenderEditorModal';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { StatusChip } from './ui/StatusChip';
import { UserChip } from './UserChip';

interface TendersTabProps { token: string; canCreate: boolean; }

export function TendersTab({ token, canCreate }: TendersTabProps) {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const requestedPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const rawType = params.get('equipmentType') ?? '';
  const rawStatus = params.get('status') ?? '';
  const type = Object.keys(EQUIPMENT_TYPE_LABELS).includes(rawType) ? rawType as EquipmentType : undefined;
  const status = Object.keys(TENDER_STATUS_LABELS).includes(rawStatus) ? rawStatus as TenderStatus : undefined;
  const incidentId = positiveId(params.get('incidentId'));
  const [input, setInput] = useState(q);
  const [result, setResult] = useState<TenderListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(canCreate && params.get('create') === '1');
  const users = useUsersMap(token, result?.items.map((item) => item.createdByUserId) ?? []);

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

    api.listTenders(token, { q, equipmentType: type, status, incidentId, page, size: EQUIPMENT_PAGE_SIZE })
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
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить тендеры'); }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, q, type, status, incidentId, page, setParams]);

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header">
        <h2>Тендеры</h2>
        {canCreate && <Button onClick={() => setEditorOpen(true)}>Создать тендер</Button>}
      </div>
      <div className="equipment-filters">
        <div className="field equipment-search-field">
          <input type="search" className="field__control" aria-label="Поиск тендеров" value={input}
            placeholder="Условия закупки" onChange={(event) => setInput(event.target.value)} />
        </div>
        <FormField label="Тип оборудования">
          <select value={type ?? ''} onChange={(event) => changeFilter('equipmentType', event.target.value)}>
            <option value="">Все типы</option>
            {Object.entries(EQUIPMENT_TYPE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </FormField>
        <FormField label="Статус">
          <select value={status ?? ''} onChange={(event) => changeFilter('status', event.target.value)}>
            <option value="">Все статусы</option>
            {Object.entries(TENDER_STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
        </FormField>
        <FormField label="Инцидент-основание"><input type="number" min={1} step={1} value={incidentId ?? ''}
          onChange={(event) => changeFilter('incidentId', String(positiveId(event.target.value) ?? ''))} /></FormField>
      </div>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка тендеров…" skeleton rows={4} />}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="Тендеры не найдены" />}
      {!loading && !error && result != null && result.items.length > 0 && (
        <div className="incidents-table-wrapper">
          <table className="incidents-table">
            <thead><tr><th>Номер</th><th>Тип</th><th>Количество оборудования</th><th>Статус</th><th>Автор</th><th>Победитель</th><th>Дата</th></tr></thead>
            <tbody>{result.items.map((item) => {
              const path = `/tenders/${item.id}?${params.toString()}`;
              return <tr key={item.id} className="incidents-table__row" onClick={() => navigate(path)}>
                <td><Link to={path} className="knowledge-base__name" onClick={(event) => event.stopPropagation()}>Тендер #{item.id}</Link></td>
                <td>{EQUIPMENT_TYPE_LABELS[item.equipmentType]}</td><td>{item.quantity}</td>
                <td><StatusChip status={item.status.toLowerCase()} label={TENDER_STATUS_LABELS[item.status]} /></td>
                <td><UserChip login={formatUserLabel(item.createdByUserId, users)} size={24} /></td>
                <td>{item.winnerParticipantId != null ? `Участник #${item.winnerParticipantId}` : '—'}</td>
                <td>{formatEquipmentDate(item.createdAt)}</td>
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
      <TenderEditorModal token={token} open={editorOpen && canCreate} tender={null} incidentId={incidentId}
        onClose={() => {
          setEditorOpen(false);
          if (params.has('create')) { setParams((previous) => { const next = new URLSearchParams(previous); next.delete('create'); return next; }, { replace: true }); }
        }} onSaved={(saved) => {
          const next = new URLSearchParams(params); next.delete('create');
          navigate(`/tenders/${saved.id}?${next.toString()}`);
        }} />
    </section>
  );
}
