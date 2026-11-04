import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React from 'react';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE } from '../equipmentLabels';
import { usePagedHistory } from '../hooks/usePagedHistory';
import { useUsersMap } from '../hooks/useUsersMap';

import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { HistoryTimeline } from './ui/HistoryTimeline';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { buildEquipmentMovementHistoryDiff } from '../utils/equipmentHistoryDiff';
import { EquipmentHistoryChanges } from './EquipmentHistoryChanges';

interface EquipmentMovementHistoryProps { token: string; movementId: number; revision: number; }

export function EquipmentMovementHistory({ token, movementId, revision }: EquipmentMovementHistoryProps) {
  const { page, setPage, result, previousEntry, loading, error } = usePagedHistory({
    scopeKey: JSON.stringify([token, movementId]), revision, errorMessage: 'Не удалось загрузить историю заявки',
    loadPage: (nextPage) => api.listEquipmentMovementHistory(token, movementId, nextPage, EQUIPMENT_PAGE_SIZE),
  });
  const users = useUsersMap(token, [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])].flatMap((entry) =>
    [entry.changedByUserId, entry.snapshot.movement.responsibleUserId]));

  return (
    <section aria-label="История заявки" aria-busy={loading}>
      {loading && <LoadingBlock label="Загрузка истории заявки…" inline />}
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="История пока пуста" />}
      {!loading && !error && result && <>
        <HistoryTimeline entries={result.items} previousEntry={previousEntry} renderHeader={(entry, index) => <>
          {page === 0 && index === 0 ? 'Создание заявки · ' : ''}
          <span className="history-timeline__when">{formatDateTime(entry.changedAt)}</span>
          <span aria-hidden>·</span>
          <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} />
        </>}>
          {(entry, previous) => <>
            <EquipmentHistoryChanges rows={buildEquipmentMovementHistoryDiff(entry.snapshot.movement, previous?.snapshot.movement)} users={users} />
            {entry.snapshot.transitionComment && <p className="text-pre-wrap">Причина: {entry.snapshot.transitionComment}</p>}
          </>}
        </HistoryTimeline>
        <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
      </>}
    </section>
  );
}
