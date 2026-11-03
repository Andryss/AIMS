import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockTender, mockTenderParticipant } from '../test/testData';
import { TenderHistory } from './TenderHistory';

jest.mock('../api/client');
beforeEach(() => {
  jest.clearAllMocks();
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 2, login: 'manager' }] });
});

it('compares with the previous page and shows changed proposal and received links', async () => {
  const before = { id: 10, changedAt: '2026-11-03T12:00:00Z', changedByUserId: 2,
    snapshot: { tender: mockTender({ status: 'COLLECTING_OFFERS' }),
      participants: [mockTenderParticipant({ proposal: 'Неделя' })], receivedEquipmentIds: [] } };
  const after = { ...before, id: 11, snapshot: {
    tender: mockTender({ status: 'COMPLETED', winnerParticipantId: 7 }),
    participants: [mockTenderParticipant({ status: 'WINNER', proposal: 'Пять дней' })],
    receivedEquipmentIds: [10, 11], transitionComment: 'Принято полностью',
  } };
  (api.listTenderHistory as jest.Mock).mockImplementation(async (_token, _id, page) => ({
    items: [page === 0 ? before : after], page, size: 10, totalElements: 11, totalPages: 2,
  }));
  render(<MemoryRouter><TenderHistory token="tok" tenderId={4} revision={0} /></MemoryRouter>);
  await screen.findByText('Неделя');
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(await screen.findByText('Неделя → Пять дней')).toBeInTheDocument();
  expect(screen.queryByText('Количество оборудования:')).not.toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Оборудование #10' })).toHaveAttribute('href', '/equipment/10');
  expect(screen.getByText('Принято полностью')).toBeInTheDocument();
  expect(await screen.findByText('manager')).toBeInTheDocument();
});
