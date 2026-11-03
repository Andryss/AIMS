import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE, formatEquipmentDate } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { TenderHistoryEntry, TenderHistoryListResponse } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { buildTenderHistoryDiff, TenderHistoryValue } from '../utils/tenderHistoryDiff';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

interface TenderHistoryProps {
  token: string; tenderId: number; revision?: number; canReadIncidents?: boolean; canReadEquipment?: boolean;
}

export function TenderHistory({ token, tenderId, revision, canReadIncidents = true, canReadEquipment = true }: TenderHistoryProps) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<TenderHistoryListResponse | null>(null);
  const [previousEntry, setPreviousEntry] = useState<TenderHistoryEntry | undefined>();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const entries = [...(result?.items ?? []), ...(previousEntry ? [previousEntry] : [])];
  const users = useUsersMap(token, entries.flatMap((entry) => [entry.changedByUserId,
    entry.snapshot.tender.createdByUserId, ...entry.snapshot.participants.map((item) => item.createdByUserId)]));

  useEffect(() => setPage(0), [tenderId]);
  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    Promise.all([api.listTenderHistory(token, tenderId, page, EQUIPMENT_PAGE_SIZE),
      page > 0 ? api.listTenderHistory(token, tenderId, page - 1, EQUIPMENT_PAGE_SIZE) : Promise.resolve(null)])
      .then(([response, previous]) => {
        if (!cancelled) { setResult(response); setPreviousEntry(previous?.items[previous.items.length - 1]); }
      })
      .catch((err: unknown) => { if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить историю'); } })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, tenderId, page, revision]);

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
      <ol className="history-timeline">{result.items.map((entry, index) => {
        const rows = buildTenderHistoryDiff(entry.snapshot, (index > 0 ? result.items[index - 1] : previousEntry)?.snapshot);
        return <li key={entry.id} className="history-timeline__item">
          <div className="history-timeline__header"><span>Изменение тендера</span><span aria-hidden>·</span>
            <span className="history-timeline__when">{formatEquipmentDate(entry.changedAt)}</span><span aria-hidden>·</span>
            <UserChip login={formatUserLabel(entry.changedByUserId, users)} size={24} /></div>
          <ul className="history-diff-list">{rows.map((row) => <li key={row.label} className="history-diff__row">
            <span className="history-diff__label">{row.label}:</span><span className="text-pre-wrap">
              {row.oldValue !== undefined && <>{value(row.oldValue)}{' → '}</>}{value(row.newValue)}
            </span>
          </li>)}</ul>
          {entry.snapshot.transitionComment && <p className="text-pre-wrap">{entry.snapshot.transitionComment}</p>}
        </li>;
      })}</ol>
      <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} loading={loading} onChange={setPage} />
    </>}
  </section>;
}
