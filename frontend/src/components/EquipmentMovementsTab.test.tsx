import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockEquipment, mockEquipmentMovement, mockIncident } from '../test/testData';
import { EquipmentMovementsTab } from './EquipmentMovementsTab';
import { EquipmentMovementDetailPage } from './EquipmentMovementDetailPage';

jest.mock('../api/client');
const listMovements = api.listEquipmentMovements as jest.MockedFunction<typeof api.listEquipmentMovements>;
const getMovement = api.getEquipmentMovement as jest.MockedFunction<typeof api.getEquipmentMovement>;
const movement = mockEquipmentMovement();

function renderAt(path = '/equipment-movements', canProcess = true, currentUserId = 1) {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/equipment-movements" element={<EquipmentMovementsTab token="tok" />} />
    <Route path="/equipment-movements/:id" element={<EquipmentMovementDetailPage token="tok" canCreate
      canProcess={canProcess} currentUserId={currentUserId} canReadEquipment />} />
  </Routes></MemoryRouter>);
}

beforeEach(() => {
  jest.clearAllMocks();
  listMovements.mockResolvedValue({ items: [movement], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  getMovement.mockResolvedValue(movement);
  (api.getIncident as jest.Mock).mockResolvedValue(mockIncident());
  (api.getEquipment as jest.Mock).mockResolvedValue(mockEquipment());
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 3, login: 'agent' }] });
  (api.listEquipmentMovementHistory as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10,
    totalElements: 0, totalPages: 0 });
});

it('opens a movement card and preserves list filters on return', async () => {
  listMovements.mockResolvedValue({ items: [movement], page: 1, size: 10, totalElements: 12, totalPages: 2 });
  renderAt('/equipment-movements?status=PENDING&page=1');
  await userEvent.click(await screen.findByRole('link', { name: 'Заявка #7' }));
  expect(await screen.findByRole('heading', { name: 'Заявка #7 — Выдача' })).toBeInTheDocument();
  expect(screen.getByRole('link', { name: 'Нейралайзер Н-1' })).toHaveAttribute('href', '/equipment/1');
  await userEvent.click(screen.getByRole('link', { name: /К списку/ }));
  await screen.findByRole('link', { name: 'Заявка #7' });
  expect(listMovements).toHaveBeenLastCalledWith('tok', expect.objectContaining({ status: 'PENDING', page: 1 }));
});

it('filters movement status and operation', async () => {
  renderAt();
  await screen.findByRole('link', { name: 'Заявка #7' });
  fireEvent.change(screen.getByLabelText('Статус'), { target: { value: 'PROCESSING' } });
  fireEvent.change(screen.getByLabelText('Операция'), { target: { value: 'RETURN' } });
  await waitFor(() => expect(listMovements).toHaveBeenLastCalledWith('tok', expect.objectContaining({
    status: 'PROCESSING', operation: 'RETURN', page: 0,
  })));
});

it('shows responsible employees with avatars and the equipment quantity', async () => {
  renderAt();
  expect(await screen.findByRole('img', { name: 'Аватар agent' })).toBeInTheDocument();
  expect(screen.getByRole('columnheader', { name: 'Количество оборудования' })).toBeInTheDocument();
});

it('places request history in the bottom history tab', async () => {
  renderAt('/equipment-movements/7');
  await screen.findByRole('heading', { name: 'Заявка #7 — Выдача' });
  expect(screen.getByRole('tab', { name: 'История' })).toHaveAttribute('aria-selected', 'true');
  expect(screen.getByRole('tabpanel', { name: 'История' })).toBeInTheDocument();
});

it('ignores unsupported enum filters in the URL', async () => {
  renderAt('/equipment-movements?status=constructor&operation=toString');
  await screen.findByRole('link', { name: 'Заявка #7' });
  expect(listMovements).toHaveBeenCalledWith('tok', expect.objectContaining({ status: undefined, operation: undefined }));
});

it.each([
  [1, false, true], [9, false, false], [9, true, true],
])('shows pending edit for user=%s process=%s', async (id, process, visible) => {
  renderAt('/equipment-movements/7', process, id);
  await screen.findByRole('heading', { name: 'Заявка #7 — Выдача' });
  expect(Boolean(screen.queryByRole('button', { name: 'Редактировать заявку' }))).toBe(visible);
});

it('offers partial return only for the outstanding equipment of a completed issue', async () => {
  getMovement.mockResolvedValue(mockEquipmentMovement({ status: 'COMPLETED', equipment: [mockEquipment({
    status: 'ISSUED', holderUserId: 3, currentIssueMovementId: 7,
  })] }));
  renderAt('/equipment-movements/7', false);
  await userEvent.click(await screen.findByRole('button', { name: 'Оформить возврат' }));
  expect(await screen.findByRole('dialog', { name: 'Заявка на возврат' })).toBeInTheDocument();
});

it('hides mutations for completed incidents and terminal movements', async () => {
  (api.getIncident as jest.Mock).mockResolvedValue(mockIncident({ status: 'EXECUTION_COMPLETED' }));
  renderAt('/equipment-movements/7');
  await screen.findByRole('heading', { name: 'Заявка #7 — Выдача' });
  expect(screen.queryByRole('combobox', { name: 'Статус заявки' })).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Редактировать заявку' })).not.toBeInTheDocument();
});

it.each(['0', 'abc'])('rejects invalid request id %s', async (id) => {
  renderAt(`/equipment-movements/${id}`);
  expect(await screen.findByRole('alert')).toHaveTextContent('Некорректный идентификатор заявки');
  expect(getMovement).not.toHaveBeenCalled();
});

it('ignores an old status response after navigating to another request', async () => {
  let resolveStatus!: (value: ReturnType<typeof mockEquipmentMovement>) => void;
  (api.changeEquipmentMovementStatus as jest.Mock).mockImplementation(() => new Promise((resolve) => { resolveStatus = resolve; }));
  getMovement.mockImplementation(async (_token, id) => mockEquipmentMovement({ id }));
  render(<MemoryRouter initialEntries={['/equipment-movements/7']}>
    <Link to="/equipment-movements/8">Другая заявка</Link>
    <Routes><Route path="/equipment-movements/:id" element={<EquipmentMovementDetailPage token="tok" canCreate
      canProcess currentUserId={1} canReadEquipment />} /></Routes>
  </MemoryRouter>);
  fireEvent.change(await screen.findByRole('combobox', { name: 'Статус заявки' }), { target: { value: 'PROCESSING' } });
  await userEvent.click(screen.getByRole('link', { name: 'Другая заявка' }));
  await screen.findByRole('heading', { name: 'Заявка #8 — Выдача' });
  await act(async () => resolveStatus(mockEquipmentMovement({ status: 'PROCESSING' })));
  expect(screen.getByRole('heading', { name: 'Заявка #8 — Выдача' })).toBeInTheDocument();
});

it('does not redirect a departed card when an old return creation completes', async () => {
  const issue = mockEquipmentMovement({ status: 'COMPLETED', equipment: [mockEquipment({
    status: 'ISSUED', holderUserId: 3, currentIssueMovementId: 7,
  })] });
  let resolveCreate!: (value: ReturnType<typeof mockEquipmentMovement>) => void;
  (api.createEquipmentMovement as jest.Mock).mockImplementation(() => new Promise((resolve) => { resolveCreate = resolve; }));
  getMovement.mockImplementation(async (_token, id) => id === 7 ? issue : mockEquipmentMovement({ id }));
  render(<MemoryRouter initialEntries={['/equipment-movements/7']}>
    <Link to="/equipment-movements/8">Другая заявка</Link>
    <Routes><Route path="/equipment-movements/:id" element={<EquipmentMovementDetailPage token="tok" canCreate
      canProcess currentUserId={1} canReadEquipment />} /></Routes>
  </MemoryRouter>);
  await userEvent.click(await screen.findByRole('button', { name: 'Оформить возврат' }));
  fireEvent.change(screen.getByLabelText('Поиск оборудования'), { target: { value: 'SN' } });
  await userEvent.click(await screen.findByRole('button', { name: 'Добавить SN-001' }));
  await userEvent.click(screen.getByRole('button', { name: 'Создать заявку' }));
  await userEvent.click(screen.getByRole('link', { name: 'Другая заявка' }));
  await screen.findByRole('heading', { name: 'Заявка #8 — Выдача' });
  await act(async () => resolveCreate(mockEquipmentMovement({ id: 9, operation: 'RETURN' })));
  expect(screen.getByRole('heading', { name: 'Заявка #8 — Выдача' })).toBeInTheDocument();
});

it('ignores an old card load after navigating to another request', async () => {
  let resolveOld!: (value: ReturnType<typeof mockEquipmentMovement>) => void;
  getMovement.mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }))
    .mockResolvedValue(mockEquipmentMovement({ id: 8 }));
  render(<MemoryRouter initialEntries={['/equipment-movements/7']}>
    <Link to="/equipment-movements/8">Другая заявка</Link>
    <Routes><Route path="/equipment-movements/:id" element={<EquipmentMovementDetailPage token="tok" canCreate
      canProcess currentUserId={1} canReadEquipment />} /></Routes>
  </MemoryRouter>);
  await userEvent.click(screen.getByRole('link', { name: 'Другая заявка' }));
  await screen.findByRole('heading', { name: 'Заявка #8 — Выдача' });
  await act(async () => resolveOld(mockEquipmentMovement()));
  expect(screen.getByRole('heading', { name: 'Заявка #8 — Выдача' })).toBeInTheDocument();
});
