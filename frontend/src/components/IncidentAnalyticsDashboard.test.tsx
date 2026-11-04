import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { analyticsMeta, analyticsPeriod, mockAnalyticsRequests } from '../test/analyticsTestData';
import { IncidentAnalyticsDashboard } from './IncidentAnalyticsDashboard';

jest.mock('../api/client');
beforeEach(() => { jest.clearAllMocks(); mockAnalyticsRequests(); });

it('shows separate threat and victim distributions without species links', async () => {
  (api.getIncidentByThreatLevel as jest.Mock).mockResolvedValue({ meta: analyticsMeta(),
    groups: [{ key: '1', count: 0 }, { key: '10', count: 2 }, { key: 'UNKNOWN', count: 3 }] });
  (api.getIncidentByVictimCount as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), groups: [
    { key: '0', count: 1 }, { key: '1', count: 2 }, { key: '2', count: 0 }, { key: '3', count: 0 },
    { key: '4', count: 0 }, { key: '5', count: 0 }, { key: '6', count: 0 }, { key: '7', count: 0 },
    { key: '8', count: 0 }, { key: '9+', count: 3 }, { key: 'UNKNOWN', count: 2 },
  ] });
  render(<MemoryRouter><IncidentAnalyticsDashboard token="tok" query={analyticsPeriod}
    permissions={['ALIEN_READ']} /></MemoryRouter>);

  const threats = await screen.findByRole('region', { name: 'По уровню угрозы' });
  const victims = screen.getByRole('region', { name: 'По количеству пострадавших' });
  expect(await within(threats).findByText('Не определён')).toBeInTheDocument();
  expect(await within(victims).findByText('Неизвестно')).toBeInTheDocument();
  expect(within(victims).getAllByRole('listitem').map((row) => row.textContent))
    .toEqual(['01', '12', '20', '30', '40', '50', '60', '70', '80', '9+3', 'Неизвестно2']);
  expect(within(threats).getAllByRole('listitem')).toHaveLength(3);
  expect(within(threats).queryByRole('link')).not.toBeInTheDocument();
});

it('updates both distributions when the period or incident filters change', async () => {
  const { rerender } = render(<MemoryRouter><IncidentAnalyticsDashboard token="tok" query={analyticsPeriod}
    permissions={[]} /></MemoryRouter>);
  await within(screen.getByRole('region', { name: 'По уровню угрозы' })).findByText('Не определён');
  expect(api.getIncidentByThreatLevel).toHaveBeenCalledWith('tok', analyticsPeriod);
  expect(api.getIncidentByVictimCount).toHaveBeenCalledWith('tok', analyticsPeriod);

  const query = { ...analyticsPeriod, to: '2025-05-03T00:00:00Z', eventType: 'ATTACK_ON_PEOPLE' as const,
    alienId: 6, responsibleUserId: 3 };
  rerender(<MemoryRouter><IncidentAnalyticsDashboard token="tok" query={query} permissions={[]} /></MemoryRouter>);
  await waitFor(() => expect(api.getIncidentByThreatLevel).toHaveBeenLastCalledWith('tok', query));
  expect(api.getIncidentByVictimCount).toHaveBeenLastCalledWith('tok', query);
});

it('retries a failed distribution without reloading the other widgets', async () => {
  (api.getIncidentByThreatLevel as jest.Mock).mockRejectedValueOnce(new Error('Нет связи'))
    .mockResolvedValue({ meta: analyticsMeta(), groups: [{ key: '7', count: 2 }] });
  render(<MemoryRouter><IncidentAnalyticsDashboard token="tok" query={analyticsPeriod}
    permissions={[]} /></MemoryRouter>);
  const threats = screen.getByRole('region', { name: 'По уровню угрозы' });
  const victims = screen.getByRole('region', { name: 'По количеству пострадавших' });
  expect(await within(threats).findByRole('alert')).toHaveTextContent('Нет связи');
  expect(await within(victims).findByText('Неизвестно')).toBeInTheDocument();
  fireEvent.click(within(threats).getByRole('button', { name: 'Повторить: По уровню угрозы' }));
  expect(await within(threats).findByText('7')).toBeInTheDocument();
  expect(api.getIncidentByVictimCount).toHaveBeenCalledTimes(1);
});
