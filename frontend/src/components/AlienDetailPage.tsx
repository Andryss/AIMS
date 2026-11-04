import { Card, CardHeader } from './ui/Card';
import { EditIcon } from './ui/EditIcon';
import React, { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { Alien } from '../types';
import { AlienEditorModal } from './AlienEditorModal';
import { Button } from './ui/Button';
import { LoadingBlock } from './ui/LoadingBlock';
import { AttachmentDownloadList } from './AttachmentDownloadList';

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
  const [downloadingId, setDownloadingId] = useState<number | null>(null);
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

  const download = async (fileId: number, fileName?: string) => {
    setDownloadingId(fileId);
    try { await api.downloadFile(token, fileId, fileName); }
    catch (err) { setError(err instanceof Error ? err.message : 'Не удалось скачать фотографию'); }
    finally { setDownloadingId(null); }
  };

  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <h2>{alien?.name ?? 'Карточка вида'}</h2>
        <div className="card__actions" role="group" aria-label="Действия вида">
          {!loading && alien && canWrite && (
            <Button variant="ghost" className="icon-button" aria-label="Редактировать вид"
              title="Редактировать вид" onClick={() => setEditorOpen(true)}>
              <EditIcon />
            </Button>
          )}
          <Button variant="outline" to={backTo}>{backLabel}</Button>
        </div>
      </CardHeader>
      {error && <div className="alert alert--error" role="alert" aria-live="polite">{error}</div>}
      {loading && <LoadingBlock label="Загрузка вида…" skeleton rows={3} />}
      {!loading && alien && (
        <section className="section">
          <dl className="data-list">
            <div className="data-list__row"><dt>ID</dt><dd>#{alien.id}</dd></div>
            <div className="data-list__row"><dt>Уровень угрозы</dt><dd>{alien.threatLevel}</dd></div>
            <div className="data-list__row"><dt>Описание</dt><dd className="text-pre-wrap">{alien.description}</dd></div>
          </dl>
          {(alien.photoFileIds?.length ?? 0) > 0 && <section className="section">
            <h3>Фотографии</h3>
            <AttachmentDownloadList token={token} fileIds={alien.photoFileIds ?? []}
              downloadingId={downloadingId} onDownload={download} />
          </section>}
        </section>
      )}
      {alien && <AlienEditorModal token={token} open={editorOpen && canWrite} alien={alien}
        onClose={() => setEditorOpen(false)} onSaved={setAlien} />}
    </Card>
  );
}
