import { buildIncidentHistoryDiffs, formatHistoryEntryHeader } from './incidentHistoryDiff';
import { IncidentHistoryEntry } from '../types';

function entry(
  id: number,
  changedAt: string,
  changedByUserId: number,
  snapshot: IncidentHistoryEntry['snapshot'],
): IncidentHistoryEntry {
  return {
    id,
    changedAt,
    changedByUserId,
    snapshot,
  };
}

const baseSnapshot = {
  status: 'DRAFT' as const,
  eventType: 'UNIDENTIFIED_SIGHTING' as const,
  location: 'Area 51',
  detectedAt: '2025-06-01T10:00:00Z',
  description: 'Bright light',
  attachmentFileIds: [1],
  alienId: null,
  responsibleUserId: null,
  executorUserIds: [] as number[],
};

describe('buildIncidentHistoryDiffs', () => {
  const usersMap = new Map<number, string>([
    [1, 'operator'],
    [2, 'analyst'],
  ]);

  it('marks first entry as creation with all fields', () => {
    const blocks = buildIncidentHistoryDiffs([
      entry(1, '2025-06-01T10:00:00Z', 1, baseSnapshot),
    ], usersMap);

    expect(blocks).toHaveLength(1);
    expect(blocks[0].isCreation).toBe(true);
    expect(blocks[0].rows).toHaveLength(11);
    expect(blocks[0].rows.find((r) => r.label === 'Статус')?.newValue).toBe('Черновик');
    expect(blocks[0].title).toContain('operator');
  });

  it('diffs status and attachments between consecutive snapshots', () => {
    const blocks = buildIncidentHistoryDiffs([
      entry(1, '2025-06-01T10:00:00Z', 1, baseSnapshot),
      entry(
        2,
        '2025-06-01T11:00:00Z',
        2,
        {
          ...baseSnapshot,
          status: 'READY_FOR_ANALYSIS',
          attachmentFileIds: [1, 2],
          alienId: 5,
        },
      ),
    ], usersMap);

    expect(blocks[1].isCreation).toBe(false);
    const labels = blocks[1].rows.map((r) => r.label);
    expect(labels).toEqual(['Статус', 'Вложения', 'Тип инопланетянина']);
  });

  it('returns empty diff rows when snapshots are identical', () => {
    const blocks = buildIncidentHistoryDiffs([
      entry(1, '2025-06-01T10:00:00Z', 1, baseSnapshot),
      entry(2, '2025-06-01T11:00:00Z', 1, { ...baseSnapshot }),
    ], usersMap);

    expect(blocks[1].rows).toHaveLength(0);
  });
});

describe('formatHistoryEntryHeader', () => {
  it('uses usersMap for changedBy label', () => {
    const header = formatHistoryEntryHeader(
      entry(1, '2025-06-01T10:00:00Z', 42, baseSnapshot),
      new Map([[42, 'agent']]),
    );
    expect(header).toContain('agent');
  });
});
