import React, { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { Alien, IncidentResponse } from '../types';
import { AlienPickerDrawer } from './AlienPickerDrawer';
import { IncidentStatusSelect } from './IncidentStatusSelect';

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('ru-RU');
  } catch {
    return iso;
  }
}

interface IncidentDetailPageProps {
  token: string;
  roles: string[];
  canChangeStatus: boolean;
  canReadAliens: boolean;
  canLinkAlien: boolean;
}

export function IncidentDetailPage({
  token,
  roles,
  canChangeStatus,
  canReadAliens,
  canLinkAlien,
}: IncidentDetailPageProps) {
  const { id: idParam } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const incidentId = idParam ? Number(idParam) : NaN;

  const [incident, setIncident] = useState<IncidentResponse | null>(null);
  const [linkedAlien, setLinkedAlien] = useState<Alien | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const [alienDrawerOpen, setAlienDrawerOpen] = useState(false);
  const [linkLoading, setLinkLoading] = useState(false);

  useEffect(() => {
    if (!Number.isFinite(incidentId)) {
      setError('Некорректный идентификатор инцидента');
      setIncident(null);
      setLinkedAlien(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    (async () => {
      try {
        const data = await api.getIncident(token, incidentId);
        if (cancelled) {
          return;
        }
        setIncident(data);
        if (canReadAliens && data.alienId != null) {
          const alien = await api.getAlien(token, data.alienId);
          if (!cancelled) {
            setLinkedAlien(alien);
          }
        } else {
          setLinkedAlien(null);
        }
      } catch (err: unknown) {
        if (cancelled) {
          return;
        }
        const message = err instanceof Error ? err.message : 'Не удалось загрузить инцидент';
        setError(message);
        setIncident(null);
        setLinkedAlien(null);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [canReadAliens, incidentId, token]);

  const handleDownload = async (fileId: number) => {
    setDownloadingId(fileId);
    try {
      await api.downloadFile(token, fileId, `attachment-${fileId}`);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось скачать файл';
      setError(message);
    } finally {
      setDownloadingId(null);
    }
  };

  const handleLinkAlien = async (alienId: number) => {
    if (!incident) {
      return;
    }
    setLinkLoading(true);
    setError(null);
    try {
      const updated = await api.putIncidentAlien(token, incident.id, alienId);
      setIncident(updated);
      const alien = await api.getAlien(token, alienId);
      setLinkedAlien(alien);
      setAlienDrawerOpen(false);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось привязать тип';
      setError(message);
    } finally {
      setLinkLoading(false);
    }
  };

  return (
    <section className="tab-panel incident-detail-page">
      <div className="tab-panel-header">
        <h2>{incident ? `Инцидент #${incident.id}` : 'Инцидент'}</h2>
        <Link to="/incidents" className="secondary link-button">
          ← К списку
        </Link>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p className="panel-muted">Загрузка…</p>}

      {incident && !loading && (
        <div className="incident-view">
          <dl className="incident-view-fields">
            <div className="incident-view-row">
              <dt>Статус</dt>
              <dd>
                <IncidentStatusSelect
                  token={token}
                  incident={incident}
                  roles={roles}
                  canChange={canChangeStatus}
                  className="status-select-wrap-modal"
                  onStatusChanged={(updated) => {
                    setIncident(updated);
                    if (updated.status !== 'READY_FOR_ANALYSIS') {
                      setAlienDrawerOpen(false);
                    }
                  }}
                />
              </dd>
            </div>
            <div className="incident-view-row">
              <dt>Тип события</dt>
              <dd>{EVENT_TYPE_LABELS[incident.eventType] ?? incident.eventType}</dd>
            </div>
            <div className="incident-view-row">
              <dt>Место</dt>
              <dd>{incident.location}</dd>
            </div>
            <div className="incident-view-row">
              <dt>Время обнаружения</dt>
              <dd>{formatDate(incident.detectedAt)}</dd>
            </div>
            <div className="incident-view-row">
              <dt>Описание</dt>
              <dd className="incident-view-description">{incident.description}</dd>
            </div>

            {(canReadAliens && (canLinkAlien || linkedAlien)) && (
              <div className="incident-view-row">
                <dt>Тип инопланетянина</dt>
                <dd>
                  {linkedAlien ? (
                    <div className="alien-linked">
                      <strong>{linkedAlien.name}</strong>
                      <span className="panel-muted">
                        {' '}
                        (угроза {linkedAlien.threatLevel}/10)
                      </span>
                      <p className="incident-view-description">{linkedAlien.description}</p>
                    </div>
                  ) : canLinkAlien && incident.status === 'READY_FOR_ANALYSIS' ? (
                    <button
                      type="button"
                      className="alien-picker-trigger"
                      onClick={() => setAlienDrawerOpen(true)}
                    >
                      Выбрать
                    </button>
                  ) : (
                    '—'
                  )}
                </dd>
              </div>
            )}

            <div className="incident-view-row">
              <dt>Вложения</dt>
              <dd>
                {incident.attachmentFileIds.length === 0 ? (
                  '—'
                ) : (
                  <ul className="incident-view-attachments">
                    {incident.attachmentFileIds.map((fileId) => (
                      <li key={fileId}>
                        <button
                          type="button"
                          className="link-button"
                          disabled={downloadingId === fileId}
                          onClick={() => handleDownload(fileId)}
                        >
                          {downloadingId === fileId
                            ? `Скачивание #${fileId}…`
                            : `Файл #${fileId}`}
                        </button>
                      </li>
                    ))}
                  </ul>
                )}
              </dd>
            </div>
            <div className="incident-view-row">
              <dt>Создан</dt>
              <dd>{formatDate(incident.createdAt)}</dd>
            </div>
            <div className="incident-view-row">
              <dt>Обновлён</dt>
              <dd>{formatDate(incident.updatedAt)}</dd>
            </div>
          </dl>
        </div>
      )}

      <AlienPickerDrawer
        token={token}
        open={
          alienDrawerOpen
          && canReadAliens
          && canLinkAlien
          && incident?.status === 'READY_FOR_ANALYSIS'
          && incident.alienId == null
        }
        selecting={linkLoading}
        onClose={() => setAlienDrawerOpen(false)}
        onSelect={handleLinkAlien}
      />

      {!loading && !incident && !error && (
        <p className="panel-muted">
          Инцидент не найден.{' '}
          <button type="button" className="link-button" onClick={() => navigate('/incidents')}>
            Вернуться к списку
          </button>
        </p>
      )}
    </section>
  );
}
