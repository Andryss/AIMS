import React, { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { Alien } from '../types';
import { AlienEditorModal } from './AlienEditorModal';
import { Button } from './ui/Button';
import { LoadingBlock } from './ui/LoadingBlock';

interface AlienDetailPageProps {
  token: string;
  canWrite: boolean;
  canBrowseList?: boolean;
}

export function AlienDetailPage({ token, canWrite, canBrowseList = true }: AlienDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const location = useLocation();
  const alienId = Number(id);
  const [alien, setAlien] = useState<Alien | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const incidentId = location.state?.incidentId;
  const fromIncident = Number.isSafeInteger(incidentId) && incidentId > 0;
  const backTo = fromIncident ? `/incidents/${incidentId}`
    : canBrowseList ? `/aliens${location.search}` : '/incidents';
  const backLabel = fromIncident ? '← К инциденту'
    : canBrowseList ? '← К списку' : '← К инцидентам';

  useEffect(() => {
    setAlien(null);
    setEditorOpen(false);
    setError(null);
    if (!Number.isSafeInteger(alienId) || alienId <= 0) {
      setError('Некорректный идентификатор вида');
      setLoading(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    api.getAlien(token, alienId)
      .then((response) => { if (!cancelled) { setAlien(response); } })
      .catch((err: unknown) => {
        if (!cancelled) {
          setError(err instanceof Error ? err.message : 'Не удалось загрузить вид');
        }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, alienId]);

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header">
        <div className="knowledge-base__heading">
          <h2>{alien?.name ?? 'Карточка вида'}</h2>
          {!loading && alien && canWrite && (
            <Button variant="ghost" className="icon-button" aria-label="Редактировать вид"
              title="Редактировать вид" onClick={() => setEditorOpen(true)}>
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
                strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
                <path d="m16 3 5 5M3 21l5-1L21 7a2.1 2.1 0 0 0-5-5L3 15z" />
              </svg>
            </Button>
          )}
        </div>
        <Button variant="outline" to={backTo}>{backLabel}</Button>
      </div>
      {error && <div className="alert alert--error" role="alert" aria-live="polite">{error}</div>}
      {loading && <LoadingBlock label="Загрузка вида…" skeleton rows={3} />}
      {!loading && alien && (
        <section className="section">
          <dl className="data-list">
            <div className="data-list__row"><dt>ID</dt><dd>#{alien.id}</dd></div>
            <div className="data-list__row"><dt>Уровень угрозы</dt><dd>{alien.threatLevel}</dd></div>
            <div className="data-list__row"><dt>Описание</dt><dd className="text-pre-wrap">{alien.description}</dd></div>
          </dl>
        </section>
      )}
      {alien && <AlienEditorModal token={token} open={editorOpen && canWrite} alien={alien}
        onClose={() => setEditorOpen(false)} onSaved={setAlien} />}
    </section>
  );
}
