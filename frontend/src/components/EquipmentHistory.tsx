import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, formatEquipmentDate } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentHistoryEntry, EquipmentHistoryListResponse } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { buildEquipmentHistoryDiff } from '../utils/equipmentHistoryDiff';
import { EquipmentHistoryChanges } from './EquipmentHistoryChanges';

const EVENT_LABELS: Record<EquipmentHistoryEntry['snapshot']['event'], string> = {
  REGISTERED: 'Добавлено на склад', UPDATED: 'Изменены реквизиты',
  ISSUED: 'Выдача сотруднику', RETURNED: 'Возврат на склад',
};

interface EquipmentHistoryProps { token: string; equipmentId: number; revision?: number; canReadMovements: boolean; }

export function EquipmentHistory({ token, equipmentId, revision, canReadMovements }: EquipmentHistoryProps) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<EquipmentHistoryListResponse | null>(null);
  const [previousEntry, setPreviousEntry] = useState<EquipmentHistoryEntry | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const users = useUsersMap(token, [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])].flatMap((entry) =>
    [entry.changedByUserId, entry.snapshot.equipment.holderUserId]));

  useEffect(() => setPage(0), [equipmentId]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([
      api.listEquipmentHistory(token, equipmentId, page, EQUIPMENT_PAGE_SIZE),
      page > 0 ? api.listEquipmentHistory(token, equipmentId, page - 1, EQUIPMENT_PAGE_SIZE) : Promise.resolve(null),
    ]).then(([response, previous]) => {
      if (!cancelled) { setResult(response); setPreviousEntry(previous?.items[previous.items.length - 1]); }
    })
      .catch((err: unknown) => {
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить историю'); }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, equipmentId, page, revision]);

  return (
    <section aria-label="История оборудования" aria-busy={loading}>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка истории…" inline />}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="История пока пуста" />}
      {!loading && !error && result && <>
        <ol className="history-timeline">{result.items.map((entry, index) => <li key={entry.id} className="history-timeline__item">
          <div className="history-timeline__header">
            <span>{EVENT_LABELS[entry.snapshot.event]}</span><span aria-hidden>·</span>
            <span className="history-timeline__when">{formatEquipmentDate(entry.changedAt)}</span>
            <span aria-hidden>·</span>
            <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} />
          </div>
          <EquipmentHistoryChanges rows={buildEquipmentHistoryDiff(entry.snapshot.equipment,
            (index > 0 ? result.items[index - 1] : previousEntry)?.snapshot.equipment)} users={users}
            canReadMovements={canReadMovements} equipmentId={equipmentId} />
          {entry.snapshot.movementId != null && <p>Основание: {canReadMovements
            ? <Link to={`/equipment-movements/${entry.snapshot.movementId}`} state={{ equipmentId }}>
              Заявка #{entry.snapshot.movementId}
            </Link> : <span>Заявка #{entry.snapshot.movementId}</span>}</p>}
        </li>)}</ol>
        <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
      </>}
    </section>
  );
}
