import React from 'react';
import { Link } from 'react-router-dom';
import { EquipmentHistoryDiffRow, EquipmentHistoryValue } from '../utils/equipmentHistoryDiff';
import { formatUserLabel } from '../utils/incidentUsers';
import { UserChip } from './UserChip';

interface EquipmentHistoryChangesProps {
  rows: EquipmentHistoryDiffRow[];
  users: Map<number, string>;
  canReadMovements?: boolean;
  equipmentId?: number;
}

export function EquipmentHistoryChanges({ rows, users, canReadMovements = true, equipmentId }: EquipmentHistoryChangesProps) {
  const renderValue = (value: EquipmentHistoryValue): React.ReactNode => {
    if (typeof value === 'string') { return value; }
    if (value.kind === 'equipment') {
      return value.items.length ? value.items.map((item, index) => <React.Fragment key={item.id}>
        {index > 0 && '; '}{item.model} · {item.serialNumber}
      </React.Fragment>) : '—';
    }
    if (value.id == null) { return '—'; }
    if (value.kind === 'user') { return <UserChip login={formatUserLabel(value.id, users)} size={24} />; }
    if (value.kind === 'incident') { return <Link to={`/incidents/${value.id}`}>Инцидент #{value.id}</Link>; }
    return canReadMovements ? <Link to={`/equipment-movements/${value.id}`} state={equipmentId ? { equipmentId } : undefined}>
      Заявка #{value.id}</Link> : `Заявка #${value.id}`;
  };

  return rows.length === 0 ? <p className="text-muted">Без изменений полей</p> : (
    <ul className="history-diff-list">
      {rows.map((row) => <li key={row.label} className="history-diff__row">
        <span className="history-diff__label">{row.label}:</span>
        <span className="text-pre-wrap">
          {row.oldValue !== undefined && <>{renderValue(row.oldValue)}{' → '}</>}{renderValue(row.newValue)}
        </span>
      </li>)}
    </ul>
  );
}
