import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE } from '../equipmentLabels';
import { usePagedHistory } from '../hooks/usePagedHistory';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentHistoryEntry } from '../types';

import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { HistoryTimeline } from './ui/HistoryTimeline';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { buildEquipmentHistoryDiff } from '../utils/equipmentHistoryDiff';
import { EquipmentHistoryChanges } from './EquipmentHistoryChanges';

const EVENT_LABELS: Record<EquipmentHistoryEntry['snapshot']['event'], string> = {
  REGISTERED: 'Добавлено на склад', RECEIVED: 'Поступление по тендеру', UPDATED: 'Изменены реквизиты',
  ISSUED: 'Выдача сотруднику', RETURNED: 'Возврат на склад',
};

interface EquipmentHistoryProps { token: string; equipmentId: number; revision?: number; canReadMovements: boolean; canReadTenders?: boolean; }

export function EquipmentHistory({ token, equipmentId, revision, canReadMovements, canReadTenders = false }: EquipmentHistoryProps) {
  const { page, setPage, result, previousEntry, loading, error } = usePagedHistory({
    scopeKey: JSON.stringify([token, equipmentId]), revision,
    loadPage: (nextPage) => api.listEquipmentHistory(token, equipmentId, nextPage, EQUIPMENT_PAGE_SIZE),
  });
  const users = useUsersMap(token, [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])].flatMap((entry) =>
    [entry.changedByUserId, entry.snapshot.equipment.holderUserId]));

  return (
    <section aria-label="История оборудования" aria-busy={loading}>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка истории…" inline />}
      {!loading && !error && result?.items.length === 0 && <EmptyState title="История пока пуста" />}
      {!loading && !error && result && <>
        <HistoryTimeline entries={result.items} previousEntry={previousEntry} renderHeader={(entry) => <>
          <span>{EVENT_LABELS[entry.snapshot.event]}</span><span aria-hidden>·</span>
          <span className="history-timeline__when">{formatDateTime(entry.changedAt)}</span>
          <span aria-hidden>·</span>
          <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} />
        </>}>
          {(entry, previous) => <>
            <EquipmentHistoryChanges rows={buildEquipmentHistoryDiff(entry.snapshot.equipment, previous?.snapshot.equipment)} users={users}
              canReadMovements={canReadMovements} canReadTenders={canReadTenders} equipmentId={equipmentId} />
            {entry.snapshot.movementId != null && <p>Основание: {canReadMovements
              ? <Link to={`/equipment-movements/${entry.snapshot.movementId}`} state={{ equipmentId }}>
                Заявка #{entry.snapshot.movementId}
              </Link> : <span>Заявка #{entry.snapshot.movementId}</span>}</p>}
          </>}
        </HistoryTimeline>
        <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
      </>}
    </section>
  );
}
