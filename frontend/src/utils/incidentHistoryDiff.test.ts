import { buildIncidentHistoryDiffs } from './incidentHistoryDiff';
import { IncidentHistoryEntry } from '../types';

function entry(
  id: number,
  changedAt: string,
  login: string,
  snapshot: IncidentHistoryEntry['snapshot'],
): IncidentHistoryEntry {
  return {
    id,
    changedAt,
    changedByLogin: login,
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
};

describe('buildIncidentHistoryDiffs', () => {
  it('marks first entry as creation with all fields', () => {
    const blocks = buildIncidentHistoryDiffs([
      entry(1, '2025-06-01T10:00:00Z', 'operator', baseSnapshot),
    ]);

    expect(blocks).toHaveLength(1);
    expect(blocks[0].isCreation).toBe(true);
    expect(blocks[0].rows).toHaveLength(7);
    expect(blocks[0].rows.find((r) => r.label === 'Статус')?.newValue).toBe('Черновик');
    expect(blocks[0].rows.find((r) => r.label === 'Статус')?.oldValue).toBe('—');
  });

  it('diffs status and attachments between consecutive snapshots', () => {
    const blocks = buildIncidentHistoryDiffs([
      entry(1, '2025-06-01T10:00:00Z', 'operator', baseSnapshot),
      entry(
        2,
        '2025-06-01T11:00:00Z',
        'analyst',
        {
          ...baseSnapshot,
          status: 'READY_FOR_ANALYSIS',
          attachmentFileIds: [1, 2],
          alienId: 5,
        },
      ),
    ]);

    expect(blocks[1].isCreation).toBe(false);
    const labels = blocks[1].rows.map((r) => r.label);
    expect(labels).toEqual(['Статус', 'Вложения', 'Тип инопланетянина']);
    expect(blocks[1].rows[0]).toMatchObject({
      label: 'Статус',
      oldValue: 'Черновик',
      newValue: 'Готов к анализу',
    });
    expect(blocks[1].rows[1]).toMatchObject({
      label: 'Вложения',
      oldValue: '#1',
      newValue: '#1, #2',
    });
    expect(blocks[1].rows[2]).toMatchObject({
      label: 'Тип инопланетянина',
      oldValue: '—',
      newValue: '#5',
    });
  });

  it('returns empty diff rows when snapshots are identical', () => {
    const blocks = buildIncidentHistoryDiffs([
      entry(1, '2025-06-01T10:00:00Z', 'operator', baseSnapshot),
      entry(2, '2025-06-01T11:00:00Z', 'operator', { ...baseSnapshot }),
    ]);

    expect(blocks[1].rows).toHaveLength(0);
  });
});
