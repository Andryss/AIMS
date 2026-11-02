import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useSearchParams } from 'react-router-dom';
import * as api from '../api/client';
import { AlienListResponse } from '../types';
import { AlienEditorModal } from './AlienEditorModal';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';

const PAGE_SIZE = 10;

interface AlienKnowledgeBaseTabProps {
  token: string;
  canWrite: boolean;
}

export function AlienKnowledgeBaseTab({ token, canWrite }: AlienKnowledgeBaseTabProps) {
  const navigate = useNavigate();
  const [params, setParams] = useSearchParams();
  const q = params.get('q') ?? '';
  const requestedPage = Number(params.get('page') ?? 0);
  const page = Number.isSafeInteger(requestedPage) && requestedPage >= 0 ? requestedPage : 0;
  const [searchInput, setSearchInput] = useState(q);
  const [result, setResult] = useState<AlienListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [revision, setRevision] = useState(0);

  useEffect(() => setSearchInput(q), [q]);

  useEffect(() => {
    if (searchInput.trim() === q) {
      return;
    }
    const timer = window.setTimeout(() => {
      setParams((previous) => {
        const next = new URLSearchParams(previous);
        const query = searchInput.trim();
        if (query) {
          next.set('q', query);
        } else {
          next.delete('q');
        }
        next.delete('page');
        return next;
      });
    }, 300);
    return () => window.clearTimeout(timer);
  }, [searchInput, q, setParams]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    api.listAliens(token, { q, page, size: PAGE_SIZE })
      .then((response) => {
        if (!cancelled) {
          if (page >= Math.max(response.totalPages, 1) && page > 0) {
            setParams((previous) => {
              const next = new URLSearchParams(previous);
              next.set('page', String(Math.max(response.totalPages - 1, 0)));
              return next;
            }, { replace: true });
          } else {
            setResult(response);
          }
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Не удалось загрузить базу знаний');
        }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, q, page, revision, setParams]);

  const changePage = (nextPage: number) => {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      next.set('page', String(nextPage));
      return next;
    });
  };

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header">
        <h2>База знаний</h2>
        {canWrite && <Button onClick={() => setEditorOpen(true)}>Добавить вид</Button>}
      </div>
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
        <div className="incidents-table-wrapper">
          <table className="incidents-table">
            <thead><tr><th>ID</th><th>Название</th><th>Описание</th><th>Угроза</th></tr></thead>
            <tbody>
              {result.items.map((alien) => {
                const path = `/aliens/${alien.id}?${params.toString()}`;
                return (
                  <tr key={alien.id} className="incidents-table__row" onClick={() => navigate(path)}>
                    <td>#{alien.id}</td>
                    <td><Link to={path} className="knowledge-base__name"
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
      {!error && result != null && result.totalPages > 0 && (
        <div className="pagination">
          <Button variant="secondary" disabled={loading || page === 0} onClick={() => changePage(page - 1)}>
            ← Предыдущая
          </Button>
          <span className="pagination__info">Страница {page + 1} из {result.totalPages} ({result.totalElements} всего)</span>
          <Button variant="secondary" disabled={loading || page >= result.totalPages - 1}
            onClick={() => changePage(page + 1)}>Следующая →</Button>
        </div>
      )}
      <AlienEditorModal token={token} open={editorOpen && canWrite} alien={null}
        onClose={() => setEditorOpen(false)} onSaved={() => setRevision((value) => value + 1)} />
    </section>
  );
}
