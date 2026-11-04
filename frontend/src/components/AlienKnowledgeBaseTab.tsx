import { Card, CardHeader } from './ui/Card';
import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import * as api from '../api/client';
import { useListSearchParams } from '../hooks/useListSearchParams';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { useDebouncedQuery } from '../hooks/useDebouncedSearch';
import { AlienEditorModal } from './AlienEditorModal';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

const PAGE_SIZE = 10;

interface AlienKnowledgeBaseTabProps {
  token: string;
  canWrite: boolean;
}

export function AlienKnowledgeBaseTab({ token, canWrite }: AlienKnowledgeBaseTabProps) {
  const navigate = useNavigate();
  const { params, page, setFilter, setPage } = useListSearchParams();
  const q = params.get('q') ?? '';
  const [editorOpen, setEditorOpen] = useState(false);

  const { result, loading, error, reload } = usePaginatedResource({
    scopeKey: JSON.stringify([token, q]), page,
    load: (requestedPage) => api.listAliens(token, { q, page: requestedPage, size: PAGE_SIZE }),
    errorMessage: 'Не удалось загрузить базу знаний', onPageCorrection: (nextPage) => setPage(nextPage, { replace: true }),
  });
  const { input: searchInput, setInput: setSearchInput } = useDebouncedQuery(q, (query) => setFilter('q', query));
  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <h2>База знаний</h2>
        {canWrite && <Button onClick={() => setEditorOpen(true)}>Добавить вид</Button>}
      </CardHeader>
      <div className="field knowledge-base__search">
        <input type="search" className="field__control" aria-label="Поиск по базе знаний"
          value={searchInput} onChange={(event) => setSearchInput(event.target.value)}
          placeholder="Название или описание вида" />
      </div>
      {error && <div className="alert alert--error" role="alert" aria-live="polite">{error}</div>}
      {loading && <LoadingBlock label="Загрузка базы знаний…" skeleton rows={4} />}
      {!loading && !error && result?.items.length === 0 && (
        <EmptyState title="Виды не найдены" hint={q
          ? (canWrite ? 'Уточните запрос или добавьте новый вид.' : 'Уточните поисковый запрос.')
          : undefined} />
      )}
      {!loading && !error && result != null && result.items.length > 0 && (
        <div className="data-table-wrapper">
          <table className="data-table">
            <thead><tr><th>ID</th><th>Название</th><th>Описание</th><th>Угроза</th></tr></thead>
            <tbody>
              {result.items.map((alien) => {
                const path = `/aliens/${alien.id}?${params.toString()}`;
                return (
                  <tr key={alien.id} className="data-table__row" onClick={() => navigate(path)}>
                    <td>#{alien.id}</td>
                    <td><Link to={path} className="entity-link"
                      onClick={(event) => event.stopPropagation()}>{alien.name}</Link></td>
                    <td><span className="knowledge-base__description" title={alien.description}>
                      {alien.description}
                    </span></td>
                    <td>{alien.threatLevel}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
      {!error && result != null && <Pagination page={page} totalPages={result.totalPages}
        totalElements={result.totalElements} loading={loading} onChange={setPage} />}
      <AlienEditorModal token={token} open={editorOpen && canWrite} alien={null}
        onClose={() => setEditorOpen(false)} onSaved={reload} />
    </Card>
  );
}
