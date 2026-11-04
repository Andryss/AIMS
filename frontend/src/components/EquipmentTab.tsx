import { formatUserLabel } from '../utils/users';
import { Card, CardHeader } from './ui/Card';
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as api from '../api/client';
import { parseEnumValue, positiveId } from '../utils/params';
import { useListSearchParams } from '../hooks/useListSearchParams';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { useDebouncedQuery } from '../hooks/useDebouncedSearch';
import { EQUIPMENT_PAGE_SIZE, EQUIPMENT_STATUS_LABELS, EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';

import { EmployeeSearch } from './EmployeeSearch';
import { EquipmentEditorModal } from './EquipmentEditorModal';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { StatusChip } from './ui/StatusChip';
import { UserChip } from './UserChip';

interface EquipmentTabProps { token: string; canWrite: boolean; canCreateTenders?: boolean; }

export function EquipmentTab({ token, canWrite, canCreateTenders = false }: EquipmentTabProps) {
  const navigate = useNavigate();
  const { params, page, setFilter: changeFilter, setPage } = useListSearchParams();
  const q = params.get('q') ?? '';
  const type = parseEnumValue(EQUIPMENT_TYPE_LABELS, params.get('type'));
  const status = parseEnumValue(EQUIPMENT_STATUS_LABELS, params.get('status'));
  const holderUserId = positiveId(params.get('holderUserId'));
  const [editorOpen, setEditorOpen] = useState(false);
  const { result, loading, error, reload } = usePaginatedResource({
    scopeKey: JSON.stringify([token, q, type, status, holderUserId]), page,
    load: (requestedPage) => api.listEquipment(token, { q, type, status, holderUserId, page: requestedPage, size: EQUIPMENT_PAGE_SIZE }),
    errorMessage: 'Не удалось загрузить склад', onPageCorrection: (nextPage) => setPage(nextPage, { replace: true }),
  });
  const { input, setInput } = useDebouncedQuery(q, (query) => changeFilter('q', query));
  const users = useUsersMap(token, [holderUserId, ...(result?.items.map((item) => item.holderUserId) ?? [])]);

  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <h2>Склад</h2>
        <div className="page-actions">
          {canCreateTenders && <Button variant="outline" to="/tenders?create=1">Создать тендер</Button>}
          {canWrite && <Button onClick={() => setEditorOpen(true)}>Добавить оборудование</Button>}
        </div>
      </CardHeader>
      <div className="filter-bar">
        <div className="field filter-search-field">
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
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>Тип</th><th>Модель</th><th>Серийный номер</th><th>Состояние</th><th>Держатель</th></tr></thead>
            <tbody>{result.items.map((item) => {
              const path = `/equipment/${item.id}?${params.toString()}`;
              return <tr key={item.id} className="data-table__row" onClick={() => navigate(path)}>
                <td>{EQUIPMENT_TYPE_LABELS[item.type]}</td>
                <td><Link to={path} className="entity-link"
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
        loading={loading} onChange={setPage} />}
      <EquipmentEditorModal token={token} open={editorOpen && canWrite} equipment={null}
        onClose={() => setEditorOpen(false)} onSaved={reload} />
    </Card>
  );
}
