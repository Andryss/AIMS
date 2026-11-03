import { EQUIPMENT_TYPE_LABELS, formatEquipmentDate } from '../equipmentLabels';
import { TENDER_PARTICIPANT_STATUS_LABELS, TENDER_STATUS_LABELS } from '../tenderLabels';
import { TenderHistorySnapshot } from '../types';

export type TenderHistoryValue = string | { kind: 'user' | 'incident'; id: number | null }
  | { kind: 'equipment'; ids: number[] };
export interface TenderHistoryDiffRow { label: string; oldValue?: TenderHistoryValue; newValue: TenderHistoryValue; }

function fields(snapshot: TenderHistorySnapshot): Record<string, TenderHistoryValue> {
  const tender = snapshot.tender;
  const winner = snapshot.participants.find((item) => item.id === tender.winnerParticipantId);
  const result: Record<string, TenderHistoryValue> = {
    'Тип': EQUIPMENT_TYPE_LABELS[tender.equipmentType],
    'Количество оборудования': String(tender.quantity),
    'Условия закупки': tender.conditions || '—',
    'Инцидент': { kind: 'incident', id: tender.incidentId ?? null },
    'Статус': TENDER_STATUS_LABELS[tender.status],
    'Автор': { kind: 'user', id: tender.createdByUserId },
    'Победитель': winner?.name ?? (tender.winnerParticipantId ? `Участник #${tender.winnerParticipantId}` : '—'),
    'Завершён': tender.completedAt ? formatEquipmentDate(tender.completedAt) : '—',
    'Полученное оборудование': { kind: 'equipment', ids: snapshot.receivedEquipmentIds },
  };
  snapshot.participants.forEach((item) => {
    result[`Участник #${item.id} · Организация`] = item.name;
    result[`Участник #${item.id} · Предложение`] = item.proposal;
    result[`Участник #${item.id} · Статус`] = TENDER_PARTICIPANT_STATUS_LABELS[item.status];
    result[`Участник #${item.id} · Добавил`] = { kind: 'user', id: item.createdByUserId };
  });
  return result;
}

export function buildTenderHistoryDiff(current: TenderHistorySnapshot, previous?: TenderHistorySnapshot): TenderHistoryDiffRow[] {
  const before = previous ? fields(previous) : undefined;
  return Object.entries(fields(current))
    .filter(([label, value]) => !before || JSON.stringify(before[label]) !== JSON.stringify(value))
    .map(([label, value]) => ({ label, oldValue: before?.[label], newValue: value }));
}
