import { USER_BATCH_SIZE } from '../constants';

export async function loadUsersMap(
  token: string,
  ids: number[],
  batchUsers: (token: string, ids: number[]) => Promise<{ items: { id: number; login: string }[] }>,
): Promise<Map<number, string>> {
  if (ids.length === 0) {
    return new Map();
  }
  const batches: number[][] = [];
  for (let start = 0; start < ids.length; start += USER_BATCH_SIZE) {
    batches.push(ids.slice(start, start + USER_BATCH_SIZE));
  }
  const responses = await Promise.all(batches.map((batch) => batchUsers(token, batch)));
  const map = new Map<number, string>();
  responses.forEach((response) => response.items.forEach((user) => map.set(user.id, user.login)));
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
