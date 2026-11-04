import { positiveId } from '../utils/params';
import { Card, CardHeader } from './ui/Card';
import React, { useCallback, useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { ANALYTICS_DASHBOARDS, AnalyticsDashboard } from '../analyticsLabels';
import { DETENTION_STATUS_LABELS } from '../detentionLabels';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { AnalyticsPeriodQuery, DetentionRequestStatus, EquipmentType, IncidentEventType } from '../types';
import { defaultAnalyticsPeriod, validateAnalyticsQuery } from '../utils/analyticsFormatting';
import { AnalyticsFilters } from './AnalyticsFilters';
import { AnalyticsPeriodFilter } from './AnalyticsPeriodFilter';
import { DetentionAnalyticsDashboard } from './DetentionAnalyticsDashboard';
import { EquipmentAnalyticsDashboard } from './EquipmentAnalyticsDashboard';
import { IncidentAnalyticsDashboard } from './IncidentAnalyticsDashboard';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';

interface AnalyticsTabProps { token: string; permissions: string[]; }

export function AnalyticsTab(props: AnalyticsTabProps) {
  return props.permissions.includes('ANALYTICS_READ') ? <AnalyticsContent {...props} /> : <EmptyState title="Нет доступа к аналитике" />;
}

function AnalyticsContent({ token, permissions }: AnalyticsTabProps) {
  const [params, setParams] = useSearchParams();
  const [defaults] = useState(defaultAnalyticsPeriod);
  const [validDraft, setValidDraft] = useState(true);
  const rawDashboard = params.get('dashboard') ?? '';
  const dashboard: AnalyticsDashboard = Object.prototype.hasOwnProperty.call(ANALYTICS_DASHBOARDS, rawDashboard)
    ? rawDashboard as AnalyticsDashboard : 'incidents';
  const from = params.get('from') ?? defaults.from;
  const to = params.get('to') ?? defaults.to;
  const query = { from, to };
  const error = validateAnalyticsQuery(from, to);
  const scoped = (key: string) => params.get(`${dashboard}.${key}`) ?? '';
  const enumValue = <T extends string>(labels: Record<T, string>, key: string): T | undefined => {
    const value = scoped(key);
    return Object.prototype.hasOwnProperty.call(labels, value) ? value as T : undefined;
  };

  const idFields = dashboard === 'incidents' ? ['alienId', 'responsibleUserId']
    : dashboard === 'equipment' ? ['responsibleUserId', 'incidentId'] : ['alienId', 'incidentId'];
  const invalidId = idFields.find((field) => scoped(field) !== '' && positiveId(scoped(field)) == null);
  const filterError = invalidId ? 'Укажите целый положительный номер в фильтре' : null;

  useEffect(() => {
    if (params.has('from') && params.has('to')) { return; }
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      if (!next.has('from')) { next.set('from', defaults.from); }
      if (!next.has('to')) { next.set('to', defaults.to); }
      return next;
    }, { replace: true });
  }, [params, setParams, defaults]);

  const changePeriod = useCallback((period: AnalyticsPeriodQuery) => {
    setParams((previous) => {
      const next = new URLSearchParams(previous);
      next.set('from', period.from); next.set('to', period.to);
      return next;
    }, { replace: true });
  }, [setParams]);

  const changeDashboard = (value: AnalyticsDashboard) => setParams((previous) => {
    const next = new URLSearchParams(previous); next.set('dashboard', value); return next;
  });

  return <Card className="analytics">
    <CardHeader><h2>Аналитика</h2></CardHeader>
    <AnalyticsPeriodFilter from={from} to={to} onChange={changePeriod} onValidityChange={setValidDraft} periodError={error} />
    <div className="tabs__bar analytics-tabs" role="tablist" aria-label="Раздел аналитики">
      {(Object.entries(ANALYTICS_DASHBOARDS) as Array<[AnalyticsDashboard, string]>).map(([value, label], index, entries) =>
        <button key={value} id={`analytics-tab-${value}`} type="button" role="tab" aria-selected={value === dashboard}
          aria-controls="analytics-panel" tabIndex={value === dashboard ? 0 : -1}
          className={`tabs__tab${value === dashboard ? ' tabs__tab--active' : ''}`} onClick={() => changeDashboard(value)}
          onKeyDown={(event) => {
            const next = event.key === 'ArrowRight' ? (index + 1) % entries.length
              : event.key === 'ArrowLeft' ? (index + entries.length - 1) % entries.length
              : event.key === 'Home' ? 0 : event.key === 'End' ? entries.length - 1 : null;
            if (next != null) { event.preventDefault(); changeDashboard(entries[next][0]);
              document.getElementById(`analytics-tab-${entries[next][0]}`)?.focus(); }
          }}>{label}</button>)}
    </div>
    <AnalyticsFilters token={token} dashboard={dashboard} params={params} permissions={permissions}
      onChange={(field, value) => setParams((previous) => {
        const next = new URLSearchParams(previous); const key = `${dashboard}.${field}`;
        if (value) { next.set(key, value); } else { next.delete(key); }
        return next;
      }, { replace: true })} />
    {filterError && <div className="alert alert--error" role="alert">{filterError}
      <Button variant="ghost" size="sm" onClick={() => setParams((previous) => {
        const next = new URLSearchParams(previous);
        Array.from(next.keys()).filter((key) => key.startsWith(`${dashboard}.`)).forEach((key) => next.delete(key));
        return next;
      }, { replace: true })}>Сбросить фильтры</Button>
    </div>}
    <div id="analytics-panel" role="tabpanel" aria-labelledby={`analytics-tab-${dashboard}`}>
      {validDraft && !error && !filterError && <>
        {dashboard === 'incidents' && <IncidentAnalyticsDashboard token={token} permissions={permissions} query={{ ...query,
          eventType: enumValue<IncidentEventType>(EVENT_TYPE_LABELS, 'eventType'), alienId: positiveId(scoped('alienId')),
          responsibleUserId: positiveId(scoped('responsibleUserId')) }} />}
        {dashboard === 'equipment' && <EquipmentAnalyticsDashboard token={token} permissions={permissions} query={{ ...query,
          equipmentType: enumValue<EquipmentType>(EQUIPMENT_TYPE_LABELS, 'equipmentType'), incidentId: positiveId(scoped('incidentId')),
          responsibleUserId: positiveId(scoped('responsibleUserId')) }} />}
        {dashboard === 'detentions' && <DetentionAnalyticsDashboard token={token} permissions={permissions} query={{ ...query,
          status: enumValue<DetentionRequestStatus>(DETENTION_STATUS_LABELS, 'status'), alienId: positiveId(scoped('alienId')),
          incidentId: positiveId(scoped('incidentId')) }} />}
      </>}
    </div>
  </Card>;
}
