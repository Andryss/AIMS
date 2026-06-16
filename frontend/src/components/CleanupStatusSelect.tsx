import React, { useMemo, useState } from 'react';
import * as api from '../api/client';
import { CLEANUP_STATUS_LABELS, CLEANUP_STATUS_TRANSITIONS } from '../incidentLabels';
import { CleanupStatus, IncidentResponse } from '../types';

interface CleanupStatusSelectProps {
  token: string;
  incident: IncidentResponse;
  canChange: boolean;
  onStatusChanged: (updated: IncidentResponse) => void;
  className?: string;
}

function allowedCleanupTargets(current: CleanupStatus | null | undefined): CleanupStatus[] {
  const key = current ?? 'null';
  return CLEANUP_STATUS_TRANSITIONS[key];
}

export function CleanupStatusSelect({
  token,
  incident,
  canChange,
  onStatusChanged,
  className,
}: CleanupStatusSelectProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const allowedTargets = useMemo(
    () => allowedCleanupTargets(incident.cleanupStatus),
    [incident.cleanupStatus],
  );

  const displayStatus = incident.cleanupStatus;
  const options: (CleanupStatus | '')[] = displayStatus
    ? [displayStatus, ...allowedTargets]
    : ['', ...allowedTargets];

  const handleChange = async (nextRaw: string) => {
    if (!nextRaw || loading) {
      return;
    }
    const nextStatus = nextRaw as CleanupStatus;
    if (nextStatus === incident.cleanupStatus) {
      return;
    }
    if (!allowedTargets.includes(nextStatus)) {
      return;
    }

    setLoading(true);
    setError(null);
    try {
      const updated = await api.changeCleanupStatus(token, incident.id, nextStatus);
      onStatusChanged(updated);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось изменить статус очистки';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const wrapClassName = className
    ? `status-select-wrap ${className}`
    : 'status-select-wrap';

  const canEdit = canChange && allowedTargets.length > 0;

  if (displayStatus == null && !canEdit) {
    return <span className="text-muted">—</span>;
  }

  const statusClass = displayStatus
    ? `status-chip--cleanup_${displayStatus.toLowerCase()}`
    : '';

  return (
    <div className={wrapClassName} onClick={(e) => e.stopPropagation()}>
      {canEdit ? (
        <select
          className={`status-select ${statusClass}${loading ? ' is-loading' : ''}`}
          value={displayStatus ?? ''}
          disabled={loading}
          onChange={(e) => void handleChange(e.target.value)}
          aria-label={`Статус очистки инцидента #${incident.id}`}
        >
          {displayStatus == null && (
            <option value="" disabled>
              Выберите статус
            </option>
          )}
          {options.filter((s) => s !== '').map((status) => (
            <option key={status} value={status}>
              {CLEANUP_STATUS_LABELS[status as CleanupStatus]}
            </option>
          ))}
        </select>
      ) : (
        <span className={`status-chip ${statusClass}`}>
          {displayStatus != null ? CLEANUP_STATUS_LABELS[displayStatus] : '—'}
        </span>
      )}
      {loading && <span className="status-select__loading">…</span>}
      {error && <span className="status-select__error" title={error}>!</span>}
    </div>
  );
}
