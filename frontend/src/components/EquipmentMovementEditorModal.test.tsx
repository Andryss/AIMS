import React from 'react';
import { fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockEquipment, mockEquipmentMovement } from '../test/testData';
import { EquipmentMovementEditorModal } from './EquipmentMovementEditorModal';

jest.mock('../api/client');
const listEquipment = api.listEquipment as jest.MockedFunction<typeof api.listEquipment>;
const createMovement = api.createEquipmentMovement as jest.MockedFunction<typeof api.createEquipmentMovement>;
const updateMovement = api.updateEquipmentMovement as jest.MockedFunction<typeof api.updateEquipmentMovement>;
const props = { token: 'tok', open: true, incidentId: 1, onClose: jest.fn(), onSaved: jest.fn() };

beforeEach(() => {
  jest.clearAllMocks();
  listEquipment.mockResolvedValue({ items: [mockEquipment()], page: 0, size: 10, totalElements: 1, totalPages: 1 });
  (api.searchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 3, login: 'agent' }] });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 3, login: 'agent' }] });
  createMovement.mockResolvedValue(mockEquipmentMovement());
  updateMovement.mockResolvedValue(mockEquipmentMovement());
});

it('creates an issue for a selected employee and specific equipment', async () => {
  render(<EquipmentMovementEditorModal {...props} />);
  fireEvent.change(screen.getByLabelText(/Ответственный/), { target: { value: 'ag' } });
  await userEvent.click(await screen.findByRole('button', { name: 'agent' }));
  await userEvent.click(await screen.findByRole('checkbox', { name: 'Выбрать SN-001' }));
  fireEvent.change(screen.getByLabelText('Комментарий'), { target: { value: '  Для обследования  ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Создать заявку' }));
  await waitFor(() => expect(createMovement).toHaveBeenCalledWith('tok', {
    operation: 'ISSUE', incidentId: 1, responsibleUserId: 3, sourceIssueMovementId: null,
    equipmentIds: [1], comment: 'Для обследования',
  }));
  expect(api.searchUsers).toHaveBeenCalledWith('tok', 'ag');
  expect(listEquipment).toHaveBeenCalledWith('tok', expect.objectContaining({ status: 'AVAILABLE' }));
});

it('prefills pending request editing and preserves the form on failure', async () => {
  updateMovement.mockRejectedValue(new Error('Оборудование уже выдано'));
  render(<EquipmentMovementEditorModal {...props} movement={mockEquipmentMovement()} />);
  expect(screen.getByLabelText('Комментарий')).toHaveValue('Обследование склада');
  expect(await screen.findByRole('checkbox', { name: 'Выбрать SN-001' })).toBeChecked();
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Оборудование уже выдано');
  expect(screen.getByLabelText('Комментарий')).toHaveValue('Обследование склада');
  expect(screen.getByRole('checkbox', { name: 'Выбрать SN-001' })).toBeChecked();
  expect(props.onClose).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole('button', { name: 'Обновить доступность' }));
  await waitFor(() => expect(listEquipment).toHaveBeenCalledTimes(2));
});

it('returns a subset of the equipment still held under this exact issue', async () => {
  const issue = mockEquipmentMovement({ status: 'COMPLETED', equipment: [
    mockEquipment({ status: 'ISSUED', holderUserId: 3, currentIssueMovementId: 7 }),
    mockEquipment({ id: 2, serialNumber: 'RETURNED-002' }),
    mockEquipment({ id: 3, serialNumber: 'REISSUED-003', status: 'ISSUED', currentIssueMovementId: 9 }),
  ] });
  (api.getEquipmentMovement as jest.Mock).mockResolvedValue(issue);
  render(<EquipmentMovementEditorModal {...props} sourceIssue={issue} />);
  expect(screen.queryByRole('checkbox', { name: /RETURNED-002|REISSUED-003/ })).not.toBeInTheDocument();
  expect(screen.queryByLabelText('Поиск оборудования')).not.toBeInTheDocument();
  await userEvent.click(await screen.findByRole('checkbox', { name: 'Выбрать SN-001' }));
  await userEvent.click(screen.getByRole('button', { name: 'Создать заявку' }));
  await waitFor(() => expect(createMovement).toHaveBeenCalledWith('tok', expect.objectContaining({
    operation: 'RETURN', sourceIssueMovementId: 7, responsibleUserId: 3, incidentId: 1, equipmentIds: [1],
  })));
});

it('requires employee and equipment and prevents closing while submitting', async () => {
  createMovement.mockImplementation(() => new Promise(() => {}));
  render(<EquipmentMovementEditorModal {...props} />);
  await screen.findByRole('checkbox', { name: 'Выбрать SN-001' });
  await userEvent.click(screen.getByRole('button', { name: 'Создать заявку' }));
  expect(screen.getByRole('alert')).toHaveTextContent('Укажите ответственного и выберите оборудование');
  fireEvent.change(screen.getByLabelText(/Ответственный/), { target: { value: 'ag' } });
  await userEvent.click(await screen.findByRole('button', { name: 'agent' }));
  await userEvent.click(await screen.findByRole('checkbox', { name: 'Выбрать SN-001' }));
  await userEvent.click(screen.getByRole('button', { name: 'Создать заявку' }));
  expect(screen.getByRole('button', { name: 'Создать заявку' })).toBeDisabled();
  await userEvent.keyboard('{Escape}');
  expect(props.onClose).not.toHaveBeenCalled();
});

it('keeps selections when searching another page and permits removing them', async () => {
  render(<EquipmentMovementEditorModal {...props} />);
  await userEvent.click(await screen.findByRole('checkbox', { name: 'Выбрать SN-001' }));
  listEquipment.mockResolvedValue({ items: [mockEquipment({ id: 2, serialNumber: 'SN-002' })],
    page: 0, size: 10, totalElements: 1, totalPages: 1 });
  fireEvent.change(screen.getByLabelText('Поиск оборудования'), { target: { value: '002' } });
  await screen.findByRole('checkbox', { name: 'Выбрать SN-002' });
  const selected = screen.getByRole('region', { name: 'Выбранное оборудование' });
  expect(within(selected).getByText(/SN-001/)).toBeInTheDocument();
  await userEvent.click(within(selected).getByRole('button', { name: 'Убрать SN-001' }));
  expect(within(selected).queryByText(/SN-001/)).not.toBeInTheDocument();
});
