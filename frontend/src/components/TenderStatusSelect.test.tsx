import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockTender } from '../test/testData';
import { TenderStatusSelect } from './TenderStatusSelect';

jest.mock('../api/client');
beforeEach(() => jest.clearAllMocks());

it.each([
  ['NEED', ['TENDER_EDIT'], 2, true], ['NEED', ['TENDER_EDIT'], 3, false],
  ['COLLECTING_OFFERS', ['TENDER_EDIT'], 2, false], ['AWAITING_DELIVERY', ['TENDER_MANAGE'], 3, true],
  ['COMPLETED', ['TENDER_MANAGE'], 2, false], ['CANCELLED', ['TENDER_MANAGE'], 2, false],
] as const)('restricts status actions for %s, %s, actor %s', (status, permissions, currentUserId, editable) => {
  render(<TenderStatusSelect token="tok" tender={mockTender({ status })}
    permissions={[...permissions]} currentUserId={currentUserId} onChanged={jest.fn()} />);
  expect(Boolean(screen.queryByRole('combobox', { name: 'Статус тендера' }))).toBe(editable);
});

it('requires a cancellation reason, preserves it after error and retries', async () => {
  (api.changeTenderStatus as jest.Mock).mockRejectedValueOnce(new Error('Обновите карточку'))
    .mockResolvedValue(mockTender({ status: 'CANCELLED' }));
  const onChanged = jest.fn();
  render(<TenderStatusSelect token="tok" tender={mockTender()} permissions={['TENDER_MANAGE']}
    currentUserId={3} onChanged={onChanged} />);
  fireEvent.change(screen.getByLabelText('Статус тендера'), { target: { value: 'CANCELLED' } });
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '   ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(api.changeTenderStatus).not.toHaveBeenCalled();
  expect(screen.getByRole('alert')).toHaveTextContent('Укажите причину');
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '  Не требуется  ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Обновите карточку');
  expect(screen.getByLabelText(/Причина/)).toHaveValue('  Не требуется  ');
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(api.changeTenderStatus).toHaveBeenLastCalledWith('tok', 4, { status: 'CANCELLED', comment: 'Не требуется' });
  await waitFor(() => expect(onChanged).toHaveBeenCalledTimes(1));
});
