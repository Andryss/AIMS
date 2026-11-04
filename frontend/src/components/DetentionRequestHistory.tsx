import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { DETENTION_PAGE_SIZE } from '../detentionLabels';

import { useUsersMap } from '../hooks/useUsersMap';
import { usePagedHistory } from '../hooks/usePagedHistory';
import { buildDetentionHistoryDiff, DetentionHistoryValue } from '../utils/detentionHistoryDiff';

import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { HistoryTimeline } from './ui/HistoryTimeline';
import { HistoryDiffList } from './ui/HistoryDiffList';

interface DetentionRequestHistoryProps {
  token: string; requestId: number; revision?: number; canReadIncidents?: boolean; canReadAliens?: boolean;
}

export function DetentionRequestHistory({ token, requestId, revision, canReadIncidents = true, canReadAliens = true }: DetentionRequestHistoryProps) {
  const { page, setPage, result, previousEntry, loading, error } = usePagedHistory({
    scopeKey: JSON.stringify([token, requestId]), revision,
    loadPage: (nextPage) => api.listDetentionRequestHistory(token, requestId, nextPage, DETENTION_PAGE_SIZE),
  });
  const [alienNames, setAlienNames] = useState<Map<number, string>>(new Map());
  const entries = [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])];
  const users = useUsersMap(token, entries.flatMap((entry) => [entry.changedByUserId,
    entry.snapshot.request.createdByUserId, entry.snapshot.request.reviewedByUserId]));
  const alienKey = Array.from(new Set(entries.map((entry) => entry.snapshot.request.alienId).filter((id): id is number => id != null))).sort((a, b) => a - b).join(',');

  useEffect(() => {
    let cancelled = false; setAlienNames(new Map());
    if (!canReadAliens || !alienKey) { return; }
    Promise.all(alienKey.split(',').map(async (value) => {
      const id = Number(value);
      try { return [id, (await api.getAlien(token, id)).name] as const; }
      catch { return [id, `Вид #${id}`] as const; }
    })).then((pairs) => { if (!cancelled) { setAlienNames(new Map(pairs)); } });
    return () => { cancelled = true; };
  }, [token, canReadAliens, alienKey]);

  const value = (item: DetentionHistoryValue): React.ReactNode => {
    if (typeof item === 'string') { return item; }
    if (item.id == null) { return '—'; }
    if (item.kind === 'user') { return <UserChip login={formatUserLabel(item.id, users)} size={24} />; }
    if (item.kind === 'alien') { const name = alienNames.get(item.id) ?? `Вид #${item.id}`;
      return canReadAliens ? <Link to={`/aliens/${item.id}`}>{name}</Link> : name; }
    return canReadIncidents ? <Link to={`/incidents/${item.id}`}>Инцидент #{item.id}</Link> : `Инцидент #${item.id}`;
  };

  return <section aria-label="История заявки на заключение" aria-busy={loading}>
    {error && <div className="alert alert--error" role="alert">{error}</div>}
    {loading && <LoadingBlock label="Загрузка истории…" inline />}
    {!loading && !error && result?.items.length === 0 && <EmptyState title="История пока пуста" />}
    {!loading && !error && result && <>
      <HistoryTimeline entries={result.items} previousEntry={previousEntry} renderHeader={(entry) => <>
        <span>Изменение заявки</span><span aria-hidden>·</span>
        <span className="history-timeline__when">{formatDateTime(entry.changedAt)}</span><span aria-hidden>·</span>
        <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} />
      </>}>
        {(entry, previous) => <>
          <HistoryDiffList rows={buildDetentionHistoryDiff(entry.snapshot, previous?.snapshot)} renderValue={value} />
          {entry.snapshot.transitionComment && <p className="text-pre-wrap">{entry.snapshot.transitionComment}</p>}
        </>}
      </HistoryTimeline>
      <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} loading={loading} onChange={setPage} />
    </>}
  </section>;
}
