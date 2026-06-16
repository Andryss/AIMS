import {
  collectIncidentUserIds,
  formatUserLabel,
  loadUsersMap,
} from './incidentUsers';
import { IncidentComment, IncidentHistoryEntry, IncidentResponse } from '../types';

const incident: IncidentResponse = {
  id: 1,
  status: 'READY_FOR_EXECUTION',
  eventType: 'UNIDENTIFIED_SIGHTING',
  location: 'Area',
  detectedAt: '2025-06-01T10:00:00Z',
  description: 'Desc',
  attachmentFileIds: [],
  responsibleUserId: 10,
  executorUserIds: [20, 21],
  createdAt: '2025-06-01T10:00:00Z',
  updatedAt: '2025-06-01T10:00:00Z',
};

const history: IncidentHistoryEntry[] = [
  {
    id: 1,
    changedAt: '2025-06-01T10:00:00Z',
    changedByUserId: 30,
    snapshot: {
      status: 'DRAFT',
      eventType: 'UNIDENTIFIED_SIGHTING',
      location: 'Area',
      detectedAt: '2025-06-01T10:00:00Z',
      description: 'Desc',
      attachmentFileIds: [],
      responsibleUserId: 40,
      executorUserIds: [50],
    },
  },
];

const comments: IncidentComment[] = [
  {
    id: 1,
    incidentId: 1,
    authorUserId: 60,
    text: 'Hi',
    createdAt: '2025-06-01T11:00:00Z',
  },
];

describe('collectIncidentUserIds', () => {
  it('deduplicates ids from incident, history and comments', () => {
    const ids = collectIncidentUserIds(incident, history, comments);
    expect(ids.sort((a, b) => a - b)).toEqual([10, 20, 21, 30, 40, 50, 60]);
  });

  it('returns empty array when nothing to resolve', () => {
    expect(collectIncidentUserIds(null, [], [])).toEqual([]);
  });
});

describe('loadUsersMap', () => {
  it('builds map from batch response', async () => {
    const batchUsers = jest.fn().mockResolvedValue({
      items: [
        { id: 1, login: 'agent' },
        { id: 2, login: 'agent2' },
      ],
    });

    const map = await loadUsersMap('tok', [1, 2], batchUsers);
    expect(batchUsers).toHaveBeenCalledWith('tok', [1, 2]);
    expect(map.get(1)).toBe('agent');
    expect(map.get(2)).toBe('agent2');
  });

  it('skips batch call for empty ids', async () => {
    const batchUsers = jest.fn();
    const map = await loadUsersMap('tok', [], batchUsers);
    expect(batchUsers).not.toHaveBeenCalled();
    expect(map.size).toBe(0);
  });
});

describe('formatUserLabel', () => {
  it('formats known and unknown users', () => {
    const map = new Map<number, string>([[5, 'operator']]);
    expect(formatUserLabel(5, map)).toBe('operator');
    expect(formatUserLabel(99, map)).toBe('#99');
    expect(formatUserLabel(null, map)).toBe('—');
  });
});
