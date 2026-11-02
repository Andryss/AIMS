import { EQUIPMENT_STATUS_LABELS, EQUIPMENT_TYPE_LABELS, MOVEMENT_OPERATION_LABELS,
  MOVEMENT_STATUS_LABELS, formatEquipmentDate } from '../equipmentLabels';
import { EquipmentMovementResponse, EquipmentResponse } from '../types';

export type EquipmentHistoryValue = string
  | { kind: 'user' | 'movement' | 'incident'; id: number | null }
  | { kind: 'equipment'; items: Array<Pick<EquipmentResponse, 'id' | 'model' | 'serialNumber'>> };

export interface EquipmentHistoryDiffRow {
  label: string;
  oldValue?: EquipmentHistoryValue;
  newValue: EquipmentHistoryValue;
}

type Fields = Record<string, EquipmentHistoryValue>;

function compareFields(current: Fields, previous?: Fields): EquipmentHistoryDiffRow[] {
  return Object.entries(current)
    .filter(([label, value]) => !previous || JSON.stringify(previous[label]) !== JSON.stringify(value))
    .map(([label, value]) => ({ label, oldValue: previous?.[label], newValue: value }));
}

function equipmentFields(item: EquipmentResponse): Fields {
  return {
    'Тип': EQUIPMENT_TYPE_LABELS[item.type],
    'Модель / название': item.model,
    'Серийный номер': item.serialNumber,
    'Состояние': EQUIPMENT_STATUS_LABELS[item.status],
    'Держатель': { kind: 'user', id: item.holderUserId ?? null },
    'Текущая выдача': { kind: 'movement', id: item.currentIssueMovementId ?? null },
  };
}

function movementFields(movement: EquipmentMovementResponse): Fields {
  return {
    'Операция': MOVEMENT_OPERATION_LABELS[movement.operation],
    'Статус': MOVEMENT_STATUS_LABELS[movement.status],
    'Инцидент': { kind: 'incident', id: movement.incidentId },
    'Ответственный': { kind: 'user', id: movement.responsibleUserId },
    'Исходная выдача': { kind: 'movement', id: movement.sourceIssueMovementId ?? null },
    'Оборудование': { kind: 'equipment', items: [...movement.equipment].sort((a, b) => a.id - b.id)
      .map(({ id, model, serialNumber }) => ({ id, model, serialNumber })) },
    'Комментарий': movement.comment || '—',
    'Выполнена': movement.completedAt ? formatEquipmentDate(movement.completedAt) : '—',
  };
}

export function buildEquipmentHistoryDiff(current: EquipmentResponse, previous?: EquipmentResponse): EquipmentHistoryDiffRow[] {
  return compareFields(equipmentFields(current), previous ? equipmentFields(previous) : undefined);
}

export function buildEquipmentMovementHistoryDiff(current: EquipmentMovementResponse,
  previous?: EquipmentMovementResponse): EquipmentHistoryDiffRow[] {
  return compareFields(movementFields(current), previous ? movementFields(previous) : undefined);
}
