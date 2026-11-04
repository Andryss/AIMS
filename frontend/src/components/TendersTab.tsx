import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import { Card, CardHeader } from './ui/Card';
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as api from '../api/client';
import { parseEnumValue, positiveId } from '../utils/params';
import { useListSearchParams } from '../hooks/useListSearchParams';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { useDebouncedQuery } from '../hooks/useDebouncedSearch';
import { EQUIPMENT_PAGE_SIZE, EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { TENDER_STATUS_LABELS } from '../tenderLabels';
import { useUsersMap } from '../hooks/useUsersMap';

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
  const { params, setParams, page, setFilter: changeFilter, setPage } = useListSearchParams();
  const q = params.get('q') ?? '';
  const type = parseEnumValue(EQUIPMENT_TYPE_LABELS, params.get('equipmentType'));
  const status = parseEnumValue(TENDER_STATUS_LABELS, params.get('status'));
  const incidentId = positiveId(params.get('incidentId'));
  const [editorOpen, setEditorOpen] = useState(canCreate && params.get('create') === '1');
  const { result, loading, error } = usePaginatedResource({
    scopeKey: JSON.stringify([token, q, type, status, incidentId]), page,
    load: (requestedPage) => api.listTenders(token, { q, equipmentType: type, status, incidentId, page: requestedPage, size: EQUIPMENT_PAGE_SIZE }),
    errorMessage: 'Не удалось загрузить тендеры', onPageCorrection: (nextPage) => setPage(nextPage, { replace: true }),
  });
  const { input, setInput } = useDebouncedQuery(q, (query) => changeFilter('q', query));
  const users = useUsersMap(token, result?.items.map((item) => item.createdByUserId) ?? []);

  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <h2>Тендеры</h2>
        {canCreate && <Button onClick={() => setEditorOpen(true)}>Создать тендер</Button>}
      </CardHeader>
      <div className="filter-bar">
        <div className="field filter-search-field">
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
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>Номер</th><th>Тип</th><th>Количество оборудования</th><th>Статус</th><th>Автор</th><th>Победитель</th><th>Дата</th></tr></thead>
            <tbody>{result.items.map((item) => {
              const path = `/tenders/${item.id}?${params.toString()}`;
              return <tr key={item.id} className="data-table__row" onClick={() => navigate(path)}>
                <td><Link to={path} className="entity-link" onClick={(event) => event.stopPropagation()}>Тендер #{item.id}</Link></td>
                <td>{EQUIPMENT_TYPE_LABELS[item.equipmentType]}</td><td>{item.quantity}</td>
                <td><StatusChip status={item.status.toLowerCase()} label={TENDER_STATUS_LABELS[item.status]} /></td>
                <td><UserChip login={formatUserLabel(item.createdByUserId, users)} size={24} /></td>
                <td>{item.winnerParticipantId != null ? `Участник #${item.winnerParticipantId}` : '—'}</td>
                <td>{formatDateTime(item.createdAt)}</td>
              </tr>;
            })}</tbody>
          </table>
        </div>
      )}
      {!error && result && <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements}
        loading={loading} onChange={setPage} />}
      <TenderEditorModal token={token} open={editorOpen && canCreate} tender={null} incidentId={incidentId}
        onClose={() => {
          setEditorOpen(false);
          if (params.has('create')) { setParams((previous) => { const next = new URLSearchParams(previous); next.delete('create'); return next; }, { replace: true }); }
        }} onSaved={(saved) => {
          const next = new URLSearchParams(params); next.delete('create');
          navigate(`/tenders/${saved.id}?${next.toString()}`);
        }} />
    </Card>
  );
}
