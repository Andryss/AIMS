import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockEquipment } from '../test/testData';
import { EquipmentEditorModal } from './EquipmentEditorModal';

jest.mock('../api/client');
const createEquipment = api.createEquipment as jest.MockedFunction<typeof api.createEquipment>;
const updateEquipment = api.updateEquipment as jest.MockedFunction<typeof api.updateEquipment>;
beforeEach(() => jest.clearAllMocks());

it.each([false, true])('saves equipment with editing=%s and prefilled fields', async (editing) => {
  const saved = mockEquipment({ type: 'NEUTRALIZER', model: 'НТ-2', serialNumber: 'SER-2' });
  createEquipment.mockResolvedValue(saved);
  updateEquipment.mockResolvedValue(saved);
  const onSaved = jest.fn();
  render(<EquipmentEditorModal token="tok" open equipment={editing ? mockEquipment() : null}
    onClose={jest.fn()} onSaved={onSaved} />);
  expect(screen.getByLabelText(/Модель/)).toHaveValue(editing ? 'Нейралайзер Н-1' : '');
  fireEvent.change(screen.getByLabelText(/Тип оборудования/), { target: { value: 'NEUTRALIZER' } });
  fireEvent.change(screen.getByLabelText(/Модель/), { target: { value: '  НТ-2  ' } });
  fireEvent.change(screen.getByLabelText(/Серийный номер/), { target: { value: '  SER-2  ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
  const payload = { type: 'NEUTRALIZER', model: 'НТ-2', serialNumber: 'SER-2' };
  expect(editing ? updateEquipment : createEquipment).toHaveBeenCalledWith(...(editing
    ? ['tok', 1, payload] : ['tok', payload]));
  expect(screen.queryByLabelText('Держатель')).not.toBeInTheDocument();
});

it('preserves input after a duplicate serial error', async () => {
  updateEquipment.mockRejectedValue(new Error('Серийный номер уже существует'));
  render(<EquipmentEditorModal token="tok" open equipment={mockEquipment()}
    onClose={jest.fn()} onSaved={jest.fn()} />);
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Серийный номер уже существует');
  expect(screen.getByLabelText(/Серийный номер/)).toHaveValue('SN-001');
});

it('rejects whitespace and blocks repeat submissions while saving', async () => {
  updateEquipment.mockImplementation(() => new Promise(() => {}));
  const onClose = jest.fn();
  render(<EquipmentEditorModal token="tok" open equipment={mockEquipment()}
    onClose={onClose} onSaved={jest.fn()} />);
  fireEvent.change(screen.getByLabelText(/Модель/), { target: { value: '  ' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Сохранить' }).closest('form')!);
  expect(screen.getByRole('alert')).toHaveTextContent('Укажите тип, модель и серийный номер');
  expect(updateEquipment).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText(/Модель/), { target: { value: 'Модель' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(screen.getByRole('button', { name: 'Сохранить' })).toBeDisabled();
  await userEvent.keyboard('{Escape}');
  expect(onClose).not.toHaveBeenCalled();
});
