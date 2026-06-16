import {
  IncidentComment,
  IncidentHistoryEntry,
  IncidentResponse,
} from '../types';

export function collectIncidentUserIds(
  incident: IncidentResponse | null,
  history: IncidentHistoryEntry[],
  comments: IncidentComment[],
): number[] {
  const ids = new Set<number>();
  if (incident?.responsibleUserId != null) {
    ids.add(incident.responsibleUserId);
  }
  incident?.executorUserIds?.forEach((id) => ids.add(id));
  history.forEach((entry) => {
    if (entry.changedByUserId != null) {
      ids.add(entry.changedByUserId);
    }
    if (entry.snapshot.responsibleUserId != null) {
      ids.add(entry.snapshot.responsibleUserId);
    }
    entry.snapshot.executorUserIds?.forEach((id) => ids.add(id));
  });
  comments.forEach((comment) => ids.add(comment.authorUserId));
  return Array.from(ids);
}

export async function loadUsersMap(
  token: string,
  ids: number[],
  batchUsers: (token: string, ids: number[]) => Promise<{ items: { id: number; login: string }[] }>,
): Promise<Map<number, string>> {
  if (ids.length === 0) {
    return new Map();
  }
  const response = await batchUsers(token, ids);
  const map = new Map<number, string>();
  response.items.forEach((user) => map.set(user.id, user.login));
  return map;
}

export function formatUserLabel(
  userId: number | null | undefined,
  usersMap: Map<number, string>,
): string {
  if (userId == null) {
    return '—';
  }
  return usersMap.get(userId) ?? `#${userId}`;
}
