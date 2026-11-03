import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { DETENTION_PAGE_SIZE } from '../detentionLabels';
import { formatEquipmentDate } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { DetentionRequestHistoryEntry, DetentionRequestHistoryListResponse } from '../types';
import { buildDetentionHistoryDiff, DetentionHistoryValue } from '../utils/detentionHistoryDiff';
import { formatUserLabel } from '../utils/incidentUsers';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

interface DetentionRequestHistoryProps {
  token: string; requestId: number; revision?: number; canReadIncidents?: boolean; canReadAliens?: boolean;
}

export function DetentionRequestHistory({ token, requestId, revision, canReadIncidents = true, canReadAliens = true }: DetentionRequestHistoryProps) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<DetentionRequestHistoryListResponse | null>(null);
  const [previousEntry, setPreviousEntry] = useState<DetentionRequestHistoryEntry | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [alienNames, setAlienNames] = useState<Map<number, string>>(new Map());
  const entries = [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])];
  const users = useUsersMap(token, entries.flatMap((entry) => [entry.changedByUserId,
    entry.snapshot.request.createdByUserId, entry.snapshot.request.reviewedByUserId]));
  const alienKey = Array.from(new Set(entries.map((entry) => entry.snapshot.request.alienId).filter((id): id is number => id != null))).sort((a, b) => a - b).join(',');

  useEffect(() => setPage(0), [requestId]);
  useEffect(() => {
    let cancelled = false; setLoading(true); setError(null);
    Promise.all([api.listDetentionRequestHistory(token, requestId, page, DETENTION_PAGE_SIZE),
      page > 0 ? api.listDetentionRequestHistory(token, requestId, page - 1, DETENTION_PAGE_SIZE) : Promise.resolve(null)])
      .then(([response, previous]) => { if (!cancelled) { setResult(response); setPreviousEntry(previous?.items[previous.items.length - 1]); } })
      .catch((err: unknown) => { if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить историю'); } })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, requestId, page, revision]);

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
      <ol className="history-timeline">{result.items.map((entry, index) => {
        const rows = buildDetentionHistoryDiff(entry.snapshot, (index ? result.items[index - 1] : previousEntry)?.snapshot);
        return <li key={entry.id} className="history-timeline__item">
          <div className="history-timeline__header"><span>Изменение заявки</span><span aria-hidden>·</span>
            <span className="history-timeline__when">{formatEquipmentDate(entry.changedAt)}</span><span aria-hidden>·</span>
            <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} /></div>
          <ul className="history-diff-list">{rows.map((row) => <li key={row.label} className="history-diff__row">
            <span className="history-diff__label">{row.label}:</span><span className="text-pre-wrap">
              {row.oldValue !== undefined && <>{value(row.oldValue)}{' → '}</>}{value(row.newValue)}</span>
          </li>)}</ul>
          {entry.snapshot.transitionComment && <p className="text-pre-wrap">{entry.snapshot.transitionComment}</p>}
        </li>;
      })}</ol>
      <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} loading={loading} onChange={setPage} />
    </>}
  </section>;
}
