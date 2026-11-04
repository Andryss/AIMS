import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { incidentTransitions } from '../domain/incidentTransitions';
import { useMutation } from '../hooks/useMutation';
import { STATUS_LABELS } from '../incidentLabels';
import { IncidentResponse, IncidentStatus } from '../types';
import { ConfirmReasonDialog } from './ui/ConfirmReasonDialog';
import { StatusSelect } from './ui/StatusSelect';

interface IncidentStatusSelectProps {
  token: string;
  incident: IncidentResponse;
  roles: string[];
  currentUserId?: number;
  canChange: boolean;
  showErrorText?: boolean;
  onStatusChanged: (updated: IncidentResponse) => void;
  className?: string;
}

export function IncidentStatusSelect({ token, incident, roles, currentUserId, canChange, showErrorText = false,
  onStatusChanged, className }: IncidentStatusSelectProps) {
  const [pendingStatus, setPendingStatus] = useState<IncidentStatus | null>(null);
  const [commentText, setCommentText] = useState('');
  const scopeKey = JSON.stringify([token, incident.id, incident.status]);
  const mutation = useMutation(scopeKey);
  const { busy: loading, error, setError } = mutation;
  const allowedTargets = incidentTransitions(incident.status, roles, incident, currentUserId);
  useEffect(() => { setPendingStatus(null); setCommentText(''); }, [scopeKey]);
  const close = () => {
    if (!mutation.isBusy()) { setPendingStatus(null); setCommentText(''); }
  };
  const applyStatusChange = async (nextStatus: IncidentStatus, comment?: string) => {
    if (!canChange || !allowedTargets.includes(nextStatus)) { return; }
    const session = mutation.start();
    if (!session) { return; }
    try {
      const updated = await api.changeIncidentStatus(token, incident.id, nextStatus, comment);
      if (!session.isCurrent()) { return; }
      onStatusChanged(updated);
      setPendingStatus(null);
      setCommentText('');
    } catch (err: unknown) {
      session.fail(err, 'Не удалось изменить статус');
    } finally {
      session.finish();
    }
  };

  return <div className={`status-select-wrap${className ? ` ${className}` : ''}`} onClick={(event) => event.stopPropagation()}>
    <StatusSelect value={incident.status} options={allowedTargets} labels={STATUS_LABELS}
      label={`Статус инцидента #${incident.id}`} disabled={!canChange || !allowedTargets.length} busy={loading}
      onChange={(nextStatus) => {
        if (nextStatus === 'CLARIFICATION_REQUIRED' || nextStatus === 'REANALYSIS_REQUIRED') {
          setPendingStatus(nextStatus); setCommentText(''); setError(null);
        } else { void applyStatusChange(nextStatus); }
      }} />
    {loading && <span className="status-select__loading">…</span>}
    {error && !pendingStatus && (showErrorText
      ? <div className="alert alert--error" role="alert">{error}</div>
      : <span className="status-select__error" title={error}>!</span>)}
    <ConfirmReasonDialog open={pendingStatus != null} title={pendingStatus ? `Смена статуса на «${STATUS_LABELS[pendingStatus]}»` : ''}
      label="Комментарий (обязательно)" maxLength={null} disableEmpty reason={commentText} onReasonChange={setCommentText} error={error}
      busy={loading} onClose={close} onConfirm={() => {
        if (pendingStatus && commentText.trim()) { void applyStatusChange(pendingStatus, commentText.trim()); }
      }} />
  </div>;
}
