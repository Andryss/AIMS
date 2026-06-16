import React, { useMemo, useState } from 'react';
import * as api from '../api/client';
import { STATUS_LABELS, STATUS_TRANSITIONS } from '../incidentLabels';
import { IncidentResponse, IncidentStatus } from '../types';

interface IncidentStatusSelectProps {
  token: string;
  incident: IncidentResponse;
  roles: string[];
  currentUserId?: number;
  canChange: boolean;
  onStatusChanged: (updated: IncidentResponse) => void;
  className?: string;
}

function hasRole(roles: string[], name: string): boolean {
  return roles.includes(name) || roles.includes('ADMIN');
}

function isResponsibleAgent(incident: IncidentResponse, currentUserId?: number): boolean {
  return incident.responsibleUserId != null
    && currentUserId != null
    && incident.responsibleUserId === currentUserId;
}

function allowedTargetsForUser(
  status: IncidentStatus,
  roles: string[],
  incident: IncidentResponse,
  currentUserId?: number,
): IncidentStatus[] {
  const fromGraph = STATUS_TRANSITIONS[status];
  if (status === 'DRAFT') {
    return hasRole(roles, 'OPERATOR') ? fromGraph : [];
  }
  if (status === 'READY_FOR_ANALYSIS') {
    return hasRole(roles, 'ANALYST') ? fromGraph : [];
  }
  if (status === 'CLARIFICATION_REQUIRED' || status === 'REANALYSIS_REQUIRED') {
    return hasRole(roles, 'OPERATOR') ? fromGraph : [];
  }
  if (status === 'PREPARED_FOR_EXECUTION' || status === 'EXECUTING') {
    if (!hasRole(roles, 'AGENT')) {
      return [];
    }
    return isResponsibleAgent(incident, currentUserId) ? fromGraph : [];
  }
  if (status === 'READY_FOR_EXECUTION' || status === 'PREPARATION_FOR_EXECUTION') {
    return hasRole(roles, 'AGENT') ? fromGraph : [];
  }
  return [];
}

export function IncidentStatusSelect({
  token,
  incident,
  roles,
  currentUserId,
  canChange,
  onStatusChanged,
  className,
}: IncidentStatusSelectProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [modalOpen, setModalOpen] = useState(false);
  const [pendingStatus, setPendingStatus] = useState<IncidentStatus | null>(null);
  const [commentText, setCommentText] = useState('');

  const allowedTargets = useMemo(
    () => allowedTargetsForUser(incident.status, roles, incident, currentUserId),
    [incident, roles, currentUserId],
  );
  const options: IncidentStatus[] = [incident.status, ...allowedTargets];

  const commentRequired =
    pendingStatus === 'CLARIFICATION_REQUIRED' || pendingStatus === 'REANALYSIS_REQUIRED';
  const canConfirm = !commentRequired || commentText.trim().length > 0;

  const applyStatusChange = async (nextStatus: IncidentStatus, comment?: string) => {
    setLoading(true);
    setError(null);

    try {
      const updated = await api.changeIncidentStatus(
        token,
        incident.id,
        nextStatus,
        comment,
      );
      onStatusChanged(updated);
      setModalOpen(false);
      setPendingStatus(null);
      setCommentText('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось изменить статус';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleStatusChange = (nextStatus: IncidentStatus) => {
    if (nextStatus === incident.status || loading || !allowedTargets.includes(nextStatus)) {
      return;
    }
    if (nextStatus === 'CLARIFICATION_REQUIRED' || nextStatus === 'REANALYSIS_REQUIRED') {
      setPendingStatus(nextStatus);
      setCommentText('');
      setError(null);
      setModalOpen(true);
      return;
    }
    void applyStatusChange(nextStatus);
  };

  const closeModal = () => {
    if (loading) {
      return;
    }
    setModalOpen(false);
    setPendingStatus(null);
    setCommentText('');
  };

  const confirmStatusChange = () => {
    if (!pendingStatus || !canConfirm || loading) {
      return;
    }
    void applyStatusChange(pendingStatus, commentText.trim());
  };

  const wrapClassName = className
    ? `status-select-wrap ${className}`
    : 'status-select-wrap';

  const canEdit = canChange && allowedTargets.length > 0;

  return (
    <div className={wrapClassName} onClick={(e) => e.stopPropagation()}>
      <select
        className={`status-select status-${incident.status.toLowerCase()}${loading ? ' is-loading' : ''}`}
        value={incident.status}
        disabled={loading || !canEdit}
        onChange={(e) => handleStatusChange(e.target.value as IncidentStatus)}
        aria-label={`Статус инцидента #${incident.id}`}
      >
        {options.map((status) => (
          <option key={status} value={status}>
            {STATUS_LABELS[status]}
          </option>
        ))}
      </select>
      {loading && <span className="status-select-loading">…</span>}
      {error && !modalOpen && (
        <span className="status-select-error" title={error}>!</span>
      )}

      {modalOpen && pendingStatus && (
        <div className="status-change-modal-backdrop" role="presentation" onClick={closeModal}>
          <div
            className="status-change-modal"
            role="dialog"
            aria-labelledby="status-change-title"
            aria-modal="true"
            onClick={(e) => e.stopPropagation()}
          >
            <h3 id="status-change-title">
              Смена статуса на «{STATUS_LABELS[pendingStatus]}»
            </h3>
            <label className="status-change-comment-label" htmlFor="status-change-comment">
              Комментарий (обязательно)
            </label>
            <textarea
              id="status-change-comment"
              className="status-change-comment-input"
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              rows={4}
              disabled={loading}
            />
            {error && <div className="alert alert-error">{error}</div>}
            <div className="status-change-modal-actions">
              <button type="button" className="secondary" onClick={closeModal} disabled={loading}>
                Отмена
              </button>
              <button
                type="button"
                onClick={confirmStatusChange}
                disabled={loading || !canConfirm}
              >
                {loading ? 'Сохранение…' : 'Подтвердить'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
