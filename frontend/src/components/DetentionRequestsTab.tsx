import { Card, CardHeader } from './ui/Card';
import React from 'react';
import * as api from '../api/client';
import { parseEnumValue, positiveId } from '../utils/params';
import { useListSearchParams } from '../hooks/useListSearchParams';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { useDebouncedQuery } from '../hooks/useDebouncedSearch';
import { DETENTION_PAGE_SIZE, DETENTION_STATUS_LABELS } from '../detentionLabels';
import { DetentionRequestTable } from './DetentionRequestTable';
import { EmptyState } from './ui/EmptyState';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

export function DetentionRequestsTab({ token, canReadIncidents = true }: { token: string; canReadIncidents?: boolean }) {
  const { params, page, setFilter: changeFilter, setPage } = useListSearchParams();
  const q = params.get('q') ?? '';
  const status = parseEnumValue(DETENTION_STATUS_LABELS, params.get('status'));
  const incidentId = positiveId(params.get('incidentId'));

  const { result, loading, error } = usePaginatedResource({
    scopeKey: JSON.stringify([token, q, status, incidentId]), page,
    load: (requestedPage) => api.listDetentionRequests(token, { q, status, incidentId, page: requestedPage, size: DETENTION_PAGE_SIZE }),
    errorMessage: 'Не удалось загрузить заявки', onPageCorrection: (nextPage) => setPage(nextPage, { replace: true }),
  });
  const { input, setInput } = useDebouncedQuery(q, (query) => changeFilter('q', query));
  return <Card  aria-busy={loading}>
    <CardHeader><h2>Заявки на заключение</h2></CardHeader>
    <div className="filter-bar">
      <div className="field filter-search-field"><input type="search" className="field__control" aria-label="Поиск заявок"
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
      loading={loading} onChange={setPage} />}
  </Card>;
}
