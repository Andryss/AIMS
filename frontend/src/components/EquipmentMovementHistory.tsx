import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, formatEquipmentDate } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentMovementHistoryEntry, EquipmentMovementHistoryListResponse } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { buildEquipmentMovementHistoryDiff } from '../utils/equipmentHistoryDiff';
import { EquipmentHistoryChanges } from './EquipmentHistoryChanges';

interface EquipmentMovementHistoryProps { token: string; movementId: number; revision: number; }

export function EquipmentMovementHistory({ token, movementId, revision }: EquipmentMovementHistoryProps) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<EquipmentMovementHistoryListResponse | null>(null);
  const [previousEntry, setPreviousEntry] = useState<EquipmentMovementHistoryEntry | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const users = useUsersMap(token, [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])].flatMap((entry) =>
    [entry.changedByUserId, entry.snapshot.movement.responsibleUserId]));

  useEffect(() => setPage(0), [movementId]);
  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError(null);
    Promise.all([
      api.listEquipmentMovementHistory(token, movementId, page, EQUIPMENT_PAGE_SIZE),
      page > 0 ? api.listEquipmentMovementHistory(token, movementId, page - 1, EQUIPMENT_PAGE_SIZE) : Promise.resolve(null),
    ]).then(([response, previous]) => {
      if (!cancelled) { setResult(response); setPreviousEntry(previous?.items[previous.items.length - 1]); }
    })
      .catch((err: unknown) => {
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить историю заявки'); }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, movementId, revision, page]);

  return (
    <section aria-label="История заявки" aria-busy={loading}>
      {loading && <LoadingBlock label="Загрузка истории заявки…" inline />}
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="История пока пуста" />}
      {!loading && !error && result && <>
        <ol className="history-timeline">{result.items.map((entry, index) => <li key={entry.id} className="history-timeline__item">
          <div className="history-timeline__header">
            {page === 0 && index === 0 ? 'Создание заявки · ' : ''}
            <span className="history-timeline__when">{formatEquipmentDate(entry.changedAt)}</span>
            <span aria-hidden>·</span>
            <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} />
          </div>
          <EquipmentHistoryChanges rows={buildEquipmentMovementHistoryDiff(entry.snapshot.movement,
            (index > 0 ? result.items[index - 1] : previousEntry)?.snapshot.movement)} users={users} />
          {entry.snapshot.transitionComment && <p className="text-pre-wrap">Причина: {entry.snapshot.transitionComment}</p>}
        </li>)}</ol>
        <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
      </>}
    </section>
  );
}
