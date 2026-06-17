import React, { useEffect, useRef } from 'react';
import * as api from '../api/client';
import { NotificationItem } from '../types';
import { extractIncidentIds } from '../utils/relatedEntities';
import { formatNotificationTime } from '../utils/formatNotificationTime';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';

interface NotificationsPanelProps {
  token: string;
  open: boolean;
  onClose: () => void;
  onUnreadChange: (count: number) => void;
  onNavigateToIncident?: (incidentId: number) => void;
}

export function NotificationsPanel({
  token,
  open,
  onClose,
  onUnreadChange,
  onNavigateToIncident,
}: NotificationsPanelProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const [items, setItems] = React.useState<NotificationItem[]>([]);
  const [loading, setLoading] = React.useState(false);
  const [error, setError] = React.useState<string | null>(null);
  const [markingId, setMarkingId] = React.useState<number | null>(null);

  useEffect(() => {
    if (!open) {
      return;
    }

    setLoading(true);
    setError(null);
    Promise.all([
      api.listNotifications(token, 0, 20),
      api.getUnreadNotificationsCount(token),
    ])
      .then(([list, unread]) => {
        setItems(list.items);
        onUnreadChange(unread.count);
      })
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message : 'Не удалось загрузить уведомления';
        setError(message);
      })
      .finally(() => setLoading(false));
  }, [open, token, onUnreadChange]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const handleClickOutside = (event: MouseEvent) => {
      if (panelRef.current && !panelRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [open, onClose]);

  const handleMarkRead = async (id: number) => {
    const target = items.find((item) => item.id === id);
    if (!target || target.read || markingId === id) {
      return;
    }

    setMarkingId(id);
    setError(null);

    try {
      await api.markNotificationRead(token, id);
      setItems((prev) =>
        prev.map((item) => (item.id === id ? { ...item, read: true } : item)),
      );
      const unread = await api.getUnreadNotificationsCount(token);
      onUnreadChange(unread.count);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось отметить уведомление';
      setError(message);
    } finally {
      setMarkingId(null);
    }
  };

  if (!open) {
    return null;
  }

  return (
    <div className="notifications__panel" ref={panelRef}>
      <div className="notifications__header">
        <h3>Уведомления</h3>
        <button type="button" className="btn btn--ghost" onClick={onClose} aria-label="Закрыть">
          ×
        </button>
      </div>
      {loading && <LoadingBlock label="Загрузка уведомлений…" inline />}
      {error && (
        <p className="text-error" role="alert" aria-live="polite">
          {error}
        </p>
      )}
      {!loading && !error && items.length === 0 && (
        <EmptyState title="Нет уведомлений" hint="Здесь появятся оповещения по инцидентам." />
      )}
      <ul className="notifications__list">
        {items.map((item) => {
          const incidentIds = extractIncidentIds(item.relatedEntities);

          return (
            <li
              key={item.id}
              className={item.read ? 'notifications__item--read' : 'notifications__item--unread'}
              onClick={() => handleMarkRead(item.id)}
              onKeyDown={(event) => {
                if (!item.read && (event.key === 'Enter' || event.key === ' ')) {
                  event.preventDefault();
                  handleMarkRead(item.id);
                }
              }}
              role={item.read ? undefined : 'button'}
              tabIndex={item.read ? undefined : 0}
              aria-disabled={item.read || markingId === item.id}
            >
              <time className="notifications__time" dateTime={item.createdAt}>
                {formatNotificationTime(item.createdAt)}
              </time>
              <p className="notifications__message">{item.message}</p>
              {incidentIds.length > 0 && onNavigateToIncident && (
                <div className="notifications__actions">
                  {incidentIds.map((incidentId) => (
                    <button
                      key={incidentId}
                      type="button"
                      className="btn btn--outline btn--sm"
                      onClick={(event) => {
                        event.stopPropagation();
                        onNavigateToIncident(incidentId);
                      }}
                    >
                      Инцидент №{incidentId}
                    </button>
                  ))}
                </div>
              )}
            </li>
          );
        })}
      </ul>
    </div>
  );
}
