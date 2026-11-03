import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockEquipment, mockTender } from '../test/testData';
import { TenderReceiptModal } from './TenderReceiptModal';

jest.mock('../api/client');
beforeEach(() => jest.clearAllMocks());

function open() {
  return render(<TenderReceiptModal token="tok" open
    tender={mockTender({ status: 'AWAITING_DELIVERY', winnerParticipantId: 7 })}
    onClose={jest.fn()} onSaved={jest.fn()} />);
}

it('registers a complete delivery with trimmed serials and fixed tender type', async () => {
  (api.receiveTender as jest.Mock).mockResolvedValue({ tender: mockTender({ status: 'COMPLETED' }),
    equipment: [mockEquipment(), mockEquipment({ id: 2 })] });
  open();
  expect(screen.getByText(/Необходимо принять: 2/)).toBeInTheDocument();
  fireEvent.change(screen.getByLabelText('Модель 1'), { target: { value: '  Н-1  ' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 1'), { target: { value: '  SN-1  ' } });
  fireEvent.change(screen.getByLabelText('Модель 2'), { target: { value: 'Н-2' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 2'), { target: { value: 'SN-2' } });
  await userEvent.click(screen.getByRole('button', { name: 'Принять поставку' }));
  await waitFor(() => expect(api.receiveTender).toHaveBeenCalledWith('tok', 4,
    { equipment: [{ model: 'Н-1', serialNumber: 'SN-1' }, { model: 'Н-2', serialNumber: 'SN-2' }] }));
  expect(screen.queryByRole('combobox')).not.toBeInTheDocument();
});

it('preserves rows through removal/addition and server conflict', async () => {
  (api.receiveTender as jest.Mock).mockRejectedValue(new Error('Серийный номер уже существует'));
  open();
  fireEvent.change(screen.getByLabelText('Модель 1'), { target: { value: 'Модель' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 1'), { target: { value: 'SN-1' } });
  await userEvent.click(screen.getByRole('button', { name: 'Удалить экземпляр 2' }));
  expect(screen.getByLabelText('Модель 1')).toHaveValue('Модель');
  await userEvent.click(screen.getByRole('button', { name: 'Добавить экземпляр' }));
  fireEvent.change(screen.getByLabelText('Модель 2'), { target: { value: 'Другая модель' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 2'), { target: { value: 'SN-2' } });
  await userEvent.click(screen.getByRole('button', { name: 'Принять поставку' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Серийный номер уже существует');
  expect(screen.getByLabelText('Серийный номер 1')).toHaveValue('SN-1');
  expect(screen.getByLabelText('Модель 2')).toHaveValue('Другая модель');
});

it('rejects duplicate normalized serials and does not submit twice', async () => {
  (api.receiveTender as jest.Mock).mockImplementation(() => new Promise(() => {}));
  open();
  fireEvent.change(screen.getByLabelText('Модель 1'), { target: { value: 'A' } });
  fireEvent.change(screen.getByLabelText('Модель 2'), { target: { value: 'B' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 1'), { target: { value: 'SN' } });
  fireEvent.change(screen.getByLabelText('Серийный номер 2'), { target: { value: ' sn ' } });
  const form = screen.getByRole('button', { name: 'Принять поставку' }).closest('form')!;
  fireEvent.submit(form);
  expect(screen.getByRole('alert')).toHaveTextContent('Серийные номера не должны повторяться');
  expect(api.receiveTender).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText('Серийный номер 2'), { target: { value: 'OTHER' } });
  fireEvent.submit(form);
  fireEvent.submit(form);
  expect(api.receiveTender).toHaveBeenCalledTimes(1);
});
