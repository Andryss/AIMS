import { DETENTION_STATUS_LABELS } from '../detentionLabels';
import { formatEquipmentDate } from '../equipmentLabels';
import { DetentionRequestHistorySnapshot } from '../types';

export type DetentionHistoryValue = string | { kind: 'user' | 'incident' | 'alien'; id: number | null };
export interface DetentionHistoryDiffRow { label: string; oldValue?: DetentionHistoryValue; newValue: DetentionHistoryValue; }

function fields({ request }: DetentionRequestHistorySnapshot): Record<string, DetentionHistoryValue> {
  return {
    'Субъект': request.subjectName,
    'Описание субъекта': request.subjectDescription,
    'Вид': { kind: 'alien', id: request.alienId ?? null },
    'Срок, суток': String(request.durationDays),
    'Инцидент': { kind: 'incident', id: request.incidentId },
    'Статус': DETENTION_STATUS_LABELS[request.status],
    'Автор': { kind: 'user', id: request.createdByUserId },
    'Рассмотрел': { kind: 'user', id: request.reviewedByUserId ?? null },
    'Дата прибытия': request.admittedAt ? formatEquipmentDate(request.admittedAt) : '—',
    'Плановое освобождение': request.plannedReleaseAt ? formatEquipmentDate(request.plannedReleaseAt) : '—',
    'Фактическое освобождение': request.releasedAt ? formatEquipmentDate(request.releasedAt) : '—',
  };
}

export function buildDetentionHistoryDiff(current: DetentionRequestHistorySnapshot, previous?: DetentionRequestHistorySnapshot): DetentionHistoryDiffRow[] {
  const before = previous ? fields(previous) : undefined;
  return Object.entries(fields(current)).filter(([label, value]) => !before || JSON.stringify(before[label]) !== JSON.stringify(value))
    .map(([label, value]) => ({ label, oldValue: before?.[label], newValue: value }));
}
