import { formatDateTime } from './dateTime';
import { EQUIPMENT_STATUS_LABELS, EQUIPMENT_TYPE_LABELS, MOVEMENT_OPERATION_LABELS, MOVEMENT_STATUS_LABELS } from '../equipmentLabels';
import { EquipmentMovementResponse, EquipmentResponse } from '../types';
import { compareHistoryFields, HistoryDiffRow } from './historyDiff';

export type EquipmentHistoryValue = string
  | { kind: 'user' | 'movement' | 'incident' | 'tender'; id: number | null }
  | { kind: 'equipment'; items: Array<Pick<EquipmentResponse, 'id' | 'model' | 'serialNumber'>> };

export type EquipmentHistoryDiffRow = HistoryDiffRow<EquipmentHistoryValue>;

type Fields = Record<string, EquipmentHistoryValue>;

function equipmentFields(item: EquipmentResponse): Fields {
  return {
    'Тип': EQUIPMENT_TYPE_LABELS[item.type],
    'Модель / название': item.model,
    'Серийный номер': item.serialNumber,
    'Состояние': EQUIPMENT_STATUS_LABELS[item.status],
    'Держатель': { kind: 'user', id: item.holderUserId ?? null },
    'Тендер-основание': { kind: 'tender', id: item.sourceTenderId ?? null },
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
    'Выполнена': movement.completedAt ? formatDateTime(movement.completedAt) : '—',
  };
}

export function buildEquipmentHistoryDiff(current: EquipmentResponse, previous?: EquipmentResponse): EquipmentHistoryDiffRow[] {
  return compareHistoryFields(equipmentFields(current), previous ? equipmentFields(previous) : undefined);
}

export function buildEquipmentMovementHistoryDiff(current: EquipmentMovementResponse,
  previous?: EquipmentMovementResponse): EquipmentHistoryDiffRow[] {
  return compareHistoryFields(movementFields(current), previous ? movementFields(previous) : undefined);
}
