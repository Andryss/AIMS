import React from 'react';
import * as api from '../api/client';
import { cleanupTransitions } from '../domain/incidentTransitions';
import { useMutation } from '../hooks/useMutation';
import { CLEANUP_STATUS_LABELS } from '../incidentLabels';
import { CleanupStatus, IncidentResponse } from '../types';
import { StatusSelect } from './ui/StatusSelect';

interface CleanupStatusSelectProps {
  token: string;
  incident: IncidentResponse;
  canChange: boolean;
  onStatusChanged: (updated: IncidentResponse) => void;
  className?: string;
}

export function CleanupStatusSelect({ token, incident, canChange, onStatusChanged, className }: CleanupStatusSelectProps) {
  const mutation = useMutation(JSON.stringify([token, incident.id, incident.cleanupStatus]));
  const { busy: loading, error } = mutation;
  const allowedTargets = cleanupTransitions(incident.cleanupStatus);
  const displayStatus = incident.cleanupStatus;
  const canEdit = canChange && allowedTargets.length > 0;
  const handleChange = async (nextStatus: CleanupStatus | '') => {
    if (!canChange || !nextStatus || !allowedTargets.includes(nextStatus)) { return; }
    const session = mutation.start();
    if (!session) { return; }
    try {
      const updated = await api.changeCleanupStatus(token, incident.id, nextStatus);
      if (session.isCurrent()) { onStatusChanged(updated); }
    } catch (err: unknown) {
      session.fail(err, 'Не удалось изменить статус очистки');
    } finally {
      session.finish();
    }
  };
  if (displayStatus == null && !canEdit) { return <span className="text-muted">—</span>; }
  const statusClass = displayStatus ? `status-chip--cleanup_${displayStatus.toLowerCase()}` : '';
  return <div className={`status-select-wrap${className ? ` ${className}` : ''}`} onClick={(event) => event.stopPropagation()}>
    {canEdit ? <StatusSelect<CleanupStatus | ''> label={`Статус очистки инцидента #${incident.id}`}
      value={displayStatus ?? ''} options={allowedTargets} labels={{ ...CLEANUP_STATUS_LABELS, '': '' }}
      busy={loading} statusClassName={statusClass} placeholder="Выберите статус" onChange={(next) => { void handleChange(next); }} />
      : <span className={`status-chip ${statusClass}`}>{displayStatus != null ? CLEANUP_STATUS_LABELS[displayStatus] : '—'}</span>}
    {loading && <span className="status-select__loading">…</span>}
    {error && <span className="status-select__error" title={error}>!</span>}
  </div>;
}
