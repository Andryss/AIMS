import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { Alien, CleanupReportResponse, IncidentComment, IncidentHistoryEntry, IncidentResponse, UserSummary } from '../types';
import { buildIncidentHistoryDiffs } from '../utils/incidentHistoryDiff';
import {
  collectIncidentUserIds,
  loadUsersMap,
} from '../utils/incidentUsers';
import { AlienPickerDrawer } from './AlienPickerDrawer';
import { AttachmentDownloadList } from './AttachmentDownloadList';
import { CleanupReportDrawer } from './CleanupReportDrawer';
import { CleanupStatusSelect } from './CleanupStatusSelect';
import { IncidentStatusSelect } from './IncidentStatusSelect';
import { UserChip } from './UserChip';
import { UserPickerDrawer } from './UserPickerDrawer';

function userLoginFromMap(userId: number, usersMap: Map<number, string>): string {
  return usersMap.get(userId) ?? `#${userId}`;
}

function sameUserIdSets(a: number[], b: number[]): boolean {
  if (a.length !== b.length) {
    return false;
  }
  const sortedA = [...a].sort((x, y) => x - y);
  const sortedB = [...b].sort((x, y) => x - y);
  return sortedA.every((id, index) => id === sortedB[index]);
}

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
  currentUserId?: number;
  canChangeStatus: boolean;
  canReadAliens: boolean;
  canLinkAlien: boolean;
  canComment: boolean;
  canAssign: boolean;
  canReadCleanupReport: boolean;
  canCreateCleanupReport: boolean;
  canChangeCleanupStatus: boolean;
}

export function IncidentDetailPage({
  token,
  roles,
  currentUserId,
  canChangeStatus,
  canReadAliens,
  canLinkAlien,
  canComment,
  canAssign,
  canReadCleanupReport,
  canCreateCleanupReport,
  canChangeCleanupStatus,
}: IncidentDetailPageProps) {
  const { id: idParam } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const incidentId = idParam ? Number(idParam) : NaN;

  const [incident, setIncident] = useState<IncidentResponse | null>(null);
  const [linkedAlien, setLinkedAlien] = useState<Alien | null>(null);
  const [cleanupReport, setCleanupReport] = useState<CleanupReportResponse | null>(null);
  const [cleanupDrawerOpen, setCleanupDrawerOpen] = useState(false);
  const [cleanupDrawerMode, setCleanupDrawerMode] = useState<'create' | 'view'>('view');
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
  const [usersMap, setUsersMap] = useState<Map<number, string>>(new Map());
  const [usersLoading, setUsersLoading] = useState(false);

  const [responsiblePickerOpen, setResponsiblePickerOpen] = useState(false);
  const [executorPickerOpen, setExecutorPickerOpen] = useState(false);
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [responsibleEditing, setResponsibleEditing] = useState(false);
  const [responsibleDraftId, setResponsibleDraftId] = useState<number | null>(null);
  const [responsibleRemoved, setResponsibleRemoved] = useState(false);
  const [executorsEditing, setExecutorsEditing] = useState(false);
  const [executorDraftIds, setExecutorDraftIds] = useState<number[]>([]);
  const [executorRemovedIds, setExecutorRemovedIds] = useState<Set<number>>(() => new Set());
  const responsibleEditRef = useRef<HTMLDivElement>(null);
  const executorEditRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setResponsibleEditing(false);
    setResponsiblePickerOpen(false);
    setResponsibleDraftId(null);
    setResponsibleRemoved(false);
    setExecutorsEditing(false);
    setExecutorPickerOpen(false);
    setExecutorDraftIds([]);
    setExecutorRemovedIds(new Set());
  }, [incidentId]);

  const canEditAssignment = canAssign && (
    incident?.status === 'READY_FOR_EXECUTION'
    || incident?.status === 'PREPARATION_FOR_EXECUTION'
  );

  const showCleanupBlock = incident?.status === 'EXECUTING'
    || incident?.status === 'EXECUTION_COMPLETED';

  const showCleanupStatusRow = showCleanupBlock && (
    incident?.cleanupStatus != null || canChangeCleanupStatus
  );

  const showCleanupReportRow = showCleanupBlock && (
    incident?.cleanupReportId != null || canCreateCleanupReport
  );

  const showResponsibleRow = canEditAssignment || incident?.responsibleUserId != null;
  const showExecutorsRow = canEditAssignment || (incident?.executorUserIds ?? []).length > 0;
  const responsibleDraftAssigned = responsibleDraftId != null && !responsibleRemoved;

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
      setCleanupReport(null);
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
        if (canReadCleanupReport && data.cleanupReportId != null) {
          const report = await api.getCleanupReport(token, incidentId);
          if (!cancelled) {
            setCleanupReport(report);
          }
        } else {
          setCleanupReport(null);
        }
      } catch (err: unknown) {
        if (cancelled) {
          return;
        }
        const message = err instanceof Error ? err.message : 'Не удалось загрузить инцидент';
        setError(message);
        setIncident(null);
        setLinkedAlien(null);
        setCleanupReport(null);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [canReadAliens, canReadCleanupReport, incidentId, token]);

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
    () => buildIncidentHistoryDiffs(historyEntries, usersMap),
    [historyEntries, usersMap],
  );

  const collectedUserIds = useMemo(
    () => collectIncidentUserIds(incident, historyEntries, comments),
    [incident, historyEntries, comments],
  );

  const collectedUserIdsKey = useMemo(
    () => collectedUserIds.slice().sort((a, b) => a - b).join(','),
    [collectedUserIds],
  );

  useEffect(() => {
    if (collectedUserIds.length === 0) {
      setUsersMap(new Map());
      return;
    }
    let cancelled = false;
    setUsersLoading(true);
    loadUsersMap(token, collectedUserIds, api.batchUsers)
      .then((map) => {
        if (!cancelled) {
          setUsersMap(map);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setUsersMap(new Map());
        }
      })
      .finally(() => {
        if (!cancelled) {
          setUsersLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [collectedUserIdsKey, token]);

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

  const handleSelectResponsibleFromPicker = (user: UserSummary) => {
    setUsersMap((prev) => {
      const next = new Map(prev);
      next.set(user.id, user.login);
      return next;
    });
    setResponsibleDraftId(user.id);
    setResponsibleRemoved(false);
    setResponsiblePickerOpen(false);
  };

  const startResponsibleEdit = () => {
    if (!incident || !canEditAssignment || assignmentLoading) {
      return;
    }
    setResponsibleDraftId(incident.responsibleUserId ?? null);
    setResponsibleRemoved(false);
    setResponsibleEditing(true);
  };

  const commitResponsibleEdit = useCallback(async () => {
    if (!incident || !responsibleEditing) {
      return;
    }

    const finalId = responsibleDraftId != null && !responsibleRemoved ? responsibleDraftId : null;
    const currentId = incident.responsibleUserId ?? null;

    setResponsibleEditing(false);
    setResponsiblePickerOpen(false);

    if (finalId === currentId) {
      return;
    }

    setAssignmentLoading(true);
    setError(null);
    try {
      const updated = await api.setIncidentResponsible(token, incident.id, finalId);
      setIncident(updated);
      if (historyLoaded) {
        loadHistory();
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось обновить ответственного';
      setError(message);
      setResponsibleDraftId(incident.responsibleUserId ?? null);
      setResponsibleRemoved(false);
      setResponsibleEditing(true);
    } finally {
      setAssignmentLoading(false);
    }
  }, [
    incident,
    responsibleEditing,
    responsibleDraftId,
    responsibleRemoved,
    token,
    historyLoaded,
    loadHistory,
  ]);

  const toggleResponsibleRemoved = () => {
    setResponsibleRemoved((prev) => !prev);
  };

  const handleAddExecutorFromPicker = (user: UserSummary) => {
    setUsersMap((prev) => {
      const next = new Map(prev);
      next.set(user.id, user.login);
      return next;
    });

    setExecutorDraftIds((prev) => (prev.includes(user.id) ? prev : [...prev, user.id]));
    setExecutorRemovedIds((prev) => {
      const next = new Set(prev);
      next.delete(user.id);
      return next;
    });
    setExecutorPickerOpen(false);
  };

  const startExecutorEdit = () => {
    if (!incident || !canEditAssignment || assignmentLoading) {
      return;
    }
    setExecutorDraftIds([...(incident.executorUserIds ?? [])]);
    setExecutorRemovedIds(new Set());
    setExecutorsEditing(true);
  };

  const commitExecutorEdit = useCallback(async () => {
    if (!incident || !executorsEditing) {
      return;
    }

    const finalIds = executorDraftIds.filter((id) => !executorRemovedIds.has(id));
    const currentIds = incident.executorUserIds ?? [];

    setExecutorsEditing(false);
    setExecutorPickerOpen(false);

    if (sameUserIdSets(finalIds, currentIds)) {
      return;
    }

    setAssignmentLoading(true);
    setError(null);
    try {
      const updated = await api.setIncidentExecutors(token, incident.id, finalIds);
      setIncident(updated);
      if (historyLoaded) {
        loadHistory();
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось обновить исполнителей';
      setError(message);
      setExecutorDraftIds([...(incident.executorUserIds ?? [])]);
      setExecutorRemovedIds(new Set());
      setExecutorsEditing(true);
    } finally {
      setAssignmentLoading(false);
    }
  }, [
    executorsEditing,
    executorDraftIds,
    executorRemovedIds,
    incident,
    token,
    historyLoaded,
    loadHistory,
  ]);

  useEffect(() => {
    if (!executorsEditing && !responsibleEditing) {
      return;
    }

    const onMouseDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (document.querySelector('.drawer-overlay')?.contains(target)) {
        return;
      }
      if (executorsEditing && !executorEditRef.current?.contains(target)) {
        void commitExecutorEdit();
      }
      if (responsibleEditing && !responsibleEditRef.current?.contains(target)) {
        void commitResponsibleEdit();
      }
    };

    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [
    commitExecutorEdit,
    commitResponsibleEdit,
    executorsEditing,
    responsibleEditing,
  ]);

  const toggleExecutorRemoved = (userId: number) => {
    setExecutorRemovedIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
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
        <Link to="/incidents" className="outline-button">
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
                    currentUserId={currentUserId}
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

              {showCleanupStatusRow && (
                <div className="incident-view-row">
                  <dt>Статус очистки</dt>
                  <dd>
                    <CleanupStatusSelect
                      token={token}
                      incident={incident}
                      canChange={canChangeCleanupStatus}
                      className="status-select-wrap-modal"
                      onStatusChanged={(updated) => {
                        setIncident(updated);
                        if (historyLoaded) {
                          loadHistory();
                        }
                      }}
                    />
                  </dd>
                </div>
              )}

              {showCleanupReportRow && (
                <div className="incident-view-row">
                  <dt>Отчёт об очистке</dt>
                  <dd>
                    {incident.cleanupReportId != null ? (
                      <button
                        type="button"
                        className="alien-picker-trigger"
                        onClick={() => {
                          setCleanupDrawerMode('view');
                          setCleanupDrawerOpen(true);
                        }}
                      >
                        Открыть
                      </button>
                    ) : canCreateCleanupReport ? (
                      <button
                        type="button"
                        className="alien-picker-trigger"
                        onClick={() => {
                          setCleanupDrawerMode('create');
                          setCleanupDrawerOpen(true);
                        }}
                      >
                        Прикрепить отчёт
                      </button>
                    ) : (
                      '—'
                    )}
                  </dd>
                </div>
              )}

              {showResponsibleRow && (
                <div className="incident-view-row">
                  <dt>Ответственный</dt>
                  <dd
                    className={
                      canEditAssignment && !responsibleEditing
                        ? 'executors-field executors-field--clickable'
                        : 'executors-field'
                    }
                    onClick={canEditAssignment && !responsibleEditing ? startResponsibleEdit : undefined}
                    onKeyDown={
                      canEditAssignment && !responsibleEditing
                        ? (event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            startResponsibleEdit();
                          }
                        }
                        : undefined
                    }
                    role={canEditAssignment && !responsibleEditing ? 'button' : undefined}
                    tabIndex={canEditAssignment && !responsibleEditing ? 0 : undefined}
                  >
                    {usersLoading && incident.responsibleUserId != null && !responsibleEditing && (
                      <span className="panel-muted">Загрузка… </span>
                    )}
                    {responsibleEditing ? (
                      <div ref={responsibleEditRef} className="executors-edit-box">
                        <div className="user-chip-group">
                          {responsibleDraftAssigned && responsibleDraftId != null ? (
                            <UserChip
                              login={userLoginFromMap(responsibleDraftId, usersMap)}
                              removable
                              removed={responsibleRemoved}
                              onRemove={toggleResponsibleRemoved}
                            />
                          ) : (
                            <span className="panel-muted">Назначьте ответственного</span>
                          )}
                        </div>
                        {!responsibleDraftAssigned && (
                          <button
                            type="button"
                            className="executors-add-btn"
                            aria-label="Выбрать ответственного"
                            disabled={assignmentLoading}
                            onClick={(event) => {
                              event.stopPropagation();
                              setResponsiblePickerOpen(true);
                            }}
                          >
                            +
                          </button>
                        )}
                      </div>
                    ) : (
                      <div className="user-chip-group">
                        {incident.responsibleUserId != null ? (
                          <UserChip login={userLoginFromMap(incident.responsibleUserId, usersMap)} />
                        ) : (
                          canEditAssignment && (
                            <span className="panel-muted">Нажмите, чтобы назначить ответственного</span>
                          )
                        )}
                      </div>
                    )}
                  </dd>
                </div>
              )}

              {showExecutorsRow && (
                <div className="incident-view-row">
                  <dt>Исполнители</dt>
                <dd
                  className={
                    canEditAssignment && !executorsEditing
                      ? 'executors-field executors-field--clickable'
                      : 'executors-field'
                  }
                  onClick={canEditAssignment && !executorsEditing ? startExecutorEdit : undefined}
                  onKeyDown={
                    canEditAssignment && !executorsEditing
                      ? (event) => {
                        if (event.key === 'Enter' || event.key === ' ') {
                          event.preventDefault();
                          startExecutorEdit();
                        }
                      }
                      : undefined
                  }
                  role={canEditAssignment && !executorsEditing ? 'button' : undefined}
                  tabIndex={canEditAssignment && !executorsEditing ? 0 : undefined}
                >
                  {usersLoading && (incident.executorUserIds ?? []).length > 0 && !executorsEditing && (
                    <span className="panel-muted">Загрузка… </span>
                  )}
                  {executorsEditing ? (
                    <div ref={executorEditRef} className="executors-edit-box">
                      <div className="user-chip-group">
                        {executorDraftIds.length === 0 && (
                          <span className="panel-muted">Добавьте исполнителей</span>
                        )}
                        {executorDraftIds.map((userId) => (
                          <UserChip
                            key={userId}
                            login={userLoginFromMap(userId, usersMap)}
                            removable
                            removed={executorRemovedIds.has(userId)}
                            onRemove={() => toggleExecutorRemoved(userId)}
                          />
                        ))}
                      </div>
                      <button
                        type="button"
                        className="executors-add-btn"
                        aria-label="Добавить исполнителя"
                        disabled={assignmentLoading}
                        onClick={(event) => {
                          event.stopPropagation();
                          setExecutorPickerOpen(true);
                        }}
                      >
                        +
                      </button>
                    </div>
                  ) : (
                    <div className="user-chip-group">
                      {(incident.executorUserIds ?? []).length === 0 ? (
                        canEditAssignment ? (
                          <span className="panel-muted">Нажмите, чтобы назначить исполнителей</span>
                        ) : (
                          '—'
                        )
                      ) : (
                        (incident.executorUserIds ?? []).map((userId) => (
                          <UserChip
                            key={userId}
                            login={userLoginFromMap(userId, usersMap)}
                          />
                        ))
                      )}
                    </div>
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
                    <AttachmentDownloadList
                      fileIds={incident.attachmentFileIds}
                      downloadingId={downloadingId}
                      onDownload={handleDownload}
                    />
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
                        <UserChip login={userLoginFromMap(comment.authorUserId, usersMap)} size={24} />
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
                        <span className="history-timeline-when">{formatDate(block.entry.changedAt)}</span>
                        <span aria-hidden>·</span>
                        <UserChip
                          login={userLoginFromMap(block.entry.changedByUserId, usersMap)}
                          size={24}
                        />
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

      <UserPickerDrawer
        token={token}
        open={responsiblePickerOpen && canEditAssignment && responsibleEditing}
        title="Назначить ответственного"
        selecting={assignmentLoading}
        onClose={() => setResponsiblePickerOpen(false)}
        onSelect={handleSelectResponsibleFromPicker}
      />

      <UserPickerDrawer
        token={token}
        open={executorPickerOpen && canEditAssignment && executorsEditing}
        title="Добавить исполнителя"
        selecting={assignmentLoading}
        onClose={() => setExecutorPickerOpen(false)}
        onSelect={handleAddExecutorFromPicker}
      />

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

      {incident && (
        <CleanupReportDrawer
          token={token}
          incidentId={incident.id}
          open={cleanupDrawerOpen}
          mode={cleanupDrawerMode}
          report={cleanupReport}
          onClose={() => setCleanupDrawerOpen(false)}
          onCreated={(created) => {
            setCleanupReport(created);
            setIncident((prev) => (
              prev ? { ...prev, cleanupReportId: created.id } : prev
            ));
            if (historyLoaded) {
              loadHistory();
            }
          }}
          onDownloadFile={handleDownload}
          downloadingId={downloadingId}
        />
      )}

      {!loading && !incident && !error && (
        <>
          <p className="panel-muted incident-not-found">Инцидент не найден.</p>
          <button type="button" className="outline-button" onClick={() => navigate('/incidents')}>
            Вернуться к списку
          </button>
        </>
      )}
    </section>
  );
}
