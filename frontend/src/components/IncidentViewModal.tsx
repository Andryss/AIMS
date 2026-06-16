import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { IncidentResponse } from '../types';
import { IncidentStatusSelect } from './IncidentStatusSelect';
import { AttachmentDownloadList } from './AttachmentDownloadList';

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('ru-RU');
  } catch {
    return iso;
  }
}

interface IncidentViewModalProps {
  token: string;
  incidentId: number | null;
  open: boolean;
  roles: string[];
  canChangeStatus: boolean;
  onClose: () => void;
  onStatusChanged?: (updated: IncidentResponse) => void;
}

export function IncidentViewModal({
  token,
  incidentId,
  open,
  roles,
  canChangeStatus,
  onClose,
  onStatusChanged,
}: IncidentViewModalProps) {
  const [incident, setIncident] = useState<IncidentResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<number | null>(null);

  useEffect(() => {
    if (!open || incidentId == null) {
      setIncident(null);
      setError(null);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    api
      .getIncident(token, incidentId)
      .then((data) => {
        if (!cancelled) {
          setIncident(data);
        }
      })
      .catch((err: unknown) => {
        if (!cancelled) {
          const message = err instanceof Error ? err.message : 'Не удалось загрузить инцидент';
          setError(message);
          setIncident(null);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [open, incidentId, token]);

  if (!open) {
    return null;
  }

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

  return (
    <div className="modal-overlay" onClick={onClose} role="presentation">
      <div
        className="modal incident-view-modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-labelledby="incident-view-title"
      >
        <div className="modal-header">
          <h2 id="incident-view-title">
            {incident ? `Инцидент #${incident.id}` : 'Инцидент'}
          </h2>
          <button type="button" className="icon-button" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
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
                      onStatusChanged?.(updated);
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
        )}

        <div className="modal-actions">
          <button type="button" className="secondary" onClick={onClose}>
            Закрыть
          </button>
        </div>
      </div>
    </div>
  );
}
