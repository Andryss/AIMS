import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockMonitoringAlert, mockMonitoringAlertList } from '../test/testData';
import { MonitoringAlertsTab } from './MonitoringAlertsTab';

jest.mock('../api/client');

const listMonitoringAlerts = api.listMonitoringAlerts as jest.MockedFunction<
  typeof api.listMonitoringAlerts
>;

describe('MonitoringAlertsTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    listMonitoringAlerts.mockResolvedValue(mockMonitoringAlertList());
  });

  it('shows empty state when there are no alerts', async () => {
    listMonitoringAlerts.mockResolvedValue(
      mockMonitoringAlertList({ items: [], totalElements: 0, totalPages: 0 }),
    );

    render(
      <MemoryRouter>
        <MonitoringAlertsTab token="tok" canCreate={false} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Алерты не найдены')).toBeInTheDocument();
  });

  it('shows error when loading fails', async () => {
    listMonitoringAlerts.mockRejectedValue(new Error('Сеть недоступна'));

    render(
      <MemoryRouter>
        <MonitoringAlertsTab token="tok" canCreate={false} />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Сеть недоступна');
  });

  it('shows generic error for non-Error rejections', async () => {
    listMonitoringAlerts.mockRejectedValue('boom');

    render(
      <MemoryRouter>
        <MonitoringAlertsTab token="tok" canCreate={false} />
      </MemoryRouter>,
    );

    expect(await screen.findByRole('alert')).toHaveTextContent('Не удалось загрузить алерты');
  });

  it('navigates between pages', async () => {
    listMonitoringAlerts
      .mockResolvedValueOnce(
        mockMonitoringAlertList({
          items: [mockMonitoringAlert({ id: 1, location: 'Page one' })],
          page: 0,
          totalElements: 24,
          totalPages: 2,
        }),
      )
      .mockResolvedValueOnce(
        mockMonitoringAlertList({
          items: [mockMonitoringAlert({ id: 2, location: 'Page two' })],
          page: 1,
          totalElements: 24,
          totalPages: 2,
        }),
      )
      .mockResolvedValueOnce(
        mockMonitoringAlertList({
          items: [mockMonitoringAlert({ id: 1, location: 'Page one again' })],
          page: 0,
          totalElements: 24,
          totalPages: 2,
        }),
      );

    render(
      <MemoryRouter>
        <MonitoringAlertsTab token="tok" canCreate={false} />
      </MemoryRouter>,
    );

    expect(await screen.findByText('Page one')).toBeInTheDocument();
    const next = screen.getByRole('button', { name: /Следующая/i });
    const prev = screen.getByRole('button', { name: /Предыдущая/i });
    expect(prev).toBeDisabled();

    await userEvent.click(next);
    expect(await screen.findByText('Page two')).toBeInTheDocument();
    expect(listMonitoringAlerts).toHaveBeenLastCalledWith('tok', 1, 12);

    await userEvent.click(screen.getByRole('button', { name: /Предыдущая/i }));
    expect(await screen.findByText('Page one again')).toBeInTheDocument();
    expect(listMonitoringAlerts).toHaveBeenLastCalledWith('tok', 0, 12);
  });

  it('loads and displays alert cards with media links', async () => {
    render(
      <MemoryRouter>
        <MonitoringAlertsTab token="tok" canCreate />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(listMonitoringAlerts).toHaveBeenCalledWith('tok', 0, 12);
    });

    expect(screen.getByRole('heading', { name: 'Входящие алерты' })).toBeInTheDocument();
    expect(await screen.findByText('Nevada desert sector 7')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /photo1\.jpg/i })).toHaveAttribute(
      'href',
      'https://example.com/evidence/photo1.jpg',
    );
    expect(screen.getByRole('button', { name: /Зарегистрировать инцидент/i })).toBeInTheDocument();
  });

  it('opens create incident modal from alert card', async () => {
    render(
      <MemoryRouter>
        <MonitoringAlertsTab token="tok" canCreate />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Nevada desert sector 7')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /Зарегистрировать инцидент/i }));
    const dialog = screen.getByRole('dialog');
    expect(
      within(dialog).getByRole('heading', { name: 'Зарегистрировать инцидент по алерту' }),
    ).toBeInTheDocument();
    expect(within(dialog).getByRole('link', { name: /photo1\.jpg/i })).toBeInTheDocument();
  });
});
