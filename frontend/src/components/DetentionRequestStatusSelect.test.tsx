import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockDetentionRequest } from '../test/testData';
import { DetentionRequestStatus } from '../types';
import { DetentionRequestStatusSelect } from './DetentionRequestStatusSelect';

jest.mock('../api/client');
beforeEach(() => jest.clearAllMocks());
const permissions = ['DETENTION_READ', 'DETENTION_EDIT', 'DETENTION_PROCESS'];

it.each([
  ['NEW', ['Принята', 'Отклонена', 'Отменена']],
  ['ACCEPTED', ['Под стражей', 'Отменена']],
  ['IN_CUSTODY', ['Освобождён']],
  ['RELEASED', []], ['REJECTED', []], ['CANCELLED', []],
] as Array<[DetentionRequestStatus, string[]]>)('offers only transitions from %s', (status, options) => {
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest({ status })}
    permissions={permissions} currentUserId={2} onChanged={jest.fn()} />);
  expect(screen.queryAllByRole('option').slice(1).map((item) => item.textContent)).toEqual(options);
});

it.each([
  { actor: 'author', currentUserId: 2, options: ['Новая', 'Отменена'], editable: true },
  { actor: 'another user', currentUserId: 3, options: [], editable: false },
])('allows cancellation of a new request by $actor', ({ currentUserId, options, editable }) => {
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest()}
    permissions={['DETENTION_EDIT']} currentUserId={currentUserId} onChanged={jest.fn()} />);
  expect(Boolean(screen.queryByRole('combobox'))).toBe(editable);
  expect(screen.queryAllByRole('option').map((item) => item.textContent)).toEqual(options);
});

it.each(['CANCELLED', 'REJECTED', 'RELEASED'] as DetentionRequestStatus[])('requires a reason for %s and prevents duplicate submission', async (target) => {
  let resolve!: (value: ReturnType<typeof mockDetentionRequest>) => void;
  (api.changeDetentionRequestStatus as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  const changed = jest.fn();
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest({
    status: target === 'RELEASED' ? 'IN_CUSTODY' : 'NEW', plannedReleaseAt: '2999-01-01T00:00:00Z',
  })} permissions={permissions} onChanged={changed} />);
  fireEvent.change(screen.getByRole('combobox'), { target: { value: target } });
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '   ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(api.changeDetentionRequestStatus).not.toHaveBeenCalled();
  expect(screen.getByRole('alert')).toHaveTextContent('Укажите причину');
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '  Уточнение  ' } });
  const confirmButton = screen.getByRole('button', { name: 'Подтвердить' });
  act(() => {
    confirmButton.click();
    expect(confirmButton).toBeEnabled();
    confirmButton.click();
  });
  expect(api.changeDetentionRequestStatus).toHaveBeenCalledTimes(1);
  expect(api.changeDetentionRequestStatus).toHaveBeenCalledWith('tok', 7, { status: target, comment: 'Уточнение' });
  await act(async () => resolve(mockDetentionRequest({ status: target })));
  expect(changed).toHaveBeenCalledTimes(1);
});

it('releases after the deadline without asking for a reason', async () => {
  (api.changeDetentionRequestStatus as jest.Mock).mockResolvedValue(mockDetentionRequest({ status: 'RELEASED' }));
  const changed = jest.fn();
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest({ status: 'IN_CUSTODY',
    plannedReleaseAt: '2000-01-01T00:00:00Z' })} permissions={permissions} onChanged={changed} />);
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'RELEASED' } });
  expect(api.changeDetentionRequestStatus).toHaveBeenCalledWith('tok', 7, { status: 'RELEASED' });
  await waitFor(() => expect(changed).toHaveBeenCalledTimes(1));
});

it('asks for a reason if the server sees an extended deadline', async () => {
  (api.changeDetentionRequestStatus as jest.Mock)
    .mockRejectedValueOnce(Object.assign(new Error('Укажите причину досрочного освобождения'), { status: 400 }))
    .mockResolvedValue(mockDetentionRequest({ status: 'RELEASED' }));
  const changed = jest.fn();
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest({ status: 'IN_CUSTODY',
    plannedReleaseAt: '2000-01-01T00:00:00Z' })} permissions={permissions} onChanged={changed} />);
  fireEvent.change(screen.getByRole('combobox'), { target: { value: 'RELEASED' } });
  const reason = await screen.findByLabelText(/Причина/);
  fireEvent.change(reason, { target: { value: 'Основания отпали' } });
  await userEvent.click(screen.getByRole('button', { name: 'Подтвердить' }));
  expect(api.changeDetentionRequestStatus).toHaveBeenLastCalledWith('tok', 7, { status: 'RELEASED', comment: 'Основания отпали' });
  await waitFor(() => expect(changed).toHaveBeenCalledTimes(1));
});
