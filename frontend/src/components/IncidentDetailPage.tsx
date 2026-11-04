import { Card, CardHeader } from './ui/Card';
import { EditIcon } from './ui/EditIcon';
import React, { useCallback, useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { IncidentResponse } from '../types';
import { collectIncidentUserIds } from '../utils/incidentUsers';
import { formatDateTime as formatDate } from '../utils/dateTime';
import { useIncidentDetails } from '../hooks/useIncidentDetails';
import { useIncidentComments } from '../hooks/useIncidentComments';
import { useIncidentHistory } from '../hooks/useIncidentHistory';
import { useUsersMapState } from '../hooks/useUsersMap';
import { IncidentAssignments } from './incident/IncidentAssignments';
import { IncidentComments } from './incident/IncidentComments';
import { IncidentHistory } from './incident/IncidentHistory';
import { IncidentCleanupSection } from './incident/IncidentCleanupSection';
import { AlienSummary } from './AlienSummary';
import { IncidentAlienClassification } from './IncidentAlienClassification';
import { IncidentThreatAssessment } from './IncidentThreatAssessment';
import { IncidentEditorModal } from './IncidentEditorModal';
import { AlienPickerDrawer } from './AlienPickerDrawer';
import { AttachmentDownloadList } from './AttachmentDownloadList';
import { IncidentStatusSelect } from './IncidentStatusSelect';
import { IncidentDetentionSection } from './IncidentDetentionSection';
import { IncidentEquipmentSection } from './IncidentEquipmentSection';
import { LoadingBlock } from './ui/LoadingBlock';
import { Button } from './ui/Button';

type DetailTab = 'comments' | 'history' | 'equipment' | 'detention';

interface IncidentDetailPageProps {
  token: string;
  roles: string[];
  currentUserId?: number;
  canChangeStatus: boolean;
  canReadAliens: boolean;
  canLinkAlien: boolean;
  canClassifyAlien?: boolean;
  canCalculateThreat?: boolean;
  canEdit?: boolean;
  canComment: boolean;
  canAssign: boolean;
  canReadCleanupReport: boolean;
  canCreateCleanupReport: boolean;
  canChangeCleanupStatus: boolean;
  canReadEquipmentMovements?: boolean;
  canCreateEquipmentMovements?: boolean;
  canReadDetentions?: boolean;
  canCreateDetentions?: boolean;
}

export function IncidentDetailPage({
  token,
  roles,
  currentUserId,
  canChangeStatus,
  canReadAliens,
  canLinkAlien,
  canClassifyAlien = false,
  canCalculateThreat = false,
  canEdit = false,
  canComment,
  canAssign,
  canReadCleanupReport,
  canCreateCleanupReport,
  canChangeCleanupStatus,
  canReadEquipmentMovements = false,
  canCreateEquipmentMovements = false,
  canReadDetentions = false,
  canCreateDetentions = false,
}: IncidentDetailPageProps) {
  const { id: idParam } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const incidentId = idParam ? Number(idParam) : NaN;

  const [editorScope, setEditorScope] = useState<string | null>(null);
  const [editorRevision, setEditorRevision] = useState(0);
  const editScope = JSON.stringify([token, incidentId]);
  const { incident, setIncident, linkedAlien, setLinkedAlien, cleanupReport, setCleanupReport, loading, error, setError } =
    useIncidentDetails(token, incidentId, canReadAliens, canReadCleanupReport);
  const editable = canEdit && (incident?.status === 'DRAFT' || incident?.status === 'CLARIFICATION_REQUIRED');
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  const [alienDrawerOpen, setAlienDrawerOpen] = useState(false);
  const [linkLoading, setLinkLoading] = useState(false);
  useEffect(() => { setLinkLoading(false); setAlienDrawerOpen(false); }, [incidentId, token]);

  const [activeTab, setActiveTab] = useState<DetailTab>('comments');
  const { historyEntries, historyLoading, historyLoaded, loadHistory, invalidateHistory } =
    useIncidentHistory(token, incidentId, setError);
  const commentState = useIncidentComments(token, incidentId, incident, setError);
  const { comments, loadComments } = commentState;

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

  const { users: usersMap, setUsers: setUsersMap, loading: usersLoading } = useUsersMapState(token,
    collectIncidentUserIds(incident, historyEntries, comments));

  const handleIncidentChanged = useCallback((updated: IncidentResponse) => {
    setIncident(updated);
    if (historyLoaded) { loadHistory(); }
  }, [setIncident, historyLoaded, loadHistory]);

  const handleDownload = async (fileId: number, fileName?: string) => {
    setDownloadingId(fileId);
    try {
      await api.downloadFile(token, fileId, fileName || `attachment-${fileId}`);
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
      if (historyLoaded) {
        loadHistory();
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось привязать тип';
      setError(message);
    } finally {
      setLinkLoading(false);
    }
  };

  return (
    <Card className="incident-detail-page">
      <CardHeader>
        <h2>{incident ? `Инцидент #${incident.id}` : 'Инцидент'}</h2>
        <div className="page-actions">
          {!loading && editable && <Button variant="ghost" className="icon-button"
            aria-label="Редактировать инцидент" title="Редактировать инцидент" onClick={() => setEditorScope(editScope)}>
            <EditIcon />
          </Button>}
          <Link to="/incidents" className="btn btn--outline">← К списку</Link>
        </div>
      </CardHeader>

      {incident && !loading && editable && editorScope === editScope && <IncidentEditorModal
        key={editScope} token={token} open incident={incident} onClose={() => setEditorScope(null)}
        onConflict={(updated) => {
          setIncident(updated);
          invalidateHistory();
          setEditorRevision((previous) => previous + 1);
        }}
        onSaved={(updated) => {
          setIncident(updated);
          invalidateHistory();
          setEditorRevision((previous) => previous + 1);
          setError(null);
        }} />}

      {error && (
        <div className="alert alert--error" role="alert" aria-live="polite">
          {error}
        </div>
      )}
      {loading && <LoadingBlock label="Загрузка инцидента…" skeleton rows={5} />}

      {incident && !loading && (
        <>
          <section className="section">
            <dl className="data-list">
              <div className="data-list__row">
                <dt>Статус</dt>
                <dd>
                  <IncidentStatusSelect
                    token={token}
                    incident={incident}
                    roles={roles}
                    currentUserId={currentUserId}
                    canChange={canChangeStatus}
                    showErrorText
                    className="status-select-wrap status-select-wrap--modal"
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
              <div className="data-list__row">
                <dt>Тип события</dt>
                <dd>{EVENT_TYPE_LABELS[incident.eventType] ?? incident.eventType}</dd>
              </div>
              <div className="data-list__row">
                <dt>Место</dt>
                <dd>{incident.location}</dd>
              </div>
              <div className="data-list__row">
                <dt>Время обнаружения</dt>
                <dd>{formatDate(incident.detectedAt)}</dd>
              </div>
              <div className="data-list__row">
                <dt>Описание</dt>
                <dd className="text-pre-wrap">{incident.description}</dd>
              </div>

              <div className="data-list__row">
                <dt>Количество пострадавших</dt>
                <dd>{incident.victimCount ?? 'Неизвестно'}</dd>
              </div>

              {(canReadAliens && (canLinkAlien || canClassifyAlien || linkedAlien)) && (
                <div className="data-list__row">
                  <dt>Тип инопланетянина</dt>
                  <dd className="incident-alien-field">
                    {linkedAlien ? <AlienSummary token={token} alien={linkedAlien} incidentId={incident.id} />
                      : !(canLinkAlien && incident.status === 'READY_FOR_ANALYSIS') && <span>—</span>}
                    {canLinkAlien && incident.status === 'READY_FOR_ANALYSIS' && (
                      <Button size="sm" variant="outline" disabled={linkLoading} onClick={() => setAlienDrawerOpen(true)}>
                        {linkedAlien ? 'Изменить вид' : 'Выбрать'}
                      </Button>
                    )}
                    {canClassifyAlien && incident.status === 'READY_FOR_ANALYSIS' && (
                      <IncidentAlienClassification key={`${incidentId}:${editorRevision}`} token={token} incident={incident}
                        canAccept={canLinkAlien} disabled={linkLoading} onSavingChange={setLinkLoading}
                        onAccepted={(updated, alien) => {
                          setIncident(updated);
                          setLinkedAlien(alien);
                          invalidateHistory();
                        }} />
                    )}
                  </dd>
                </div>
              )}

              <div className="data-list__row">
                <dt>Уровень угрозы</dt>
                <dd>
                  <IncidentThreatAssessment key={`${incidentId}:${editorRevision}`} token={token} incident={incident}
                    canCalculate={canCalculateThreat} alienThreatLevel={linkedAlien?.threatLevel} disabled={linkLoading}
                    onUpdated={(updated) => {
                      setIncident(updated);
                      invalidateHistory();
                    }} />
                </dd>
              </div>

              <IncidentCleanupSection key={`${token}:${incident.id}`} token={token} incident={incident} canReadCleanupReport={canReadCleanupReport}
                canCreateCleanupReport={canCreateCleanupReport} canChangeCleanupStatus={canChangeCleanupStatus}
                cleanupReport={cleanupReport} onChanged={handleIncidentChanged} handleDownload={handleDownload}
                downloadingId={downloadingId} onCreated={(created) => {
                  setCleanupReport(created);
                  setIncident((prev) => prev ? { ...prev, cleanupReportId: created.id } : prev);
                  if (historyLoaded) { loadHistory(); }
                }} />

              <IncidentAssignments key={incident.id} token={token} incident={incident} canAssign={canAssign}
                usersMap={usersMap} usersLoading={usersLoading} setUsersMap={setUsersMap}
                onChanged={handleIncidentChanged} onError={setError} />

              <div className="data-list__row">
                <dt>Вложения</dt>
                <dd>
                  {incident.attachmentFileIds.length === 0 ? (
                    '—'
                  ) : (
                    <AttachmentDownloadList token={token}
                      fileIds={incident.attachmentFileIds}
                      downloadingId={downloadingId}
                      onDownload={handleDownload}
                    />
                  )}
                </dd>
              </div>
              <div className="data-list__row">
                <dt>Создан</dt>
                <dd>{formatDate(incident.createdAt)}</dd>
              </div>
              <div className="data-list__row">
                <dt>Обновлён</dt>
                <dd>{formatDate(incident.updatedAt)}</dd>
              </div>
            </dl>
          </section>

          <section className="section">
          <div className="tabs">
            <div className="tabs__bar" role="tablist" aria-label="Разделы инцидента">
              <button
                type="button"
                role="tab"
                id="incident-tab-comments"
                aria-selected={activeTab === 'comments'}
                aria-controls="incident-tab-panel-comments"
                className={activeTab === 'comments' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
                onClick={() => setActiveTab('comments')}
              >
                Комментарии
              </button>
              <button
                type="button"
                role="tab"
                id="incident-tab-history"
                aria-selected={activeTab === 'history'}
                aria-controls="incident-tab-panel-history"
                className={activeTab === 'history' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
                onClick={() => setActiveTab('history')}
              >
                История
              </button>
              {canReadEquipmentMovements && <button type="button" role="tab" id="incident-tab-equipment"
                aria-selected={activeTab === 'equipment'} aria-controls="incident-tab-panel-equipment"
                className={activeTab === 'equipment' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
                onClick={() => setActiveTab('equipment')}>
                Оборудование
              </button>}
              {canReadDetentions && <button type="button" role="tab" id="incident-tab-detention"
                aria-selected={activeTab === 'detention'} aria-controls="incident-tab-panel-detention"
                className={activeTab === 'detention' ? 'tabs__tab tabs__tab--active' : 'tabs__tab'}
                onClick={() => setActiveTab('detention')}>Заключение</button>}
            </div>

            {activeTab === 'comments' && (
              <div
                id="incident-tab-panel-comments"
                className="tabs__panel"
                role="tabpanel"
                aria-labelledby="incident-tab-comments"
              >
                <IncidentComments {...commentState} canComment={canComment} usersMap={usersMap} />
              </div>
            )}

            {activeTab === 'equipment' && canReadEquipmentMovements && (
              <div id="incident-tab-panel-equipment" className="tabs__panel" role="tabpanel"
                aria-labelledby="incident-tab-equipment">
                <IncidentEquipmentSection key={incident.id} token={token} incidentId={incident.id}
                  incidentStatus={incident.status} currentUserId={currentUserId} canRead={canReadEquipmentMovements}
                  canCreate={canCreateEquipmentMovements} />
              </div>
            )}

            {activeTab === 'detention' && canReadDetentions && (
              <div id="incident-tab-panel-detention" className="tabs__panel" role="tabpanel"
                aria-labelledby="incident-tab-detention">
                <IncidentDetentionSection token={token} incidentId={incident.id} incidentStatus={incident.status}
                  initialAlienId={incident.alienId} canRead={canReadDetentions} canCreate={canCreateDetentions}
                  canReadAliens={canReadAliens} />
              </div>
            )}

            {activeTab === 'history' && (
              <div
                id="incident-tab-panel-history"
                className="tabs__panel"
                role="tabpanel"
                aria-labelledby="incident-tab-history"
              >
                <IncidentHistory token={token} canReadAliens={canReadAliens} historyEntries={historyEntries}
                  usersMap={usersMap} historyLoading={historyLoading} />
              </div>
            )}
          </div>
          </section>
        </>
      )}

      <AlienPickerDrawer
        token={token}
        open={
          alienDrawerOpen
          && canReadAliens
          && canLinkAlien
          && incident?.status === 'READY_FOR_ANALYSIS'
        }
        selecting={linkLoading}
        onClose={() => setAlienDrawerOpen(false)}
        onSelect={handleLinkAlien}
      />

      {!loading && !incident && !error && (
        <>
          <p className="text-muted incident-not-found">Инцидент не найден.</p>
          <button type="button" className="btn btn--outline" onClick={() => navigate('/incidents')}>
            Вернуться к списку
          </button>
        </>
      )}
    </Card>
  );
}
