import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { Alien, IncidentComment, IncidentHistoryEntry, IncidentResponse } from '../types';
import { buildIncidentHistoryDiffs } from '../utils/incidentHistoryDiff';
import { AlienPickerDrawer } from './AlienPickerDrawer';
import { IncidentStatusSelect } from './IncidentStatusSelect';

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('ru-RU');
  } catch {
    return iso;
  }
}

type DetailTab = 'comments' | 'history';

interface IncidentDetailPageProps {
  token: string;
  roles: string[];
  canChangeStatus: boolean;
  canReadAliens: boolean;
  canLinkAlien: boolean;
  canComment: boolean;
}

export function IncidentDetailPage({
  token,
  roles,
  canChangeStatus,
  canReadAliens,
  canLinkAlien,
  canComment,
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

  const [activeTab, setActiveTab] = useState<DetailTab>('comments');
  const [comments, setComments] = useState<IncidentComment[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);

  const [historyEntries, setHistoryEntries] = useState<IncidentHistoryEntry[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const [alienNames, setAlienNames] = useState<Record<number, string>>({});

  const loadComments = useCallback(async () => {
    if (!Number.isFinite(incidentId)) {
      return;
    }
    setCommentsLoading(true);
    try {
      const response = await api.listIncidentComments(token, incidentId);
      setComments(response.items);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось загрузить комментарии';
      setError(message);
    } finally {
      setCommentsLoading(false);
    }
  }, [incidentId, token]);

  const loadHistory = useCallback(async () => {
    if (!Number.isFinite(incidentId)) {
      return;
    }
    setHistoryLoading(true);
    try {
      const response = await api.listIncidentHistory(token, incidentId);
      setHistoryEntries(response.items);
      setHistoryLoaded(true);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось загрузить историю';
      setError(message);
    } finally {
      setHistoryLoading(false);
    }
  }, [incidentId, token]);

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
    setHistoryLoaded(false);
    setHistoryEntries([]);
    setComments([]);

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

  useEffect(() => {
    if (!incident || loading) {
      return;
    }
    loadComments();
  }, [incident, loading, loadComments]);

  useEffect(() => {
    if (activeTab === 'history' && !historyLoaded && incident && !loading) {
      loadHistory();
    }
  }, [activeTab, historyLoaded, incident, loading, loadHistory]);

  const historyDiffs = useMemo(
    () => buildIncidentHistoryDiffs(historyEntries),
    [historyEntries],
  );

  useEffect(() => {
    if (!canReadAliens || historyEntries.length === 0) {
      return;
    }
    const alienIds = new Set<number>();
    historyEntries.forEach((entry) => {
      if (entry.snapshot.alienId != null) {
        alienIds.add(entry.snapshot.alienId);
      }
    });
    const missing = Array.from(alienIds).filter((id) => alienNames[id] == null);
    if (missing.length === 0) {
      return;
    }
    let cancelled = false;
    (async () => {
      const resolved: Record<number, string> = {};
      await Promise.all(
        missing.map(async (alienId) => {
          try {
            const alien = await api.getAlien(token, alienId);
            resolved[alienId] = alien.name;
          } catch {
            resolved[alienId] = `#${alienId}`;
          }
        }),
      );
      if (!cancelled) {
        setAlienNames((prev) => ({ ...prev, ...resolved }));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [alienNames, canReadAliens, historyEntries, token]);

  const formatAlienLabel = (alienId: number | null | undefined): string => {
    if (alienId == null) {
      return '—';
    }
    const name = alienNames[alienId];
    return name ? `${name} (#${alienId})` : `#${alienId}`;
  };

  const enrichAlienDiffValue = (label: string, value: string): string => {
    if (label !== 'Тип инопланетянина' || value === '—') {
      return value;
    }
    const match = value.match(/^#(\d+)$/);
    if (!match) {
      return value;
    }
    return formatAlienLabel(Number(match[1]));
  };

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

  const handleAddComment = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!incident || !commentText.trim() || commentSubmitting) {
      return;
    }
    setCommentSubmitting(true);
    setError(null);
    try {
      const created = await api.createIncidentComment(token, incident.id, commentText.trim());
      setComments((prev) => [...prev, created]);
      setCommentText('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось добавить комментарий';
      setError(message);
    } finally {
      setCommentSubmitting(false);
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
        <>
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
                      loadComments();
                      if (historyLoaded) {
                        loadHistory();
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

          <div className="incident-tabs">
            <div className="incident-tabs-bar" role="tablist" aria-label="Комментарии и история">
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'comments'}
                className={activeTab === 'comments' ? 'incident-tab is-active' : 'incident-tab'}
                onClick={() => setActiveTab('comments')}
              >
                Комментарии
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={activeTab === 'history'}
                className={activeTab === 'history' ? 'incident-tab is-active' : 'incident-tab'}
                onClick={() => setActiveTab('history')}
              >
                История
              </button>
            </div>

            {activeTab === 'comments' && (
              <div className="incident-tab-panel" role="tabpanel">
                {commentsLoading && <p className="panel-muted">Загрузка комментариев…</p>}
                {!commentsLoading && comments.length === 0 && (
                  <p className="panel-muted">Комментариев пока нет.</p>
                )}
                <ul className="comment-list">
                  {comments.map((comment) => (
                    <li key={comment.id} className="comment-list-item">
                      <div className="comment-list-meta">
                        <strong>{comment.authorLogin}</strong>
                        <span className="panel-muted">{formatDate(comment.createdAt)}</span>
                      </div>
                      <p className="comment-list-text">{comment.text}</p>
                    </li>
                  ))}
                </ul>

                {canComment && (
                  <form className="comment-form" onSubmit={handleAddComment}>
                    <label htmlFor="incident-comment-text">Добавить комментарий</label>
                    <textarea
                      id="incident-comment-text"
                      value={commentText}
                      onChange={(e) => setCommentText(e.target.value)}
                      rows={3}
                      disabled={commentSubmitting}
                    />
                    <button type="submit" disabled={commentSubmitting || !commentText.trim()}>
                      {commentSubmitting ? 'Отправка…' : 'Отправить'}
                    </button>
                  </form>
                )}
              </div>
            )}

            {activeTab === 'history' && (
              <div className="incident-tab-panel" role="tabpanel">
                {historyLoading && <p className="panel-muted">Загрузка истории…</p>}
                {!historyLoading && historyDiffs.length === 0 && (
                  <p className="panel-muted">История изменений пуста.</p>
                )}
                <ol className="history-timeline">
                  {historyDiffs.map((block) => (
                    <li key={block.entry.id} className="history-timeline-item">
                      <div className="history-timeline-header">
                        {block.isCreation ? 'Создание инцидента · ' : ''}
                        {block.title}
                      </div>
                      {block.rows.length === 0 && !block.isCreation && (
                        <p className="panel-muted">Без изменений полей</p>
                      )}
                      <ul className="history-diff-list">
                        {block.rows.map((row) => (
                          <li key={`${block.entry.id}-${row.label}`} className="history-diff-row">
                            <span className="history-diff-label">{row.label}:</span>
                            {block.isCreation ? (
                              <span>{enrichAlienDiffValue(row.label, row.newValue)}</span>
                            ) : (
                              <span>
                                {enrichAlienDiffValue(row.label, row.oldValue)}
                                {' → '}
                                {enrichAlienDiffValue(row.label, row.newValue)}
                              </span>
                            )}
                          </li>
                        ))}
                      </ul>
                    </li>
                  ))}
                </ol>
              </div>
            )}
          </div>
        </>
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
