import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { DETENTION_PAGE_SIZE } from '../detentionLabels';
import { DetentionRequestListResponse, IncidentStatus } from '../types';
import { DetentionRequestEditorModal } from './DetentionRequestEditorModal';
import { DetentionRequestTable } from './DetentionRequestTable';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

interface IncidentDetentionSectionProps {
  token: string; incidentId: number; incidentStatus: IncidentStatus; initialAlienId?: number | null;
  canRead: boolean; canCreate: boolean; canReadAliens?: boolean;
}

export function IncidentDetentionSection(props: IncidentDetentionSectionProps) {
  return props.canRead ? <IncidentDetentions key={`${props.token}:${props.incidentId}`} {...props} /> : null;
}

function IncidentDetentions({ token, incidentId, incidentStatus, initialAlienId, canCreate, canReadAliens = true }: IncidentDetentionSectionProps) {
  const [result, setResult] = useState<DetentionRequestListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [revision, setRevision] = useState(0);
  const [page, setPage] = useState(0);
  const creatable = canCreate && incidentStatus !== 'EXECUTION_COMPLETED';

  useEffect(() => {
    let cancelled = false; setLoading(true); setError(null);
    api.listDetentionRequests(token, { incidentId, page, size: DETENTION_PAGE_SIZE })
      .then((response) => {
        if (cancelled) { return; }
        if (page > 0 && page >= Math.max(response.totalPages, 1)) { setPage(Math.max(response.totalPages - 1, 0)); }
        else { setResult(response); }
      })
      .catch((err: unknown) => { if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить заявки'); } })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, incidentId, incidentStatus, revision, page]);

  return <section className="section" aria-label="Заключение по инциденту" aria-busy={loading}>
    {creatable && <div className="card__header"><Button onClick={() => setOpen(true)}>Создать заявку на заключение</Button></div>}
    {loading && <LoadingBlock label="Загрузка заявок…" inline />}
    {error && <div className="alert alert--error" role="alert">{error}</div>}
    {!loading && !error && result && <>
      {result.items.length ? <DetentionRequestTable token={token} items={result.items} fromIncidentId={incidentId} />
        : <EmptyState title="Заявок на заключение пока нет" />}
      <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
      <Button variant="outline" to={`/detention-requests?incidentId=${incidentId}`}>Все заявки по инциденту</Button>
    </>}
    <DetentionRequestEditorModal token={token} open={open && creatable} incidentId={incidentId} initialAlienId={initialAlienId}
      canReadAliens={canReadAliens} onClose={() => setOpen(false)} onSaved={() => { setPage(0); setRevision((value) => value + 1); }} />
  </section>;
}
