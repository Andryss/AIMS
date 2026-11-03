import React from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { ASSIGNED_TERM_LABELS, REMAINING_TIME_LABELS } from '../analyticsLabels';
import { DETENTION_STATUS_LABELS } from '../detentionLabels';
import { DetentionAnalyticsQuery } from '../types';
import { formatAnalyticsDate } from '../utils/analyticsFormatting';
import { AnalyticsGroups } from './AnalyticsGroups';
import { AnalyticsSummary } from './AnalyticsSummary';
import { AnalyticsTrendChart } from './AnalyticsTrendChart';
import { AnalyticsWidget } from './AnalyticsWidget';
import { DurationPercentiles } from './DurationPercentiles';
import { EmptyState } from './ui/EmptyState';

interface DetentionAnalyticsDashboardProps { token: string; query: DetentionAnalyticsQuery; permissions: string[]; }

export function DetentionAnalyticsDashboard({ token, query, permissions }: DetentionAnalyticsDashboardProps) {
  const props = { token, query };
  return <div className="analytics-dashboard">
    <h3 className="analytics-section-title">События за период</h3>
    <div className="analytics-summary-grid">
      <AnalyticsSummary {...props} title="Создано заявок" load={api.getDetentionCreatedCount} />
      <AnalyticsSummary {...props} title="Помещено под стражу" load={api.getDetentionAdmittedCount} />
      <AnalyticsSummary {...props} title="Освобождено за период" load={api.getDetentionReleasedCount} />
    </div>
    <div className="analytics-grid">
      <AnalyticsWidget {...props} title="Динамика заключений" load={api.getDetentionTrend}>{(data) =>
        <AnalyticsTrendChart label="Динамика заключений" buckets={data.buckets} bucketUnit={data.meta.bucketUnit} series={[
          { key: 'created', label: 'Создано' }, { key: 'admitted', label: 'Под стражу' }, { key: 'released', label: 'Освобождено' },
        ]} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="Фактическое время под стражей" load={api.getDetentionCustodyDuration}>{(data) =>
        <DurationPercentiles value={data.percentiles} />
      }</AnalyticsWidget>
    </div>
    <h3 className="analytics-section-title">Состояние на {formatAnalyticsDate(query.to)}</h3>
    <div className="analytics-summary-grid">
      <AnalyticsSummary {...props} title="Ожидают прибытия" load={api.getDetentionAwaitingAtTo} />
      <AnalyticsSummary {...props} title="Под стражей" load={api.getDetentionInCustodyAtTo} />
      <AnalyticsSummary {...props} title="Срок истёк" load={api.getDetentionOverdueAtTo} />
    </div>
    <div className="analytics-grid">
      <AnalyticsWidget {...props} title="По статусам" load={api.getDetentionByStatus}>{(data) =>
        <AnalyticsGroups groups={data.groups} labels={DETENTION_STATUS_LABELS} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="Под стражей по видам" load={api.getDetentionByAlien}>{(data) =>
        <AnalyticsGroups groups={data.groups} canReadAliens={permissions.includes('ALIEN_READ')} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="Назначенный срок" load={api.getDetentionByAssignedTerm}>{(data) =>
        <AnalyticsGroups groups={data.groups} labels={ASSIGNED_TERM_LABELS} />
      }</AnalyticsWidget>
      <AnalyticsWidget {...props} title="Оставшееся время" load={api.getDetentionByRemainingTime}>{(data) =>
        <AnalyticsGroups groups={data.groups} labels={REMAINING_TIME_LABELS} />
      }</AnalyticsWidget>
    </div>
    <AnalyticsWidget {...props} title="Ближайшие освобождения" load={api.getDetentionUpcomingReleases}>{(data) => <>
      <p className="analytics-note">{data.totalCount > 10 ? `Показаны первые 10 из ${data.totalCount}` : `Всего: ${data.totalCount}`}</p>
      {data.items.length ? <div className="incidents-table-wrapper"><table className="incidents-table">
        <thead><tr><th>Заявка</th><th>Субъект</th><th>Вид</th><th>Инцидент</th><th>Плановое освобождение</th></tr></thead>
        <tbody>{data.items.map((item) => <tr key={item.requestId}>
          <td>{permissions.includes('DETENTION_READ') ? <Link to={`/detention-requests/${item.requestId}`} className="knowledge-base__name">
            Заявка #{item.requestId}</Link> : `Заявка #${item.requestId}`}</td><td>{item.subjectName}</td>
          <td>{item.alienId && permissions.includes('ALIEN_READ') ? <Link to={`/aliens/${item.alienId}`} className="knowledge-base__name">
            {item.alienName ?? `Вид #${item.alienId}`}</Link> : item.alienName ?? 'Не определён'}</td>
          <td>{permissions.includes('INCIDENT_READ') ? <Link to={`/incidents/${item.incidentId}`} className="knowledge-base__name">
            Инцидент #{item.incidentId}</Link> : `Инцидент #${item.incidentId}`}</td>
          <td>{formatAnalyticsDate(item.plannedReleaseAt)}</td>
        </tr>)}</tbody>
      </table></div> : <EmptyState title="Ближайших освобождений нет" />}
    </>}</AnalyticsWidget>
    <p className="analytics-note">Сроки рассчитаны на выбранную дату. Карточки показывают текущее состояние.</p>
  </div>;
}
