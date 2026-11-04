import React, { useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { IncidentResponse, IncidentThreatCalculationResponse } from '../types';
import { Button } from './ui/Button';
import { SparkleIcon } from './ui/SparkleIcon';
import { ThreatCalculationDetails } from './ThreatCalculationDetails';

const MIN_THREAT_LEVEL = 1;
const MAX_THREAT_LEVEL = 10;

interface Props {
  token: string;
  incident: IncidentResponse;
  canCalculate: boolean;
  alienThreatLevel?: number;
  disabled?: boolean;
  onUpdated: (incident: IncidentResponse) => void;
}

export function IncidentThreatAssessment({ token, incident, canCalculate, alienThreatLevel,
  disabled = false, onUpdated }: Props) {
  const [proposal, setProposal] = useState<{
    scope: string; calculation: IncidentThreatCalculationResponse | null; level: number;
  } | null>(null);
  const [calculating, setCalculating] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const generation = useRef(0);
  const scope = JSON.stringify([token, canCalculate, incident.id, incident.status, incident.eventType,
    incident.victimCount, incident.alienId, incident.threatLevel, incident.updatedAt, alienThreatLevel]);
  const currentScope = useRef(scope);
  currentScope.current = scope;
  const active = canCalculate && incident.status !== 'EXECUTION_COMPLETED';
  const draft = active && proposal?.scope === scope ? proposal : null;
  const busy = calculating || saving;
  const isCurrent = (request: number) => generation.current === request && currentScope.current === scope;

  useEffect(() => {
    generation.current += 1;
    setProposal(null);
    setCalculating(false);
    setSaving(false);
    setError(null);
    return () => { generation.current += 1; };
  }, [scope]);

  const calculate = async () => {
    if (!active || busy || disabled) { return; }
    const current = ++generation.current;
    setProposal(null);
    setError(null);
    setCalculating(true);
    try {
      const response = await api.calculateIncidentThreat(token, incident.id);
      if (isCurrent(current)) {
        setProposal({ scope, calculation: response, level: response.calculatedThreatLevel });
      }
    } catch (err) {
      if (isCurrent(current)) { setError(err instanceof Error ? err.message : 'Не удалось рассчитать угрозу'); }
    } finally {
      if (isCurrent(current)) { setCalculating(false); }
    }
  };

  const accept = async () => {
    if (!draft || busy || disabled) { return; }
    const current = generation.current;
    setSaving(true);
    setError(null);
    try {
      const updated = await api.setIncidentThreatLevel(token, incident.id, draft.level);
      if (isCurrent(current)) {
        setProposal(null);
        onUpdated(updated);
      }
    } catch (err) {
      if (isCurrent(current)) {
        setError(err instanceof Error ? err.message : 'Не удалось сохранить угрозу');
        if (err instanceof api.ApiError && err.status === 409) {
          setProposal(null);
          try {
            const updated = await api.getIncident(token, incident.id);
            if (isCurrent(current)) { onUpdated(updated); }
          } catch { /* Исходная ошибка остаётся видимой, если карточку не удалось обновить. */ }
        }
      }
    } finally {
      if (isCurrent(current)) { setSaving(false); }
    }
  };

  const adjust = (delta: number) => {
    if (busy || disabled) { return; }
    setProposal((previous) => previous && previous.scope === scope ? { ...previous,
      level: Math.max(MIN_THREAT_LEVEL, Math.min(MAX_THREAT_LEVEL, previous.level + delta)),
    } : previous);
  };

  const edit = () => {
    if (!active || busy || disabled) { return; }
    generation.current += 1;
    setError(null);
    setProposal({ scope, calculation: null, level: incident.threatLevel ?? MIN_THREAT_LEVEL });
  };

  if (!active) { return <span>{incident.threatLevel ?? '—'}</span>; }
  const manual = draft && !draft.calculation;
  const editor = draft && <span className="incident-threat__result">
    <Button size="sm" variant="outline" aria-label="Уменьшить угрозу" className="incident-threat__step"
      disabled={busy || disabled || draft.level <= MIN_THREAT_LEVEL} onClick={() => adjust(-1)}>−</Button>
    <output className="incident-threat__level" aria-label="Уровень угрозы" aria-live="polite">{draft.level}</output>
    {draft.calculation && <ThreatCalculationDetails calculation={draft.calculation} eventType={incident.eventType}
      disabled={busy || disabled} />}
    <Button size="sm" variant="outline" aria-label="Увеличить угрозу" className="incident-threat__step"
      disabled={busy || disabled || draft.level >= MAX_THREAT_LEVEL} onClick={() => adjust(1)}>+</Button>
    <Button size="sm" variant="outline" loading={saving} disabled={disabled} onClick={accept}>Принять</Button>
    <Button size="sm" variant="ghost" disabled={saving || disabled}
      onClick={() => { setProposal(null); setError(null); }}>Отклонить</Button>
  </span>;

  return <div className="incident-threat">
    <div className="incident-threat__actions">
      {manual ? editor : <>
        <span>{incident.threatLevel ?? '—'}</span>
        <Button size="sm" variant="outline" aria-label="Изменить угрозу" disabled={busy || disabled} onClick={edit}>Изменить</Button>
      </>}
      <button type="button" className="btn btn--outline icon-button incident-threat__calculate"
        aria-label="Рассчитать угрозу" title="Рассчитать угрозу" disabled={busy || disabled}
        aria-busy={calculating || undefined} onClick={calculate}>
        {calculating ? <span className="spinner spinner--sm" aria-hidden="true" /> : <SparkleIcon />}
      </button>
      {!manual && editor}
    </div>
    {error && <span className="incident-classification__error" role="alert">{error}</span>}
  </div>;
}
