import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { DETENTION_STATUS_LABELS } from '../detentionLabels';

import { useUsersMap } from '../hooks/useUsersMap';
import { DetentionRequestResponse } from '../types';

import { UserChip } from './UserChip';
import { StatusChip } from './ui/StatusChip';

interface DetentionRequestTableProps {
  token: string; items: DetentionRequestResponse[]; search?: string; fromIncidentId?: number; canReadIncidents?: boolean;
}

export function DetentionRequestTable({ token, items, search = '', fromIncidentId, canReadIncidents = true }: DetentionRequestTableProps) {
  const navigate = useNavigate();
  const users = useUsersMap(token, items.map((item) => item.createdByUserId));

  return <div className="data-table-wrapper"><table className="data-table">
    <thead><tr><th>Номер</th><th>Субъект</th><th>Инцидент</th><th>Статус</th><th>Срок, суток</th><th>Автор</th><th>Создана</th></tr></thead>
    <tbody>{items.map((item) => {
      const path = `/detention-requests/${item.id}${search}`;
      const state = fromIncidentId ? { incidentId: fromIncidentId } : undefined;
      return <tr key={item.id} className="data-table__row" onClick={() => navigate(path, { state })}>
        <td><Link to={path} state={state} className="entity-link" onClick={(event) => event.stopPropagation()}>Заявка #{item.id}</Link></td>
        <td>{item.subjectName}</td><td>{canReadIncidents ? <Link to={`/incidents/${item.incidentId}`}
          onClick={(event) => event.stopPropagation()}>Инцидент #{item.incidentId}</Link> : `Инцидент #${item.incidentId}`}</td>
        <td><StatusChip status={item.status.toLowerCase()} label={DETENTION_STATUS_LABELS[item.status]} /></td>
        <td>{item.durationDays}</td><td><UserChip login={formatUserLabel(item.createdByUserId, users)} size={24} /></td>
        <td>{formatDateTime(item.createdAt)}</td>
      </tr>;
    })}</tbody>
  </table></div>;
}
