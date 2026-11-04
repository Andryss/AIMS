import { IncidentComment, IncidentHistoryEntry, IncidentResponse } from '../types';

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

export { loadUsersMap, formatUserLabel } from './users';
