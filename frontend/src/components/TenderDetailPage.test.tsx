import React from 'react';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockEquipment, mockTender } from '../test/testData';
import { TenderDetailPage } from './TenderDetailPage';

jest.mock('../api/client');
const permissions = ['TENDER_READ', 'TENDER_CREATE', 'TENDER_EDIT', 'TENDER_MANAGE', 'TENDER_RECEIVE'];
beforeEach(() => {
  jest.clearAllMocks();
  (api.getTender as jest.Mock).mockImplementation(async (_token, id) => mockTender({ id }));
  (api.listTenderParticipants as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10,
    totalElements: 0, totalPages: 0 });
  (api.listTenderHistory as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10,
    totalElements: 0, totalPages: 0 });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [] });
});

function open(path = '/tenders/4', granted = permissions) {
  render(<MemoryRouter initialEntries={[path]}><Link to="/tenders/5">Другой тендер</Link><Routes>
    <Route path="/tenders/:id" element={<TenderDetailPage token="tok" permissions={granted} currentUserId={2} />} />
  </Routes></MemoryRouter>);
}

it('retains the list query, shows the history tab and guards editing by ownership', async () => {
  open('/tenders/4?q=неделя&page=1', ['TENDER_READ', 'TENDER_EDIT']);
  await screen.findByRole('heading', { name: 'Тендер #4' });
  expect(screen.getByRole('link', { name: /К списку/ })).toHaveAttribute('href', '/tenders?q=неделя&page=1');
  expect(screen.getByRole('button', { name: 'Редактировать тендер' })).toBeInTheDocument();
  expect(screen.getByRole('tab', { name: 'История' })).toHaveAttribute('aria-selected', 'true');
});

it('ignores a delayed GET after changing the card', async () => {
  let resolve!: (value: ReturnType<typeof mockTender>) => void;
  (api.getTender as jest.Mock).mockImplementationOnce(() => new Promise((done) => { resolve = done; }))
    .mockResolvedValue(mockTender({ id: 5 }));
  open();
  await userEvent.click(screen.getByRole('link', { name: 'Другой тендер' }));
  await screen.findByRole('heading', { name: 'Тендер #5' });
  await act(async () => resolve(mockTender()));
  expect(screen.queryByRole('heading', { name: 'Тендер #4' })).not.toBeInTheDocument();
});

it('ignores a save from a previous card after navigation', async () => {
  let resolve!: (value: ReturnType<typeof mockTender>) => void;
  (api.updateTender as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  open();
  await userEvent.click(await screen.findByRole('button', { name: 'Редактировать тендер' }));
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await userEvent.click(screen.getByRole('link', { name: 'Другой тендер' }));
  await screen.findByRole('heading', { name: 'Тендер #5' });
  await act(async () => resolve(mockTender({ conditions: 'Старое сохранение' })));
  expect(screen.getByRole('heading', { name: 'Тендер #5' })).toBeInTheDocument();
  expect(screen.queryByText('Старое сохранение')).not.toBeInTheDocument();
});

it('receives the delivery, refreshes the card/history and links new units', async () => {
  (api.getTender as jest.Mock).mockResolvedValue(mockTender({ status: 'AWAITING_DELIVERY', winnerParticipantId: 7 }));
  (api.receiveTender as jest.Mock).mockResolvedValue({ tender: mockTender({ status: 'COMPLETED', winnerParticipantId: 7 }),
    equipment: [mockEquipment({ id: 10 }), mockEquipment({ id: 11 })] });
  open();
  await userEvent.click(await screen.findByRole('button', { name: 'Принять поставку' }));
  fireEvent.change(screen.getByLabelText('Модель 1'), { target: { value: 'A' } });
  fireEvent.change(screen.getByLabelText('Модель 2'), { target: { value: 'B' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 1'), { target: { value: '1' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 2'), { target: { value: '2' } });
  await userEvent.click(within(screen.getByRole('dialog')).getByRole('button', { name: 'Принять поставку' }));
  expect(await screen.findByText('Завершён')).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Оборудование #10' })).toHaveAttribute('href', '/equipment/10');
  expect(screen.queryByRole('button', { name: 'Редактировать тендер' })).not.toBeInTheDocument();
});

it.each(['0', 'abc', '1.5'])('rejects invalid tender id %s', async (id) => {
  open(`/tenders/${id}`);
  expect(await screen.findByRole('alert')).toHaveTextContent('Некорректный идентификатор тендера');
  expect(api.getTender).not.toHaveBeenCalled();
});

it('ignores an old winner selection response after changing tender cards', async () => {
  let resolve!: (value: ReturnType<typeof mockTender>) => void;
  (api.getTender as jest.Mock).mockImplementation(async (_token, id) => mockTender({ id, status: 'COLLECTING_OFFERS' }));
  (api.listTenderParticipants as jest.Mock).mockResolvedValue({ items: [{ id: 7, tenderId: 4,
    name: 'ООО Орбита', proposal: 'Неделя', status: 'PARTICIPATING', createdByUserId: 2,
    createdAt: '2026-11-03T12:00:00Z', updatedAt: '2026-11-03T12:00:00Z' }],
    page: 0, size: 10, totalElements: 1, totalPages: 1 });
  (api.selectTenderWinner as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  open();
  await userEvent.click(await screen.findByRole('button', { name: 'Выбрать победителя' }));
  const confirm = screen.getByRole('button', { name: 'Подтвердить' });
  fireEvent.click(confirm);
  fireEvent.click(confirm);
  expect(api.selectTenderWinner).toHaveBeenCalledTimes(1);
  await userEvent.click(screen.getByRole('link', { name: 'Другой тендер' }));
  await screen.findByRole('heading', { name: 'Тендер #5' });
  await act(async () => resolve(mockTender({ status: 'AWAITING_DELIVERY', winnerParticipantId: 7 })));
  expect(screen.getByRole('heading', { name: 'Тендер #5' })).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Принять поставку' })).not.toBeInTheDocument();
});


it('freezes purchase fields immediately after the first offer even if refetch fails', async () => {
  (api.getTender as jest.Mock).mockResolvedValueOnce(mockTender({ status: 'COLLECTING_OFFERS' }))
    .mockImplementation(() => new Promise(() => {}));
  (api.listTenderParticipants as jest.Mock).mockResolvedValueOnce({ items: [], page: 0, size: 10,
    totalElements: 0, totalPages: 0 }).mockRejectedValue(new Error('Нет соединения'));
  (api.createTenderParticipant as jest.Mock).mockResolvedValue({ id: 7, tenderId: 4, name: 'Орбита', proposal: 'Неделя' });
  open();
  await userEvent.click(await screen.findByRole('button', { name: 'Добавить участника' }));
  fireEvent.change(screen.getByLabelText(/Организация/), { target: { value: 'Орбита' } });
  fireEvent.change(screen.getByLabelText(/Предложение/), { target: { value: 'Неделя' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await screen.findByRole('alert');
  await userEvent.click(screen.getByRole('button', { name: 'Редактировать тендер' }));
  expect(screen.getByLabelText(/Тип оборудования/)).toBeDisabled();
  expect(screen.getByLabelText(/Количество оборудования/)).toBeDisabled();
});

it('groups tender actions in the header and labels the status row', async () => {
  (api.getTender as jest.Mock).mockResolvedValue(mockTender({ status: 'AWAITING_DELIVERY', winnerParticipantId: 7 }));
  open();
  await screen.findByRole('heading', { name: 'Тендер #4' });
  const actions = within(screen.getByRole('group', { name: 'Действия тендера' }));
  expect(actions.getByRole('button', { name: 'Принять поставку' })).toBeInTheDocument();
  expect(actions.getByRole('link', { name: /К списку/ })).toBeInTheDocument();
  expect(screen.getByText('Статус', { exact: true }).tagName).toBe('DT');
});
