import React, { useState } from 'react';
import * as api from '../api/client';
import { STATUS_LABELS, STATUS_TRANSITIONS } from '../incidentLabels';
import { IncidentResponse, IncidentStatus } from '../types';

interface IncidentStatusSelectProps {
  token: string;
  incident: IncidentResponse;
  canChange: boolean;
  onStatusChanged: (updated: IncidentResponse) => void;
  className?: string;
}

export function IncidentStatusSelect({
  token,
  incident,
  canChange,
  onStatusChanged,
  className,
}: IncidentStatusSelectProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const allowedTargets = STATUS_TRANSITIONS[incident.status];
  const options: IncidentStatus[] = [incident.status, ...allowedTargets];

  const handleChange = async (nextStatus: IncidentStatus) => {
    if (nextStatus === incident.status || loading || !allowedTargets.includes(nextStatus)) {
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const updated = await api.changeIncidentStatus(token, incident.id, nextStatus);
      onStatusChanged(updated);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось изменить статус';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const wrapClassName = className
    ? `status-select-wrap ${className}`
    : 'status-select-wrap';

  return (
    <div className={wrapClassName} onClick={(e) => e.stopPropagation()}>
      <select
        className={`status-select status-${incident.status.toLowerCase()}${loading ? ' is-loading' : ''}`}
        value={incident.status}
        disabled={loading || !canChange}
        onChange={(e) => handleChange(e.target.value as IncidentStatus)}
        aria-label={`Статус инцидента #${incident.id}`}
      >
        {options.map((status) => (
          <option key={status} value={status}>
            {STATUS_LABELS[status]}
          </option>
        ))}
      </select>
      {loading && <span className="status-select-loading">…</span>}
      {error && <span className="status-select-error" title={error}>!</span>}
    </div>
  );
}
