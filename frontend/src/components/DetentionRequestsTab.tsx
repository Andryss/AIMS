import React, { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import * as api from '../api/client';
import { DETENTION_PAGE_SIZE, DETENTION_STATUS_LABELS } from '../detentionLabels';
import { positiveId } from '../equipmentLabels';
import { DetentionRequestListResponse, DetentionRequestStatus } from '../types';
import { DetentionRequestTable } from './DetentionRequestTable';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

export function DetentionRequestsTab({ token, canReadIncidents = true }: { token: string; canReadIncidents?: boolean }) {
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const rawStatus = params.get('status') ?? '';
  const status = Object.keys(DETENTION_STATUS_LABELS).includes(rawStatus) ? rawStatus as DetentionRequestStatus : undefined;
  const incidentId = positiveId(params.get('incidentId'));
  const requestedPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const [input, setInput] = useState(q);
  const [result, setResult] = useState<DetentionRequestListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const changeFilter = (key: string, value: string) => {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (value) { next.set(key, value); } else { next.delete(key); }
      next.delete('page'); return next;
    });
  };
  useEffect(() => setInput(q), [q]);
  useEffect(() => {
    if (input.trim() === q) { return; }
    const timer = window.setTimeout(() => {
      setParams((previous) => {
        const next = new URLSearchParams(previous);
        if (input.trim()) { next.set('q', input.trim()); } else { next.delete('q'); }
        next.delete('page'); return next;
      });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [input, q, setParams]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError(null);
    api.listDetentionRequests(token, { q, status, incidentId, page, size: DETENTION_PAGE_SIZE })
      .then((response) => {
        if (cancelled) { return; }
        if (page >= Math.max(response.totalPages, 1) && page > 0) {
          setParams((previous) => { const next = new URLSearchParams(previous);
            next.set('page', String(Math.max(response.totalPages - 1, 0))); return next;
          }, { replace: true });
        } else { setResult(response); }
      })
      .catch((err: unknown) => { if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить заявки'); } })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, q, status, incidentId, page, setParams]);

  return <section className="card" aria-busy={loading}>
    <div className="card__header"><h2>Заявки на заключение</h2></div>
    <div className="equipment-filters">
      <div className="field equipment-search-field"><input type="search" className="field__control" aria-label="Поиск заявок"
        placeholder="Имя или описание субъекта" value={input} onChange={(event) => setInput(event.target.value)} /></div>
      <FormField label="Статус"><select value={status ?? ''} onChange={(event) => changeFilter('status', event.target.value)}>
        <option value="">Все статусы</option>
        {Object.entries(DETENTION_STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
      </select></FormField>
      <FormField label="Инцидент-основание"><input type="number" min={1} step={1} value={incidentId ?? ''}
        onChange={(event) => changeFilter('incidentId', String(positiveId(event.target.value) ?? ''))} /></FormField>
    </div>
    {error && <div className="alert alert--error" role="alert">{error}</div>}
    {loading && <LoadingBlock label="Загрузка заявок…" skeleton rows={4} />}
    {!loading && !error && result && (result.items.length ? <DetentionRequestTable token={token} items={result.items}
      search={`?${params.toString()}`} canReadIncidents={canReadIncidents} /> : <EmptyState title="Заявки не найдены" />)}
    {!error && result && <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements}
      loading={loading} onChange={(nextPage) => setParams((previous) => { const next = new URLSearchParams(previous);
        next.set('page', String(nextPage)); return next; })} />}
  </section>;
}
