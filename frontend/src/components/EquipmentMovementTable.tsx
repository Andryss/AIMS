import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { MOVEMENT_OPERATION_LABELS, MOVEMENT_STATUS_LABELS } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentMovementResponse } from '../types';

import { StatusChip } from './ui/StatusChip';
import { UserChip } from './UserChip';

interface EquipmentMovementTableProps {
  token: string;
  items: EquipmentMovementResponse[];
  search?: string;
  fromIncidentId?: number;
}

export function EquipmentMovementTable({ token, items, search = '', fromIncidentId }: EquipmentMovementTableProps) {
  const navigate = useNavigate();
  const users = useUsersMap(token, items.map((item) => item.responsibleUserId));

  return (
    <div className="data-table-wrapper">
      <table className="data-table">
        <thead><tr><th>Заявка</th><th>Операция</th><th>Статус</th><th>Инцидент</th>
          <th>Ответственный</th><th>Количество оборудования</th><th>Создана</th></tr></thead>
        <tbody>{items.map((item) => {
          const path = `/equipment-movements/${item.id}${search}`;
          const state = fromIncidentId ? { incidentId: fromIncidentId } : undefined;
          return <tr key={item.id} className="data-table__row" onClick={() => navigate(path, { state })}>
            <td><Link to={path} state={state} className="entity-link"
              onClick={(event) => event.stopPropagation()}>Заявка #{item.id}</Link></td>
            <td>{MOVEMENT_OPERATION_LABELS[item.operation]}</td>
            <td><StatusChip status={item.status.toLowerCase()} label={MOVEMENT_STATUS_LABELS[item.status]} /></td>
            <td><Link to={`/incidents/${item.incidentId}`} className="entity-link"
              onClick={(event) => event.stopPropagation()}>Инцидент #{item.incidentId}</Link></td>
            <td><UserChip login={formatUserLabel(item.responsibleUserId, users)} size={24} /></td>
            <td>{item.equipment.length}</td><td>{formatDateTime(item.createdAt)}</td>
          </tr>;
        })}</tbody>
      </table>
    </div>
  );
}
