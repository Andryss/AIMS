import type { Page } from './common';

export interface NotificationItem {
  id: number;
  message: string;
  relatedEntities: string[];
  read: boolean;
  readAt?: string;
  createdAt: string;
}

export type NotificationListResponse = Page<NotificationItem>;

export interface UnreadCountResponse {
  count: number;
}
