import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, MOVEMENT_OPERATION_LABELS, MOVEMENT_STATUS_LABELS, positiveId } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentMovementListResponse, EquipmentMovementOperation, EquipmentMovementStatus } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { EmployeeSearch } from './EmployeeSearch';
import { EquipmentMovementTable } from './EquipmentMovementTable';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

export function EquipmentMovementsTab({ token }: { token: string }) {
  const [params, setParams] = useSearchParams();
  const requestedPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const rawStatus = params.get('status') ?? '';
  const rawOperation = params.get('operation') ?? '';
  const status = Object.keys(MOVEMENT_STATUS_LABELS).includes(rawStatus) ? rawStatus as EquipmentMovementStatus : undefined;
  const operation = Object.keys(MOVEMENT_OPERATION_LABELS).includes(rawOperation)
    ? rawOperation as EquipmentMovementOperation : undefined;
  const incidentId = positiveId(params.get('incidentId'));
  const responsibleUserId = positiveId(params.get('responsibleUserId'));
  const equipmentId = positiveId(params.get('equipmentId'));
  const [result, setResult] = useState<EquipmentMovementListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const users = useUsersMap(token, [responsibleUserId]);

  const filter = (key: string, value: string) => setParams((previous) => {
    const next = new URLSearchParams(previous);
    if (value) { next.set(key, value); } else { next.delete(key); }
    next.delete('page');
    return next;
  });

  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError(null);

    api.listEquipmentMovements(token, { incidentId, responsibleUserId, status, operation, equipmentId,
      page, size: EQUIPMENT_PAGE_SIZE })
      .then((response) => {
        if (cancelled) { return; }
        if (page >= Math.max(response.totalPages, 1) && page > 0) {
          setParams((previous) => {
            const next = new URLSearchParams(previous);
            next.set('page', String(Math.max(response.totalPages - 1, 0)));
            return next;
          }, { replace: true });
        } else { setResult(response); }
      })
      .catch((err: unknown) => {
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить заявки'); }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, incidentId, responsibleUserId, status, operation, equipmentId, page, setParams]);

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header"><h2>Движение оборудования</h2></div>
      <div className="equipment-filters">
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
        loading={loading} onChange={(nextPage) => setParams((previous) => {
          const next = new URLSearchParams(previous);
          next.set('page', String(nextPage));
          return next;
        })} />}
    </section>
  );
}
