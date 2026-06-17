import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockMonitoringAlertList } from '../test/testData';
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
