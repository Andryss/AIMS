import { EVENT_TYPE_LABELS, STATUS_LABELS } from '../incidentLabels';
import { IncidentHistoryEntry, IncidentHistorySnapshot } from '../types';

export interface HistoryDiffRow {
  label: string;
  oldValue: string;
  newValue: string;
}

export interface HistoryDiffBlock {
  entry: IncidentHistoryEntry;
  title: string;
  rows: HistoryDiffRow[];
  isCreation: boolean;
}

type SnapshotField = {
  label: string;
  format: (snapshot: IncidentHistorySnapshot, usersMap: Map<number, string>) => string;
  compare: (prev: IncidentHistorySnapshot, curr: IncidentHistorySnapshot) => boolean;
};

function formatDate(iso: string): string {
  try {
    return new Date(iso).toLocaleString('ru-RU');
  } catch {
    return iso;
  }
}

function formatAttachments(ids: number[]): string {
  if (ids.length === 0) {
    return '—';
  }
  return ids.map((id) => `#${id}`).join(', ');
}

function formatAlienId(alienId: number | null | undefined): string {
  if (alienId == null) {
    return '—';
  }
  return `#${alienId}`;
}

function formatUserId(
  userId: number | null | undefined,
  usersMap: Map<number, string>,
): string {
  if (userId == null) {
    return '—';
  }
  return usersMap.get(userId) ?? `#${userId}`;
}

function formatUserIds(userIds: number[], usersMap: Map<number, string>): string {
  if (userIds.length === 0) {
    return '—';
  }
  return userIds.map((id) => usersMap.get(id) ?? `#${id}`).join(', ');
}

const SNAPSHOT_FIELDS: SnapshotField[] = [
  {
    label: 'Статус',
    format: (s) => STATUS_LABELS[s.status] ?? s.status,
    compare: (prev, curr) => prev.status !== curr.status,
  },
  {
    label: 'Тип события',
    format: (s) => EVENT_TYPE_LABELS[s.eventType] ?? s.eventType,
    compare: (prev, curr) => prev.eventType !== curr.eventType,
  },
  {
    label: 'Место',
    format: (s) => s.location,
    compare: (prev, curr) => prev.location !== curr.location,
  },
  {
    label: 'Время обнаружения',
    format: (s) => formatDate(s.detectedAt),
    compare: (prev, curr) => prev.detectedAt !== curr.detectedAt,
  },
  {
    label: 'Описание',
    format: (s) => s.description,
    compare: (prev, curr) => prev.description !== curr.description,
  },
  {
    label: 'Вложения',
    format: (s) => formatAttachments(s.attachmentFileIds),
    compare: (prev, curr) =>
      JSON.stringify(prev.attachmentFileIds) !== JSON.stringify(curr.attachmentFileIds),
  },
  {
    label: 'Тип инопланетянина',
    format: (s) => formatAlienId(s.alienId),
    compare: (prev, curr) => (prev.alienId ?? null) !== (curr.alienId ?? null),
  },
  {
    label: 'Ответственный',
    format: (s, usersMap) => formatUserId(s.responsibleUserId, usersMap),
    compare: (prev, curr) => (prev.responsibleUserId ?? null) !== (curr.responsibleUserId ?? null),
  },
  {
    label: 'Исполнители',
    format: (s, usersMap) => formatUserIds(s.executorUserIds ?? [], usersMap),
    compare: (prev, curr) =>
      JSON.stringify(prev.executorUserIds ?? []) !== JSON.stringify(curr.executorUserIds ?? []),
  },
];

export function formatHistoryEntryHeader(
  entry: IncidentHistoryEntry,
  usersMap: Map<number, string>,
): string {
  const when = formatDate(entry.changedAt);
  const who = usersMap.get(entry.changedByUserId) ?? `#${entry.changedByUserId}`;
  return `${when} · ${who}`;
}

export function buildIncidentHistoryDiffs(
  entries: IncidentHistoryEntry[],
  usersMap: Map<number, string> = new Map(),
): HistoryDiffBlock[] {
  return entries.map((entry, index) => {
    const title = formatHistoryEntryHeader(entry, usersMap);
    if (index === 0) {
      return {
        entry,
        title,
        isCreation: true,
        rows: SNAPSHOT_FIELDS.map((field) => ({
          label: field.label,
          oldValue: '—',
          newValue: field.format(entry.snapshot, usersMap),
        })),
      };
    }

    const previous = entries[index - 1].snapshot;
    const current = entry.snapshot;
    const rows = SNAPSHOT_FIELDS.filter((field) => field.compare(previous, current)).map(
      (field) => ({
        label: field.label,
        oldValue: field.format(previous, usersMap),
        newValue: field.format(current, usersMap),
      }),
    );

    return {
      entry,
      title,
      isCreation: false,
      rows,
    };
  });
}
