import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockNotificationList } from '../test/testData';
import { NotificationsPanel } from './NotificationsPanel';

jest.mock('../api/client');

const listNotifications = api.listNotifications as jest.MockedFunction<typeof api.listNotifications>;
const getUnreadNotificationsCount = api.getUnreadNotificationsCount as jest.MockedFunction<
  typeof api.getUnreadNotificationsCount
>;
const markNotificationRead = api.markNotificationRead as jest.MockedFunction<
  typeof api.markNotificationRead
>;

describe('NotificationsPanel', () => {
  const onClose = jest.fn();
  const onUnreadChange = jest.fn();
  const onNavigateToIncident = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    getUnreadNotificationsCount.mockResolvedValue({ count: 1 });
    listNotifications.mockResolvedValue(
      mockNotificationList({
        items: [
          {
            id: 7,
            message: 'Новый инцидент',
            read: false,
            createdAt: '2025-06-01T12:00:00Z',
            relatedEntities: ['INCIDENT:42'],
          },
        ],
        totalElements: 1,
      }),
    );
  });

  it('does not render when closed', () => {
    const { container } = render(
      <NotificationsPanel
        token="tok"
        open={false}
        onClose={onClose}
        onUnreadChange={onUnreadChange}
      />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('loads notifications when open', async () => {
    render(
      <NotificationsPanel
        token="tok"
        open
        onClose={onClose}
        onUnreadChange={onUnreadChange}
        onNavigateToIncident={onNavigateToIncident}
      />,
    );

    await waitFor(() => {
      expect(listNotifications).toHaveBeenCalledWith('tok', 0, 20);
    });
    await waitFor(() => {
      expect(screen.getByText('Новый инцидент')).toBeInTheDocument();
    });
    expect(onUnreadChange).toHaveBeenCalledWith(1);
  });

  it('ignores click on already read notification', async () => {
    listNotifications.mockResolvedValue(
      mockNotificationList({
        items: [
          {
            id: 3,
            message: 'Прочитано',
            read: true,
            createdAt: '2025-06-01T12:00:00Z',
            relatedEntities: [],
          },
        ],
        totalElements: 1,
      }),
    );

    render(
      <NotificationsPanel
        token="tok"
        open
        onClose={onClose}
        onUnreadChange={onUnreadChange}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('Прочитано')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText('Прочитано'));
    expect(markNotificationRead).not.toHaveBeenCalled();
  });

  it('marks notification as read on click', async () => {
    markNotificationRead.mockResolvedValue(undefined);
    getUnreadNotificationsCount.mockResolvedValueOnce({ count: 1 }).mockResolvedValueOnce({ count: 0 });

    render(
      <NotificationsPanel
        token="tok"
        open
        onClose={onClose}
        onUnreadChange={onUnreadChange}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('Новый инцидент')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText('Новый инцидент'));

    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalledWith('tok', 7);
    });
    expect(onUnreadChange).toHaveBeenLastCalledWith(0);
  });

  it('navigates to incident from notification link', async () => {
    render(
      <NotificationsPanel
        token="tok"
        open
        onClose={onClose}
        onUnreadChange={onUnreadChange}
        onNavigateToIncident={onNavigateToIncident}
      />,
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Перейти к инциденту' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Перейти к инциденту' }));
    expect(onNavigateToIncident).toHaveBeenCalledWith(42);
  });

  it('shows error when loading fails', async () => {
    listNotifications.mockRejectedValueOnce(new Error('Load failed'));

    render(
      <NotificationsPanel
        token="tok"
        open
        onClose={onClose}
        onUnreadChange={onUnreadChange}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('Load failed')).toBeInTheDocument();
    });
  });

  it('shows error when mark read fails', async () => {
    markNotificationRead.mockRejectedValueOnce(new Error('Mark failed'));

    render(
      <NotificationsPanel
        token="tok"
        open
        onClose={onClose}
        onUnreadChange={onUnreadChange}
      />,
    );

    await waitFor(() => {
      expect(screen.getByText('Новый инцидент')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText('Новый инцидент'));

    await waitFor(() => {
      expect(screen.getByText('Mark failed')).toBeInTheDocument();
    });
  });

  it('closes on outside click', async () => {
    render(
      <div>
        <button type="button">Outside</button>
        <NotificationsPanel
          token="tok"
          open
          onClose={onClose}
          onUnreadChange={onUnreadChange}
        />
      </div>,
    );

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Уведомления' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Outside' }));
    expect(onClose).toHaveBeenCalled();
  });
});
