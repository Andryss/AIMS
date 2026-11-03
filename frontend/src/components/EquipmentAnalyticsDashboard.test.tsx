import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { analyticsMeta, analyticsPeriod, mockAnalyticsRequests } from '../test/analyticsTestData';
import { EquipmentAnalyticsDashboard } from './EquipmentAnalyticsDashboard';

jest.mock('../api/client');
beforeEach(() => { jest.clearAllMocks(); mockAnalyticsRequests(); });

it('shows inventory without activity and keeps global stock independent of the movement filters', async () => {
  render(<MemoryRouter><EquipmentAnalyticsDashboard token="tok" permissions={['INCIDENT_READ', 'USER_READ']}
    query={{ ...analyticsPeriod, responsibleUserId: 3, incidentId: 8 }} /></MemoryRouter>);
  expect(await screen.findByText('Нейралайзер')).toBeInTheDocument();
  expect(screen.getByText('—')).toBeInTheDocument();
  expect(screen.getByText('За период событий нет')).toBeInTheDocument();
  expect(api.getEquipmentInventoryByType).toHaveBeenCalledWith('tok', analyticsPeriod);
  expect(api.getEquipmentIssuedCount).toHaveBeenCalledWith('tok', { ...analyticsPeriod, responsibleUserId: 3, incidentId: 8 });
});

it('renders unique request totals, deliveries and outstanding groups with employee avatars', async () => {
  (api.getEquipmentOpenRequests as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), totalCount: 1,
    items: [{ equipmentType: 'NEURALYZER', operation: 'ISSUE', requestCount: 1, unitCount: 2 }] });
  (api.getEquipmentExpectedDeliveriesByType as jest.Mock).mockResolvedValue({ meta: analyticsMeta(),
    items: [{ equipmentType: 'NEUTRALIZER', tenderCount: 2, unitCount: 5 }] });
  (api.getEquipmentOutstandingByIncident as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), totalCount: 12,
    items: [{ incidentId: 8, location: 'Area 51', responsibleUserId: 3, unitsByType: { NEURALYZER: 2 } }] });
  render(<MemoryRouter><EquipmentAnalyticsDashboard token="tok" permissions={['INCIDENT_READ', 'USER_READ']}
    query={analyticsPeriod} /></MemoryRouter>);
  expect(await screen.findByRole('link', { name: 'Инцидент #8' })).toHaveAttribute('href', '/incidents/8');
  expect(await screen.findByText('agent')).toBeInTheDocument();
  expect(screen.getByText('Выдача')).toBeInTheDocument();
  expect(screen.getByText(/Показаны первые 10 из 12/)).toBeInTheDocument();
  await waitFor(() => expect(api.batchUsers).toHaveBeenCalledWith('tok', [3]));
});
