import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockEquipmentMovement } from '../test/testData';
import { EquipmentMovementStatus } from '../types';
import { EquipmentMovementStatusSelect } from './EquipmentMovementStatusSelect';

jest.mock('../api/client');
beforeEach(() => jest.clearAllMocks());

it.each([
  ['PENDING', false, 1, ['Отменена']],
  ['PENDING', false, 9, []],
  ['PENDING', true, 9, ['Обрабатывается', 'Отменена', 'Отклонена']],
  ['PROCESSING', true, 9, ['Выполнена', 'Отменена', 'Отклонена']],
  ['COMPLETED', true, 1, []],
  ['CANCELLED', true, 1, []],
  ['REJECTED', true, 1, []],
] as const)('allows actions for status=%s process=%s user=%s', (status, canProcess, currentUserId, labels) => {
  render(<EquipmentMovementStatusSelect token="tok" movement={mockEquipmentMovement({ status })}
    canCreate canProcess={canProcess} currentUserId={currentUserId} onChanged={jest.fn()} />);
  expect(screen.queryAllByRole('combobox')).toHaveLength(labels.length > 0 ? 1 : 0);
  expect(screen.queryAllByRole('option').slice(1).map((option) => option.textContent)).toEqual(labels);
});

it.each(['CANCELLED', 'REJECTED'] as EquipmentMovementStatus[])('requires a reason for %s', async (target) => {
  (api.changeEquipmentMovementStatus as jest.Mock).mockResolvedValue(mockEquipmentMovement({ status: target }));
  const onChanged = jest.fn();
  render(<EquipmentMovementStatusSelect token="tok" movement={mockEquipmentMovement()}
    canCreate canProcess currentUserId={1} onChanged={onChanged} />);
  fireEvent.change(screen.getByRole('combobox'), { target: { value: target } });
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '   ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(api.changeEquipmentMovementStatus).not.toHaveBeenCalled();
  expect(screen.getByRole('alert')).toHaveTextContent('Укажите причину');
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '  Не требуется  ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  await waitFor(() => expect(api.changeEquipmentMovementStatus).toHaveBeenCalledWith('tok', 7,
    { status: target, comment: 'Не требуется' }));
  expect(onChanged).toHaveBeenCalled();
});
