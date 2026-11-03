import React from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS, STATUS_LABELS } from '../incidentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { IncidentAnalyticsQuery, StaleIncidentAnalyticsRow } from '../types';
import { formatAnalyticsDate } from '../utils/analyticsFormatting';
import { formatUserLabel } from '../utils/incidentUsers';
import { AnalyticsGroups } from './AnalyticsGroups';
import { AnalyticsSummary } from './AnalyticsSummary';
import { AnalyticsTrendChart } from './AnalyticsTrendChart';
import { AnalyticsWidget } from './AnalyticsWidget';
import { DurationPercentiles } from './DurationPercentiles';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';
import { StatusChip } from './ui/StatusChip';

interface IncidentAnalyticsDashboardProps { token: string; query: IncidentAnalyticsQuery; permissions: string[]; }

function StaleRows({ token, items, permissions }: { token: string; items: StaleIncidentAnalyticsRow[]; permissions: string[] }) {
  const users = useUsersMap(token, permissions.includes('USER_READ') ? items.map((item) => item.responsibleUserId) : []);
  if (!items.length) { return <EmptyState title="Нет инцидентов без изменений более 72 часов" />; }
  return <div className="incidents-table-wrapper"><table className="incidents-table">
    <thead><tr><th>Инцидент</th><th>Место</th><th>Статус</th><th>Ответственный</th><th>Последнее изменение</th></tr></thead>
    <tbody>{items.map((item) => <tr key={item.incidentId}>
      <td>{permissions.includes('INCIDENT_READ') ? <Link to={`/incidents/${item.incidentId}`} className="knowledge-base__name">
        Инцидент #{item.incidentId}</Link> : `Инцидент #${item.incidentId}`}</td>
      <td>{item.location}</td><td><StatusChip status={item.status.toLowerCase()} label={STATUS_LABELS[item.status]} /></td>
      <td>{item.responsibleUserId ? <UserChip login={formatUserLabel(item.responsibleUserId, users)} size={24} /> : '—'}</td>
      <td>{formatAnalyticsDate(item.lastChangedAt)}</td>
    </tr>)}</tbody>
  </table></div>;
}

export function IncidentAnalyticsDashboard({ token, query, permissions }: IncidentAnalyticsDashboardProps) {
  const props = { token, query };
  return <div className="analytics-dashboard">
    <h3 className="analytics-section-title">События за период</h3>
    <div className="analytics-summary-grid">
      <AnalyticsSummary {...props} title="Зарегистрировано" load={api.getIncidentCreatedCount} />
      <AnalyticsSummary {...props} title="Завершено за период" load={api.getIncidentCompletedCount} />
    </div>
    <div className="analytics-grid">
      <AnalyticsWidget {...props} title="Динамика инцидентов" load={api.getIncidentTrend}>{(data) =>
        <AnalyticsTrendChart label="Динамика инцидентов" buckets={data.buckets} bucketUnit={data.meta.bucketUnit}
          series={[{ key: 'created', label: 'Зарегистрировано' }, { key: 'completed', label: 'Завершено' }]} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="Время обработки инцидента" load={api.getIncidentCompletionDuration}>{(data) =>
        <DurationPercentiles value={data.percentiles} />
      }</AnalyticsWidget>
    </div>
    <h3 className="analytics-section-title">Состояние на {formatAnalyticsDate(query.to)}</h3>
    <div className="analytics-summary-grid">
      <AnalyticsSummary {...props} title="Всего инцидентов" load={api.getIncidentTotalAtTo} />
      <AnalyticsSummary {...props} title="Завершено на дату" load={api.getIncidentCompletedAtTo} />
      <AnalyticsSummary {...props} title="В работе" load={api.getIncidentOpenAtTo} />
    </div>
    <div className="analytics-grid">
      <AnalyticsWidget {...props} title="По статусам" load={api.getIncidentByStatus}>{(data) =>
        <AnalyticsGroups groups={data.groups} labels={STATUS_LABELS} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="По типам событий" load={api.getIncidentByEventType}>{(data) =>
        <AnalyticsGroups groups={data.groups} labels={EVENT_TYPE_LABELS} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="По видам" load={api.getIncidentByAlien}>{(data) =>
        <AnalyticsGroups groups={data.groups} canReadAliens={permissions.includes('ALIEN_READ')} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="По местам" load={api.getIncidentByLocation}>{(data) =>
        <AnalyticsGroups groups={data.groups} />
      }</AnalyticsWidget>
    </div>
    <AnalyticsWidget {...props} title="Без изменений более 72 часов" load={api.getIncidentStale}>{(data) => <>
      <p className="analytics-note">{data.totalCount > 10 ? `Показаны первые 10 из ${data.totalCount}` : `Всего: ${data.totalCount}`}</p>
      <StaleRows token={token} items={data.items} permissions={permissions} />
    </>}</AnalyticsWidget>
    <p className="analytics-note">Переход к карточке показывает её текущее состояние.</p>
  </div>;
}
