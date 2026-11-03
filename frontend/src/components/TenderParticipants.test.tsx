import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockTender, mockTenderParticipant } from '../test/testData';
import { TenderParticipants } from './TenderParticipants';

jest.mock('../api/client');
beforeEach(() => {
  jest.clearAllMocks();
  (api.listTenderParticipants as jest.Mock).mockResolvedValue({ items: [mockTenderParticipant()],
    page: 0, size: 10, totalElements: 1, totalPages: 1 });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 2, login: 'manager' }] });
});

it('requires confirmation, blocks duplicate winner selection and preserves errors', async () => {
  (api.selectTenderWinner as jest.Mock).mockRejectedValue(new Error('Тендер уже изменён'));
  render(<TenderParticipants token="tok" tender={mockTender({ status: 'COLLECTING_OFFERS' })}
    canManage onChanged={jest.fn()} />);
  expect(await screen.findByText('ООО Орбита')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Выбрать победителя' }));
  expect(api.selectTenderWinner).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Тендер уже изменён');
  expect(api.selectTenderWinner).toHaveBeenCalledWith('tok', 4, { participantId: 7 });
});

it.each(['NEED', 'AWAITING_DELIVERY', 'COMPLETED', 'CANCELLED'] as const)
('does not offer mutations in %s', async (status) => {
  render(<TenderParticipants token="tok" tender={mockTender({ status })} canManage onChanged={jest.fn()} />);
  await screen.findByText('ООО Орбита');
  expect(screen.queryByRole('button', { name: 'Выбрать победителя' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Добавить участника' })).not.toBeInTheDocument();
});

it('paginates offers without granting a reader mutation controls', async () => {
  (api.listTenderParticipants as jest.Mock).mockResolvedValue({ items: [mockTenderParticipant()],
    page: 0, size: 10, totalElements: 11, totalPages: 2 });
  render(<TenderParticipants token="tok" tender={mockTender({ status: 'COLLECTING_OFFERS' })}
    canManage={false} onChanged={jest.fn()} />);
  await screen.findByText('ООО Орбита');
  expect(screen.queryByRole('button', { name: 'Выбрать победителя' })).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  await waitFor(() => expect(api.listTenderParticipants).toHaveBeenLastCalledWith('tok', 4, 1, 10));
});


it('does not offer old-page winner actions after the next page fails', async () => {
  (api.listTenderParticipants as jest.Mock).mockResolvedValueOnce({ items: [mockTenderParticipant()],
    page: 0, size: 10, totalElements: 11, totalPages: 2 }).mockRejectedValue(new Error('Нет соединения'));
  render(<TenderParticipants token="tok" tender={mockTender({ status: 'COLLECTING_OFFERS' })}
    canManage onChanged={jest.fn()} />);
  await screen.findByText('ООО Орбита');
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Нет соединения');
  expect(screen.queryByText('ООО Орбита')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Выбрать победителя' })).not.toBeInTheDocument();
});

it('shows only the empty state when there are no offers', async () => {
  (api.listTenderParticipants as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10,
    totalElements: 0, totalPages: 0 });
  render(<TenderParticipants token="tok" tender={mockTender({ status: 'COLLECTING_OFFERS' })}
    canManage onChanged={jest.fn()} />);
  await screen.findByText('Предложений пока нет');
  const section = within(screen.getByRole('region', { name: 'Участники тендера' }));
  expect(section.queryByRole('table')).not.toBeInTheDocument();
  expect(section.getByRole('button', { name: 'Добавить участника' })).toBeInTheDocument();
});
