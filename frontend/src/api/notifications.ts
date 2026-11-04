import type { NotificationListResponse, UnreadCountResponse } from '../types';
import { requestJson } from './http';

export function listNotifications(
  token: string,
  page: number,
  size: number,
): Promise<NotificationListResponse> {
  return requestJson<NotificationListResponse>(
    `/notifications?page=${page}&size=${size}`,
    { method: 'GET' },
    token,
  );
}

export function getUnreadNotificationsCount(token: string): Promise<UnreadCountResponse> {
  return requestJson<UnreadCountResponse>('/notifications/unread-count', { method: 'GET' }, token);
}

export function markNotificationRead(token: string, id: number): Promise<void> {
  return requestJson<void>(
    `/notifications/${id}/read`,
    { method: 'PATCH' },
    token,
  );
}
