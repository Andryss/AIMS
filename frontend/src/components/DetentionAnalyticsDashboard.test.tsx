import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { analyticsMeta, analyticsPeriod, mockAnalyticsRequests } from '../test/analyticsTestData';
import { DetentionAnalyticsDashboard } from './DetentionAnalyticsDashboard';

jest.mock('../api/client');
beforeEach(() => { jest.clearAllMocks(); mockAnalyticsRequests(); });

it('uses historical deadlines, all status groups and actual duration percentiles', async () => {
  (api.getDetentionByStatus as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), groups: [
    { key: 'NEW', count: 0 }, { key: 'ACCEPTED', count: 1 }, { key: 'IN_CUSTODY', count: 1 },
    { key: 'RELEASED', count: 0 }, { key: 'REJECTED', count: 0 }, { key: 'CANCELLED', count: 0 },
  ] });
  (api.getDetentionCustodyDuration as jest.Mock).mockResolvedValue({ meta: analyticsMeta(),
    percentiles: { sampleCount: 1, excludedSampleCount: 0, p50Seconds: 30, p90Seconds: 30, p95Seconds: 30, p99Seconds: 30 } });
  (api.getDetentionUpcomingReleases as jest.Mock).mockResolvedValue({ meta: analyticsMeta(), totalCount: 12,
    items: [{ requestId: 7, subjectName: 'Субъект', incidentId: 8, alienId: 1, alienName: 'Слизень', plannedReleaseAt: '2025-05-03T00:00:00Z' }] });
  render(<MemoryRouter><DetentionAnalyticsDashboard token="tok" permissions={['DETENTION_READ', 'INCIDENT_READ', 'ALIEN_READ']}
    query={{ ...analyticsPeriod, status: 'IN_CUSTODY', alienId: 1, incidentId: 8 }} /></MemoryRouter>);
  expect(await screen.findByRole('link', { name: 'Заявка #7' })).toHaveAttribute('href', '/detention-requests/7');
  expect(screen.getByRole('link', { name: 'Инцидент #8' })).toHaveAttribute('href', '/incidents/8');
  expect(screen.getByRole('link', { name: 'Слизень' })).toHaveAttribute('href', '/aliens/1');
  expect(screen.getByText('Отменена')).toBeInTheDocument();
  expect(screen.getAllByText('30 с')).toHaveLength(4);
  expect(screen.getByText(/Показаны первые 10 из 12/)).toBeInTheDocument();
  expect(api.getDetentionCustodyDuration).toHaveBeenCalledWith('tok', { ...analyticsPeriod,
    status: 'IN_CUSTODY', alienId: 1, incidentId: 8 });
});
