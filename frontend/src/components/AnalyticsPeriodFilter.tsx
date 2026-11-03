import React, { useEffect, useState } from 'react';
import { AnalyticsPeriodQuery } from '../types';
import { isAnalyticsTimestamp, normalizeLocalDateTimeInput, toLocalDateTimeInput, validateAnalyticsQuery } from '../utils/analyticsFormatting';
import { FormField } from './ui/FormField';

interface AnalyticsPeriodFilterProps extends AnalyticsPeriodQuery {
  periodError?: string | null;
  onChange: (period: AnalyticsPeriodQuery) => void;
  onValidityChange: (valid: boolean) => void;
}

export function AnalyticsPeriodFilter({ from, to, onChange, onValidityChange, periodError }: AnalyticsPeriodFilterProps) {
  const [start, setStart] = useState(() => toLocalDateTimeInput(from));
  const [end, setEnd] = useState(() => toLocalDateTimeInput(to));
  const startChanged = normalizeLocalDateTimeInput(start) !== toLocalDateTimeInput(from);
  const endChanged = normalizeLocalDateTimeInput(end) !== toLocalDateTimeInput(to);
  const dirty = startChanged || endChanged;
  const candidate = (input: string, source: string, changed: boolean) => {
    if (!changed && isAnalyticsTimestamp(source)) { return { value: source, error: null }; }
    const millis = Date.parse(input);
    if (!Number.isFinite(millis)) { return { value: '', error: 'Выберите дату и время «От» и «До»' }; }
    const iso = new Date(millis).toISOString();
    if (toLocalDateTimeInput(iso) !== normalizeLocalDateTimeInput(input)) {
      return { value: '', error: 'Выбранное время не существует в часовом поясе браузера' };
    }
    return { value: iso, error: null };
  };
  const candidateFrom = candidate(start, from, startChanged);
  const candidateTo = candidate(end, to, endChanged);
  const error = dirty ? candidateFrom.error ?? candidateTo.error ?? validateAnalyticsQuery(candidateFrom.value, candidateTo.value)
    : periodError ?? validateAnalyticsQuery(from, to);
  const pending = dirty && !error;

  useEffect(() => { setStart(toLocalDateTimeInput(from)); setEnd(toLocalDateTimeInput(to)); }, [from, to]);

  useEffect(() => {
    if (error) { onValidityChange(false); return; }
    if (!pending) { onValidityChange(true); return; }
    const timer = window.setTimeout(() => {
      onChange({ from: candidateFrom.value, to: candidateTo.value });
      onValidityChange(true);
    }, 300);
    return () => window.clearTimeout(timer);
  }, [candidateFrom.value, candidateTo.value, error, pending, onChange, onValidityChange]);

  return <>
    <div className="analytics-period">
      <FormField label="От"><input type="datetime-local" step="0.001" value={start}
        max={toLocalDateTimeInput(new Date().toISOString())} onChange={(event) => {
          onValidityChange(false); setStart(event.target.value);
        }} /></FormField>
      <FormField label="До"><input type="datetime-local" step="0.001" value={end}
        max={toLocalDateTimeInput(new Date().toISOString())} onChange={(event) => {
          onValidityChange(false); setEnd(event.target.value);
        }} /></FormField>
    </div>
    <p className="analytics-note">Часовой пояс: {Intl.DateTimeFormat().resolvedOptions().timeZone}. Начало включается в период, конец — нет.</p>
    {error && <p className="alert alert--error" role="alert">{error}</p>}
    {pending && <p className="analytics-note" role="status">Обновление периода…</p>}
  </>;
}
