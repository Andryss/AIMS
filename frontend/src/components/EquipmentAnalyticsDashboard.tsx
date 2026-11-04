import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React from 'react';
import { Link } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS, MOVEMENT_OPERATION_LABELS } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentAnalyticsQuery, EquipmentType, OutstandingEquipmentAnalyticsRow } from '../types';

import { AnalyticsSummary } from './AnalyticsSummary';
import { AnalyticsTrendChart } from './AnalyticsTrendChart';
import { AnalyticsWidget } from './AnalyticsWidget';
import { UserChip } from './UserChip';
import { EmptyState } from './ui/EmptyState';

interface EquipmentAnalyticsDashboardProps { token: string; query: EquipmentAnalyticsQuery; permissions: string[]; }

function OutstandingRows({ token, items, permissions }: {
  token: string; items: OutstandingEquipmentAnalyticsRow[]; permissions: string[];
}) {
  const users = useUsersMap(token, permissions.includes('USER_READ') ? items.map((item) => item.responsibleUserId) : []);
  if (!items.length) { return <EmptyState title="Невозвращённого оборудования нет" />; }
  return <div className="data-table-wrapper"><table className="data-table">
    <thead><tr><th>Инцидент</th><th>Место</th><th>Ответственный</th><th>Количество оборудования</th></tr></thead>
    <tbody>{items.map((item) => <tr key={`${item.incidentId}:${item.responsibleUserId}`}>
      <td>{permissions.includes('INCIDENT_READ') ? <Link to={`/incidents/${item.incidentId}`} className="entity-link">
        Инцидент #{item.incidentId}</Link> : `Инцидент #${item.incidentId}`}</td>
      <td>{item.location}</td><td><UserChip login={formatUserLabel(item.responsibleUserId, users)} size={24} /></td>
      <td>{Object.entries(item.unitsByType).map(([type, count]) => <div key={type}>{EQUIPMENT_TYPE_LABELS[type as EquipmentType] ?? type}: {count}</div>)}</td>
    </tr>)}</tbody>
  </table></div>;
}

export function EquipmentAnalyticsDashboard({ token, query, permissions }: EquipmentAnalyticsDashboardProps) {
  const props = { token, query };
  const stock = { token, query: { from: query.from, to: query.to, equipmentType: query.equipmentType } };
  return <div className="analytics-dashboard">
    <h3 className="analytics-section-title">События за период</h3>
    <div className="analytics-summary-grid">
      <AnalyticsSummary {...props} title="Поступило" load={api.getEquipmentReceivedCount} />
      <AnalyticsSummary {...props} title="Выдано за период" load={api.getEquipmentIssuedCount} />
      <AnalyticsSummary {...props} title="Возвращено" load={api.getEquipmentReturnedCount} />
    </div>
    <AnalyticsWidget {...props} title="Динамика оборудования" load={api.getEquipmentTrend}>{(data) =>
      <AnalyticsTrendChart label="Динамика оборудования" buckets={data.buckets} bucketUnit={data.meta.bucketUnit} series={[
        { key: 'received', label: 'Поступило' }, { key: 'issued', label: 'Выдано' }, { key: 'returned', label: 'Возвращено' },
      ]} />
    }</AnalyticsWidget>
    <h3 className="analytics-section-title">Состояние на {formatDateTime(query.to)}</h3>
    <div className="analytics-summary-grid">
      <AnalyticsSummary {...stock} title="Всего оборудования" load={api.getEquipmentTotalAtTo} />
      <AnalyticsSummary {...stock} title="На складе" load={api.getEquipmentAvailableAtTo} />
      <AnalyticsSummary {...stock} title="Выдано на дату" load={api.getEquipmentIssuedAtTo} />
    </div>
    <AnalyticsWidget {...stock} title="Остатки по типам" load={api.getEquipmentInventoryByType}>{(data) =>
      data.items.length ? <div className="data-table-wrapper"><table className="data-table">
        <thead><tr><th>Тип</th><th>На складе</th><th>Выдано</th><th>Всего</th><th>Доля выданного</th></tr></thead>
        <tbody>{data.items.map((item) => <tr key={item.equipmentType}>
          <td>{permissions.includes('EQUIPMENT_READ') ? <Link className="entity-link" to={`/equipment?type=${item.equipmentType}`}>
            {EQUIPMENT_TYPE_LABELS[item.equipmentType]}</Link> : EQUIPMENT_TYPE_LABELS[item.equipmentType]}</td>
          <td>{item.available}</td><td>{item.issued}</td><td>{item.total}</td><td>{item.issuedShare == null ? '—'
            : new Intl.NumberFormat('ru-RU', { style: 'percent', maximumFractionDigits: 1 }).format(item.issuedShare)}</td>
        </tr>)}</tbody>
      </table></div> : <EmptyState title="Оборудование не найдено" />
    }</AnalyticsWidget>
    <div className="analytics-grid">
      <AnalyticsWidget {...props} title="Открытые заявки на движение" load={api.getEquipmentOpenRequests}>{(data) => <>
        <p className="analytics-note">Всего заявок: {data.totalCount}</p>
        {data.items.length ? <div className="data-table-wrapper"><table className="data-table">
          <thead><tr><th>Тип</th><th>Операция</th><th>Заявок</th><th>Количество оборудования</th></tr></thead>
          <tbody>{data.items.map((item) => <tr key={`${item.equipmentType}:${item.operation}`}><td>{EQUIPMENT_TYPE_LABELS[item.equipmentType]}</td>
            <td>{MOVEMENT_OPERATION_LABELS[item.operation]}</td><td>{item.requestCount}</td><td>{item.unitCount}</td></tr>)}</tbody>
        </table></div> : <EmptyState title="Открытых заявок нет" />}
      </>}</AnalyticsWidget>
      <AnalyticsWidget {...stock} title="Ожидаемые поставки" load={api.getEquipmentExpectedDeliveriesByType}>{(data) =>
        data.items.length ? <div className="data-table-wrapper"><table className="data-table">
          <thead><tr><th>Тип</th><th>Тендеров</th><th>Количество оборудования</th></tr></thead>
          <tbody>{data.items.map((item) => <tr key={item.equipmentType}><td>{EQUIPMENT_TYPE_LABELS[item.equipmentType]}</td>
            <td>{item.tenderCount}</td><td>{item.unitCount}</td></tr>)}</tbody>
        </table></div> : <EmptyState title="Ожидаемых поставок нет" />
      }</AnalyticsWidget>
    </div>
    <AnalyticsWidget {...props} title="Активные выдачи по инцидентам" load={api.getEquipmentOutstandingByIncident}>{(data) => <>
      <p className="analytics-note">{data.totalCount > 10 ? `Показаны первые 10 из ${data.totalCount}` : `Всего групп: ${data.totalCount}`}</p>
      <OutstandingRows token={token} items={data.items} permissions={permissions} />
    </>}</AnalyticsWidget>
    <p className="analytics-note">Карточки и списки по ссылкам показывают текущее состояние.</p>
  </div>;
}
