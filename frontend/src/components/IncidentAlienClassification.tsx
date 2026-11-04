import React, { useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { Alien, IncidentAlienClassificationResponse, IncidentResponse } from '../types';
import { AlienSummary } from './AlienSummary';
import { Button } from './ui/Button';

interface Props {
  token: string;
  incident: IncidentResponse;
  canAccept: boolean;
  disabled?: boolean;
  onAccepted: (incident: IncidentResponse, alien: Alien) => void;
  onSavingChange?: (saving: boolean) => void;
}

const confidenceFormat = new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 1 });

export function IncidentAlienClassification({ token, incident, canAccept, disabled = false,
  onAccepted, onSavingChange }: Props) {
  const [proposal, setProposal] = useState<{
    incidentId: number; token: string; result: IncidentAlienClassificationResponse;
  } | null>(null);
  const [recognizing, setRecognizing] = useState(false);
  const [accepting, setAccepting] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const ready = incident.status === 'READY_FOR_ANALYSIS';
  const result = ready && proposal?.incidentId === incident.id && proposal.token === token ? proposal.result : null;

  useEffect(() => {
    generation.current += 1;
    setProposal(null);
    setError(null);
    setRecognizing(false);
    setAccepting(false);
    return () => { generation.current += 1; onSavingChange?.(false); };
  }, [incident.id, incident.status, token, canAccept, onSavingChange]);

  const recognize = async () => {
    const current = ++generation.current;
    setProposal(null);
    setError(null);
    setRecognizing(true);
    try {
      const response = await api.classifyIncidentAlien(token, incident.id);
      if (current === generation.current) { setProposal({ incidentId: incident.id, token, result: response }); }
    } catch (err) {
      if (current === generation.current) { setError(err instanceof Error ? err.message : 'Не удалось распознать вид'); }
    } finally {
      if (current === generation.current) { setRecognizing(false); }
    }
  };

  const accept = async () => {
    if (!result?.alien || !canAccept || !ready || accepting || disabled) { return; }
    const current = generation.current;
    const alien = result.alien;
    setAccepting(true);
    onSavingChange?.(true);
    setError(null);
    try {
      const updated = await api.putIncidentAlien(token, incident.id, alien.id);
      if (current === generation.current) {
        setProposal(null);
        onAccepted(updated, alien);
      }
    } catch (err) {
      if (current === generation.current) { setError(err instanceof Error ? err.message : 'Не удалось принять вид'); }
    } finally {
      if (current === generation.current) { setAccepting(false); onSavingChange?.(false); }
    }
  };

  if (!ready) { return null; }
  const noAttachments = incident.attachmentFileIds.length === 0;
  return <span className="incident-classification">
    <button type="button" className="btn btn--outline icon-button incident-classification__trigger"
      aria-label="Распознать вид" title={noAttachments ? 'Добавьте изображения PNG/JPEG в инцидент' : 'Распознать вид'}
      disabled={recognizing || accepting || disabled || noAttachments} aria-busy={recognizing || undefined}
      onClick={recognize}>
      {recognizing ? <span className="spinner spinner--sm" aria-hidden="true" />
        : <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" aria-hidden="true">
          <path d="M12 2C13.4 8 16 10.6 22 12C16 13.4 13.4 16 12 22C10.6 16 8 13.4 2 12C8 10.6 10.6 8 12 2Z" />
        </svg>}
    </button>
    {recognizing && <span className="text-muted" role="status">Распознаём…</span>}
    {noAttachments && <span className="text-muted">Нет изображений для распознавания</span>}
    {result && <span className="incident-classification__result" role="status" aria-live="polite">
      {result.status === 'MATCH' && result.alien
        ? <AlienSummary token={token} alien={result.alien} incidentId={incident.id} />
        : <span>Не удалось подобрать вид</span>}
      <span className="incident-classification__confidence" title="Уверенность модели">
        {confidenceFormat.format(result.confidence)}%
      </span>
      {result.status === 'MATCH' && result.alien && canAccept && <Button size="sm" variant="outline"
        loading={accepting} disabled={disabled} onClick={accept}>Принять</Button>}
      <Button size="sm" variant="ghost" disabled={accepting} onClick={() => { setProposal(null); setError(null); }}>
        Отклонить
      </Button>
    </span>}
    {error && <span className="incident-classification__error" role="alert">{error}</span>}
  </span>;
}
