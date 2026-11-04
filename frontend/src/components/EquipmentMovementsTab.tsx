import { formatUserLabel } from '../utils/users';
import { Card, CardHeader } from './ui/Card';
import React from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { parseEnumValue, positiveId } from '../utils/params';
import { useListSearchParams } from '../hooks/useListSearchParams';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { EQUIPMENT_PAGE_SIZE, MOVEMENT_OPERATION_LABELS, MOVEMENT_STATUS_LABELS } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';

import { EmployeeSearch } from './EmployeeSearch';
import { EquipmentMovementTable } from './EquipmentMovementTable';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

export function EquipmentMovementsTab({ token }: { token: string }) {
  const { params, page, setFilter: filter, setPage } = useListSearchParams();
  const status = parseEnumValue(MOVEMENT_STATUS_LABELS, params.get('status'));
  const operation = parseEnumValue(MOVEMENT_OPERATION_LABELS, params.get('operation'));
  const incidentId = positiveId(params.get('incidentId'));
  const responsibleUserId = positiveId(params.get('responsibleUserId'));
  const equipmentId = positiveId(params.get('equipmentId'));
  const { result, loading, error } = usePaginatedResource({
    scopeKey: JSON.stringify([token, incidentId, responsibleUserId, status, operation, equipmentId]), page,
    load: (requestedPage) => api.listEquipmentMovements(token, { incidentId, responsibleUserId, status, operation, equipmentId, page: requestedPage, size: EQUIPMENT_PAGE_SIZE }),
    errorMessage: 'Не удалось загрузить заявки', onPageCorrection: (nextPage) => setPage(nextPage, { replace: true }),
  });
  const users = useUsersMap(token, [responsibleUserId]);

  return (
    <Card  aria-busy={loading}>
      <CardHeader><h2>Движение оборудования</h2></CardHeader>
      <div className="filter-bar">
        <FormField label="Статус"><select value={status ?? ''} onChange={(event) => filter('status', event.target.value)}>
          <option value="">Все статусы</option>
          {Object.entries(MOVEMENT_STATUS_LABELS).map(([value, label]) => <option value={value} key={value}>{label}</option>)}
        </select></FormField>
        <FormField label="Операция"><select value={operation ?? ''} onChange={(event) => filter('operation', event.target.value)}>
          <option value="">Все операции</option>
          {Object.entries(MOVEMENT_OPERATION_LABELS).map(([value, label]) => <option value={value} key={value}>{label}</option>)}
        </select></FormField>
        <FormField label="Инцидент"><input type="number" min={1} placeholder="Номер инцидента"
          value={incidentId ?? ''} onChange={(event) => filter('incidentId', positiveId(event.target.value)
            ? event.target.value : '')} /></FormField>
        <EmployeeSearch token={token} label="Ответственный"
          value={responsibleUserId ? { id: responsibleUserId, login: formatUserLabel(responsibleUserId, users) } : null}
          onChange={(user) => filter('responsibleUserId', user ? String(user.id) : '')} />
      </div>
      {equipmentId && <p className="equipment-context">Движения по <Link to={`/equipment/${equipmentId}`}>
        оборудованию #{equipmentId}</Link>
        <Button variant="ghost" size="sm" onClick={() => filter('equipmentId', '')}>Снять фильтр</Button>
      </p>}
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка заявок…" skeleton rows={4} />}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="Заявки не найдены" />}
      {!loading && !error && result != null && result.items.length > 0 && <EquipmentMovementTable token={token}
        items={result.items} search={`?${params.toString()}`} />}
      {!error && result && <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements}
        loading={loading} onChange={setPage} />}
    </Card>
  );
}
