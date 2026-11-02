import { EquipmentMovementOperation, EquipmentMovementStatus, EquipmentResponse, EquipmentStatus, EquipmentType } from './types';

export const EQUIPMENT_PAGE_SIZE = 10;

export const EQUIPMENT_TYPE_LABELS: Record<EquipmentType, string> = {
  NEURALYZER: 'Нейралайзер',
  NEUTRALIZER: 'Нейтрализатор',
};

export const EQUIPMENT_STATUS_LABELS: Record<EquipmentStatus, string> = {
  AVAILABLE: 'На складе',
  ISSUED: 'Выдано',
};

export const MOVEMENT_OPERATION_LABELS: Record<EquipmentMovementOperation, string> = {
  ISSUE: 'Выдача',
  RETURN: 'Возврат',
};

export const MOVEMENT_STATUS_LABELS: Record<EquipmentMovementStatus, string> = {
  PENDING: 'В ожидании',
  PROCESSING: 'Обрабатывается',
  COMPLETED: 'Выполнена',
  CANCELLED: 'Отменена',
  REJECTED: 'Отклонена',
};

export function formatEquipmentDate(value: string): string {
  return new Date(value).toLocaleString('ru-RU');
}

export function positiveId(value: string | null | undefined): number | undefined {
  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0 ? id : undefined;
}

export function outstandingEquipment(equipment: EquipmentResponse[], issueId: number): EquipmentResponse[] {
  return equipment.filter((item) => item.status === 'ISSUED' && item.currentIssueMovementId === issueId);
}
