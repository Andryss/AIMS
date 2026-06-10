import React, { useMemo, useState } from 'react';
import * as api from '../api/client';
import { STATUS_LABELS, STATUS_TRANSITIONS } from '../incidentLabels';
import { IncidentResponse, IncidentStatus } from '../types';

interface IncidentStatusSelectProps {
  token: string;
  incident: IncidentResponse;
  roles: string[];
  canChange: boolean;
  onStatusChanged: (updated: IncidentResponse) => void;
  className?: string;
}

function hasRole(roles: string[], name: string): boolean {
  return roles.includes(name) || roles.includes('ADMIN');
}

function allowedTargetsForUser(
  status: IncidentStatus,
  roles: string[],
): IncidentStatus[] {
  const fromGraph = STATUS_TRANSITIONS[status];
  if (status === 'DRAFT') {
    return hasRole(roles, 'OPERATOR') ? fromGraph : [];
  }
  if (status === 'READY_FOR_ANALYSIS') {
    return hasRole(roles, 'ANALYST') ? fromGraph : [];
  }
  return [];
}

export function IncidentStatusSelect({
  token,
  incident,
  roles,
  canChange,
  onStatusChanged,
  className,
}: IncidentStatusSelectProps) {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const allowedTargets = useMemo(
    () => allowedTargetsForUser(incident.status, roles),
    [incident.status, roles],
  );
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

  const canEdit = canChange && allowedTargets.length > 0;

  return (
    <div className={wrapClassName} onClick={(e) => e.stopPropagation()}>
      <select
        className={`status-select status-${incident.status.toLowerCase()}${loading ? ' is-loading' : ''}`}
        value={incident.status}
        disabled={loading || !canEdit}
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
