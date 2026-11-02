import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { EquipmentListResponse } from '../types';
import { mockEquipment } from '../test/testData';
import { EquipmentTab } from './EquipmentTab';
import { EquipmentDetailPage } from './EquipmentDetailPage';

jest.mock('../api/client');
const listEquipment = api.listEquipment as jest.MockedFunction<typeof api.listEquipment>;
const getEquipment = api.getEquipment as jest.MockedFunction<typeof api.getEquipment>;
const listHistory = api.listEquipmentHistory as jest.MockedFunction<typeof api.listEquipmentHistory>;
const page: EquipmentListResponse = { items: [mockEquipment()], page: 0, size: 10, totalElements: 1, totalPages: 1 };

function renderAt(path = '/equipment', canWrite = true) {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/equipment" element={<EquipmentTab token="tok" canWrite={canWrite} />} />
    <Route path="/equipment/:id" element={<EquipmentDetailPage token="tok" canWrite={canWrite}
      canReadMovements />} />
  </Routes></MemoryRouter>);
}

beforeEach(() => {
  jest.clearAllMocks();
  listEquipment.mockResolvedValue(page);
  getEquipment.mockResolvedValue(mockEquipment());
  listHistory.mockResolvedValue({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 3, login: 'agent' }] });
});

it('opens the item card and preserves search/filters/page when returning', async () => {
  listEquipment.mockResolvedValue({ ...page, page: 1, totalElements: 12, totalPages: 2 });
  renderAt('/equipment?q=SN&type=NEURALYZER&status=AVAILABLE&page=1');
  await userEvent.click(await screen.findByRole('link', { name: 'Нейралайзер Н-1' }));
  expect(await screen.findByRole('heading', { name: 'Нейралайзер Н-1' })).toBeInTheDocument();
  expect(screen.getByText('SN-001')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('link', { name: /К списку/ }));
  await screen.findByRole('link', { name: 'Нейралайзер Н-1' });
  expect(listEquipment).toHaveBeenLastCalledWith('tok', expect.objectContaining({
    q: 'SN', type: 'NEURALYZER', status: 'AVAILABLE', page: 1, size: 10,
  }));
});

it('resets page after search and filter changes', async () => {
  renderAt('/equipment?page=1');
  await screen.findByRole('link', { name: 'Нейралайзер Н-1' });
  fireEvent.change(screen.getByLabelText('Поиск оборудования'), { target: { value: '  SN-2  ' } });
  await waitFor(() => expect(listEquipment).toHaveBeenLastCalledWith('tok', expect.objectContaining({
    q: 'SN-2', page: 0,
  })));
  fireEvent.change(screen.getByLabelText('Состояние'), { target: { value: 'ISSUED' } });
  await waitFor(() => expect(listEquipment).toHaveBeenLastCalledWith('tok', expect.objectContaining({ status: 'ISSUED' })));
});

it('ignores unsupported enum filters in the URL', async () => {
  renderAt('/equipment?type=constructor&status=toString');
  await screen.findByRole('link', { name: 'Нейралайзер Н-1' });
  expect(listEquipment).toHaveBeenCalledWith('tok', expect.objectContaining({ type: undefined, status: undefined }));
});

it('ignores a stale result for an older search', async () => {
  let resolveOld!: (value: EquipmentListResponse) => void;
  listEquipment.mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }))
    .mockResolvedValue({ ...page, items: [mockEquipment({ model: 'Нейтрализатор' })] });
  renderAt();
  fireEvent.change(screen.getByLabelText('Поиск оборудования'), { target: { value: 'новый' } });
  await screen.findByRole('link', { name: 'Нейтрализатор' });
  await act(async () => resolveOld(page));
  expect(screen.queryByRole('link', { name: 'Нейралайзер Н-1' })).not.toBeInTheDocument();
});

it.each([true, false])('shows create/edit controls only for canWrite=%s', async (canWrite) => {
  renderAt('/equipment', canWrite);
  await userEvent.click(await screen.findByRole('link', { name: 'Нейралайзер Н-1' }));
  await screen.findByRole('heading', { name: 'Нейралайзер Н-1' });
  expect(Boolean(screen.queryByRole('button', { name: 'Редактировать оборудование' }))).toBe(canWrite);
  await userEvent.click(screen.getByRole('link', { name: /К списку/ }));
  expect(Boolean(await screen.queryByRole('button', { name: 'Добавить оборудование' }))).toBe(canWrite);
});

it('loads the next history page with movement and actor information', async () => {
  const entry = { id: 1, changedAt: '2026-11-02T12:00:00Z', changedByUserId: 3,
    snapshot: { event: 'ISSUED' as const, movementId: 7,
      equipment: mockEquipment({ status: 'ISSUED', holderUserId: 3, currentIssueMovementId: 7 }) } };
  listHistory.mockResolvedValueOnce({ items: [entry], page: 0, size: 10, totalElements: 11, totalPages: 2 })
    .mockResolvedValue({ items: [{ ...entry, id: 2, snapshot: { ...entry.snapshot, event: 'RETURNED' } }],
      page: 1, size: 10, totalElements: 11, totalPages: 2 });
  renderAt('/equipment/1');
  expect((await screen.findAllByRole('link', { name: /Заявка #7/ }))[0]).toHaveAttribute('href', '/equipment-movements/7');
  expect(await screen.findByRole('tab', { name: 'История' })).toHaveAttribute('aria-selected', 'true');
  expect(await screen.findAllByText('agent')).not.toHaveLength(0);
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(await screen.findByText('Возврат на склад')).toBeInTheDocument();
  expect(listHistory).toHaveBeenCalledWith('tok', 1, 1, 10);
});

it.each(['0', 'abc', '1.5'])('rejects invalid equipment id %s', async (id) => {
  renderAt(`/equipment/${id}`);
  expect(await screen.findByRole('alert')).toHaveTextContent('Некорректный идентификатор оборудования');
  expect(getEquipment).not.toHaveBeenCalled();
});

it('shows list errors and empty results', async () => {
  listEquipment.mockRejectedValue(new Error('Нет соединения'));
  renderAt();
  expect(await screen.findByRole('alert')).toHaveTextContent('Нет соединения');
});

it('ignores an old save after navigating to another equipment card', async () => {
  let resolveSave!: (value: ReturnType<typeof mockEquipment>) => void;
  (api.updateEquipment as jest.Mock).mockImplementation(() => new Promise((resolve) => { resolveSave = resolve; }));
  getEquipment.mockImplementation(async (_token, id) => mockEquipment({ id, model: `Модель ${id}` }));
  render(<MemoryRouter initialEntries={['/equipment/1']}>
    <Link to="/equipment/2">Другая карточка</Link>
    <Routes><Route path="/equipment/:id" element={<EquipmentDetailPage token="tok" canWrite canReadMovements />} /></Routes>
  </MemoryRouter>);
  await userEvent.click(await screen.findByRole('button', { name: 'Редактировать оборудование' }));
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await userEvent.click(screen.getByRole('link', { name: 'Другая карточка' }));
  await screen.findByRole('heading', { name: 'Модель 2' });
  await act(async () => resolveSave(mockEquipment({ model: 'Старая модель' })));
  expect(screen.getByRole('heading', { name: 'Модель 2' })).toBeInTheDocument();
  expect(screen.queryByRole('heading', { name: 'Старая модель' })).not.toBeInTheDocument();
});
