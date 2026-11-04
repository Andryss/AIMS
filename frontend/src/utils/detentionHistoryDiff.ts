import { formatDateTime } from './dateTime';
import { DETENTION_STATUS_LABELS } from '../detentionLabels';

import { DetentionRequestHistorySnapshot } from '../types';
import { compareHistoryFields, HistoryDiffRow } from './historyDiff';

export type DetentionHistoryValue = string | { kind: 'user' | 'incident' | 'alien'; id: number | null };
export type DetentionHistoryDiffRow = HistoryDiffRow<DetentionHistoryValue>;

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
    'Дата прибытия': request.admittedAt ? formatDateTime(request.admittedAt) : '—',
    'Плановое освобождение': request.plannedReleaseAt ? formatDateTime(request.plannedReleaseAt) : '—',
    'Фактическое освобождение': request.releasedAt ? formatDateTime(request.releasedAt) : '—',
  };
}

export function buildDetentionHistoryDiff(current: DetentionRequestHistorySnapshot, previous?: DetentionRequestHistorySnapshot): DetentionHistoryDiffRow[] {
  return compareHistoryFields(fields(current), previous ? fields(previous) : undefined);
}
