import React from 'react';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockEquipment, mockEquipmentMovement } from '../test/testData';
import { EquipmentHistoryEntry, EquipmentMovementHistoryEntry } from '../types';
import { EquipmentHistory } from './EquipmentHistory';
import { EquipmentMovementHistory } from './EquipmentMovementHistory';

jest.mock('../api/client');
const listHistory = api.listEquipmentHistory as jest.MockedFunction<typeof api.listEquipmentHistory>;
const listMovementHistory = api.listEquipmentMovementHistory as jest.MockedFunction<typeof api.listEquipmentMovementHistory>;
const registered: EquipmentHistoryEntry = { id: 1, changedAt: '2026-11-02T10:00:00Z', changedByUserId: 5,
  snapshot: { event: 'REGISTERED', equipment: mockEquipment() } };
const created: EquipmentMovementHistoryEntry = { id: 1, changedAt: '2026-11-02T10:00:00Z', changedByUserId: 5,
  snapshot: { movement: mockEquipmentMovement() } };

beforeEach(() => {
  jest.clearAllMocks();
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [
    { id: 3, login: 'agent' }, { id: 4, login: 'agent2' }, { id: 5, login: 'storekeeper' },
  ] });
});

it('shows only changed equipment fields as old and new values', async () => {
  const updated: EquipmentHistoryEntry = { ...registered, id: 2, snapshot: { event: 'UPDATED',
    equipment: mockEquipment({ model: 'Нейралайзер Н-2', serialNumber: 'SN-002' }) } };
  listHistory.mockResolvedValue({ items: [registered, updated], page: 0, size: 10, totalElements: 2, totalPages: 1 });
  render(<MemoryRouter><EquipmentHistory token="tok" equipmentId={1} canReadMovements /></MemoryRouter>);
  const entries = await screen.findAllByRole('listitem');
  const update = entries.find((entry) => entry.textContent?.includes('Изменены реквизиты'))!;
  expect(within(update).getByText(/Нейралайзер Н-1 → Нейралайзер Н-2/)).toBeInTheDocument();
  expect(within(update).getByText(/SN-001 → SN-002/)).toBeInTheDocument();
  expect(within(update).queryByText('Тип:')).not.toBeInTheDocument();
  expect(within(update).queryByText('Состояние:')).not.toBeInTheDocument();
});

it('compares the first equipment entry of a later page with its preceding snapshot', async () => {
  const issued: EquipmentHistoryEntry = { ...registered, id: 10, snapshot: { event: 'ISSUED', movementId: 7,
    equipment: mockEquipment({ status: 'ISSUED', holderUserId: 3, currentIssueMovementId: 7 }) } };
  const returned: EquipmentHistoryEntry = { ...registered, id: 11, snapshot: { ...registered.snapshot,
    event: 'RETURNED', movementId: 8 } };
  listHistory.mockImplementation(async (_token, _id, page) => ({ items: [page === 0 ? issued : returned],
    page: page ?? 0, size: 10, totalElements: 11, totalPages: 2 }));
  render(<MemoryRouter><EquipmentHistory token="tok" equipmentId={1} canReadMovements /></MemoryRouter>);
  await screen.findByText('Выдача сотруднику');
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  await screen.findByText('Возврат на склад');
  expect(screen.getByText('Выдано → На складе')).toBeInTheDocument();
  expect(screen.getByRole('img', { name: 'Аватар agent' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Заявка #7' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Заявка #8' })).toBeInTheDocument();
  expect(screen.queryByText('Серийный номер:')).not.toBeInTheDocument();
});

it('shows request changes without repeating unchanged fields or equipment metadata', async () => {
  const updated: EquipmentMovementHistoryEntry = { ...created, id: 2, snapshot: {
    movement: mockEquipmentMovement({ responsibleUserId: 4, comment: 'Новый комментарий',
      equipment: [mockEquipment({ id: 2, serialNumber: 'SN-002' })] }),
  } };
  const processed: EquipmentMovementHistoryEntry = { ...updated, id: 3, snapshot: {
    movement: { ...updated.snapshot.movement, status: 'PROCESSING' },
  } };
  const rejected: EquipmentMovementHistoryEntry = { ...processed, id: 4, snapshot: {
    movement: { ...processed.snapshot.movement, status: 'REJECTED' }, transitionComment: 'Выдача не нужна',
  } };
  listMovementHistory.mockResolvedValue({ items: [created, updated, processed, rejected],
    page: 0, size: 10, totalElements: 4, totalPages: 1 });
  const { container } = render(<MemoryRouter><EquipmentMovementHistory token="tok" movementId={7} revision={0} /></MemoryRouter>);
  await screen.findByText('В ожидании → Обрабатывается');
  const entries = container.querySelectorAll('.history-timeline__item');
  expect(within(entries[1] as HTMLElement).getByText('Обследование склада → Новый комментарий')).toBeInTheDocument();
  expect(await within(entries[1] as HTMLElement).findByRole('img', { name: 'Аватар agent' })).toBeInTheDocument();
  expect(within(entries[1] as HTMLElement).getByRole('img', { name: 'Аватар agent2' })).toBeInTheDocument();
  expect(within(entries[1] as HTMLElement).getByText(/SN-001/)).toBeInTheDocument();
  expect(within(entries[1] as HTMLElement).getByText(/SN-002/)).toBeInTheDocument();
  expect(within(entries[2] as HTMLElement).queryByText('Оборудование:')).not.toBeInTheDocument();
  expect(within(entries[2] as HTMLElement).queryByText('Ответственный:')).not.toBeInTheDocument();
  expect(within(entries[3] as HTMLElement).getByText('Причина: Выдача не нужна')).toBeInTheDocument();
});

it('uses the previous page when comparing request history statuses', async () => {
  listMovementHistory.mockImplementation(async (_token, _id, page) => ({
    items: [{ ...created, id: page === 0 ? 10 : 11, snapshot: {
      movement: mockEquipmentMovement({ status: page === 0 ? 'PROCESSING' : 'COMPLETED' }),
    } }], page: page ?? 0, size: 10, totalElements: 11, totalPages: 2,
  }));
  render(<MemoryRouter><EquipmentMovementHistory token="tok" movementId={7} revision={0} /></MemoryRouter>);
  await screen.findByText('Обрабатывается');
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(await screen.findByText('Обрабатывается → Выполнена')).toBeInTheDocument();
  expect(screen.queryByText('Операция:')).not.toBeInTheDocument();
});


it.each([true, false])('renders receipt provenance with tender permission=%s', async (canReadTenders) => {
  listHistory.mockResolvedValue({ items: [{ ...created, snapshot: { event: 'RECEIVED',
    equipment: mockEquipment({ sourceTenderId: 6 }) } }], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  render(<MemoryRouter><EquipmentHistory token="tok" equipmentId={1} canReadMovements
    canReadTenders={canReadTenders} /></MemoryRouter>);
  expect(await screen.findByText('Поступление по тендеру')).toBeInTheDocument();
  if (canReadTenders) {
    expect(screen.getByRole('link', { name: 'Тендер #6' })).toHaveAttribute('href', '/tenders/6');
  } else {
    expect(screen.getByText('Тендер #6')).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'Тендер #6' })).not.toBeInTheDocument();
  }
});

it('treats a missing source in old snapshots and an explicit null as the same value', async () => {
  listHistory.mockResolvedValue({ items: [registered, { ...registered, id: 2, snapshot: { event: 'UPDATED',
    equipment: mockEquipment({ model: 'Новое название', sourceTenderId: null }) } }],
    page: 0, size: 10, totalElements: 2, totalPages: 1 });
  render(<MemoryRouter><EquipmentHistory token="tok" equipmentId={1} canReadMovements /></MemoryRouter>);
  const changed = await screen.findByText('Изменены реквизиты');
  expect(within(changed.closest('li')!).queryByText('Тендер-основание:')).not.toBeInTheDocument();
});
