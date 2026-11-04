import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { usePagedHistory } from '../hooks/usePagedHistory';

import { buildTenderHistoryDiff, TenderHistoryValue } from '../utils/tenderHistoryDiff';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { HistoryTimeline } from './ui/HistoryTimeline';
import { HistoryDiffList } from './ui/HistoryDiffList';

interface TenderHistoryProps {
  token: string; tenderId: number; revision?: number; canReadIncidents?: boolean; canReadEquipment?: boolean;
}

export function TenderHistory({ token, tenderId, revision, canReadIncidents = true, canReadEquipment = true }: TenderHistoryProps) {
  const { page, setPage, result, previousEntry, loading, error } = usePagedHistory({
    scopeKey: JSON.stringify([token, tenderId]), revision,
    loadPage: (nextPage) => api.listTenderHistory(token, tenderId, nextPage, EQUIPMENT_PAGE_SIZE),
  });
  const entries = [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])];
  const users = useUsersMap(token, entries.flatMap((entry) => [entry.changedByUserId,
    entry.snapshot.tender.createdByUserId, ...entry.snapshot.participants.map((item) => item.createdByUserId)]));

  const value = (item: TenderHistoryValue): React.ReactNode => {
    if (typeof item === 'string') { return item; }
    if (item.kind === 'equipment') {
      return item.ids.length ? item.ids.map((id, index) => <React.Fragment key={id}>{index > 0 && ', '}
        {canReadEquipment ? <Link to={`/equipment/${id}`}>Оборудование #{id}</Link> : `Оборудование #${id}`}
      </React.Fragment>) : '—';
    }
    if (item.id == null) { return '—'; }
    if (item.kind === 'user') { return <UserChip login={formatUserLabel(item.id, users)} size={24} />; }
    return canReadIncidents ? <Link to={`/incidents/${item.id}`}>Инцидент #{item.id}</Link> : `Инцидент #${item.id}`;
  };

  return <section aria-label="История тендера" aria-busy={loading}>
    {error && <div className="alert alert--error" role="alert">{error}</div>}
    {loading && <LoadingBlock label="Загрузка истории…" inline />}
    {!loading && !error && result?.items.length === 0 && <EmptyState title="История пока пуста" />}
    {!loading && !error && result && <>
      <HistoryTimeline entries={result.items} previousEntry={previousEntry} renderHeader={(entry) => <>
        <span>Изменение тендера</span><span aria-hidden>·</span>
        <span className="history-timeline__when">{formatDateTime(entry.changedAt)}</span><span aria-hidden>·</span>
        <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} />
      </>}>
        {(entry, previous) => <>
          <HistoryDiffList rows={buildTenderHistoryDiff(entry.snapshot, previous?.snapshot)} renderValue={value} />
          {entry.snapshot.transitionComment && <p className="text-pre-wrap">{entry.snapshot.transitionComment}</p>}
        </>}
      </HistoryTimeline>
      <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} loading={loading} onChange={setPage} />
    </>}
  </section>;
}
