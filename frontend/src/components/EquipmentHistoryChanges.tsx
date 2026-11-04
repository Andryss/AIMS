import { formatUserLabel } from '../utils/users';
import React from 'react';
import { Link } from 'react-router-dom';
import { EquipmentHistoryDiffRow, EquipmentHistoryValue } from '../utils/equipmentHistoryDiff';

import { UserChip } from './UserChip';
import { HistoryDiffList } from './ui/HistoryDiffList';

interface EquipmentHistoryChangesProps {
  rows: EquipmentHistoryDiffRow[];
  users: Map<number, string>;
  canReadMovements?: boolean;
  canReadTenders?: boolean;
  equipmentId?: number;
}

export function EquipmentHistoryChanges({ rows, users, canReadMovements = true, canReadTenders = false, equipmentId }: EquipmentHistoryChangesProps) {
  const renderValue = (value: EquipmentHistoryValue): React.ReactNode => {
    if (typeof value === 'string') { return value; }
    if (value.kind === 'equipment') {
      return value.items.length ? value.items.map((item, index) => <React.Fragment key={item.id}>
        {index > 0 && '; '}{item.model} · {item.serialNumber}
      </React.Fragment>) : '—';
    }
    if (value.id == null) { return '—'; }
    if (value.kind === 'user') { return <UserChip login={formatUserLabel(value.id, users)} size={24} />; }
    if (value.kind === 'tender') { return canReadTenders ? <Link to={`/tenders/${value.id}`}>Тендер #{value.id}</Link> : `Тендер #${value.id}`; }
    if (value.kind === 'incident') { return <Link to={`/incidents/${value.id}`}>Инцидент #{value.id}</Link>; }
    return canReadMovements ? <Link to={`/equipment-movements/${value.id}`} state={equipmentId ? { equipmentId } : undefined}>
      Заявка #{value.id}</Link> : `Заявка #${value.id}`;
  };

  return <HistoryDiffList rows={rows} renderValue={renderValue} emptyMessage="Без изменений полей" />;
}
