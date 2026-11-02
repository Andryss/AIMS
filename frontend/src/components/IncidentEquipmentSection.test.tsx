import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockEquipment, mockEquipmentMovement } from '../test/testData';
import { IncidentEquipmentSection } from './IncidentEquipmentSection';

jest.mock('../api/client');
beforeEach(() => {
  jest.clearAllMocks();
  (api.listEquipmentMovements as jest.Mock).mockResolvedValue({ items: [mockEquipmentMovement({
    status: 'COMPLETED', equipment: [mockEquipment({ status: 'ISSUED', holderUserId: 3, currentIssueMovementId: 7 })],
  })], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [] });
  (api.listEquipment as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
});

it('shows the request list without obligation counters', async () => {
  render(<MemoryRouter><IncidentEquipmentSection token="tok" incidentId={1} incidentStatus="EXECUTING"
    currentUserId={1} canRead canCreate /></MemoryRouter>);
  expect(await screen.findByRole('link', { name: 'Заявка #7' })).toHaveAttribute('href', '/equipment-movements/7');
  expect(screen.queryByText(/Открытых заявок:|Не возвращено:/)).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Заявка на выдачу' }));
  expect(await screen.findByRole('dialog', { name: 'Заявка на выдачу' })).toBeInTheDocument();
});

it.each([true, false])('requires create permission and an active incident (create=%s)', async (canCreate) => {
  render(<MemoryRouter><IncidentEquipmentSection token="tok" incidentId={1} incidentStatus="EXECUTION_COMPLETED"
    canRead canCreate={canCreate} /></MemoryRouter>);
  await screen.findByRole('link', { name: 'Заявка #7' });
  expect(screen.queryByRole('button', { name: 'Заявка на выдачу' })).not.toBeInTheDocument();
});

it('paginates incident requests through the server', async () => {
  (api.listEquipmentMovements as jest.Mock).mockImplementation(async (_token, { page }) => ({
    items: [mockEquipmentMovement({ id: page === 0 ? 7 : 8 })], page, size: 10, totalElements: 11, totalPages: 2,
  }));
  render(<MemoryRouter><IncidentEquipmentSection token="tok" incidentId={1} incidentStatus="EXECUTING"
    canRead canCreate /></MemoryRouter>);
  await screen.findByRole('link', { name: 'Заявка #7' });
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(await screen.findByRole('link', { name: 'Заявка #8' })).toBeInTheDocument();
  expect(screen.queryByRole('link', { name: 'Заявка #7' })).not.toBeInTheDocument();
});
