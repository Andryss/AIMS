import React from 'react';
import { Link } from 'react-router-dom';
import { AnalyticsCountGroup } from '../types';
import { formatAnalyticsNumber } from '../utils/analyticsFormatting';
import { EmptyState } from './ui/EmptyState';

export function AnalyticsGroups({ groups, labels = {}, canReadAliens = false }: {
  groups: AnalyticsCountGroup[]; labels?: Record<string, string>; canReadAliens?: boolean;
}) {
  const max = Math.max(1, ...groups.map((group) => group.count));
  if (!groups.length) { return <EmptyState title="Нет данных" />; }
  return <ul className="analytics-groups">{groups.map((group) => {
    const label = group.key === 'UNKNOWN' ? labels.UNKNOWN ?? 'Не определён' : group.key === 'OTHER' ? 'Остальные'
      : group.name ?? labels[group.key] ?? group.key.replace(/^LOCATION:/, '');
    const alienId = /^\d+$/.test(group.key) ? Number(group.key) : null;
    return <li key={group.key}>
      <div className="analytics-groups__label">{canReadAliens && alienId
        ? <Link to={`/aliens/${alienId}`} className="entity-link">{label}</Link> : label}
        <strong>{formatAnalyticsNumber(group.count)}</strong></div>
      <div className="analytics-groups__track" aria-hidden="true"><span style={{ width: `${group.count / max * 100}%` }} /></div>
    </li>;
  })}</ul>;
}
