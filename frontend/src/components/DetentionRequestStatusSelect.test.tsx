import React from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';
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

it.each([true, false])('allows only the author to cancel a new request, owner=%s', (owner) => {
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest()}
    permissions={['DETENTION_EDIT']} currentUserId={owner ? 2 : 3} onChanged={jest.fn()} />);
  expect(Boolean(screen.queryByRole('combobox'))).toBe(owner);
  if (owner) { expect(screen.getAllByRole('option').map((item) => item.textContent)).toEqual(['Новая', 'Отменена']); }
});

it.each(['CANCELLED', 'REJECTED', 'RELEASED'] as DetentionRequestStatus[])('requires a reason for %s and prevents duplicate submission', async (target) => {
  let resolve!: (value: ReturnType<typeof mockDetentionRequest>) => void;
  (api.changeDetentionRequestStatus as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  const changed = jest.fn();
  render(<DetentionRequestStatusSelect token="tok" request={mockDetentionRequest({
    status: target === 'RELEASED' ? 'IN_CUSTODY' : 'NEW', plannedReleaseAt: '2999-01-01T00:00:00Z',
  })} permissions={permissions} onChanged={changed} />);
  fireEvent.change(screen.getByRole('combobox'), { target: { value: target } });
  const form = screen.getByRole('button', { name: 'Подтвердить' }).closest('form')!;
  fireEvent.submit(form);
  expect(api.changeDetentionRequestStatus).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: '  Уточнение  ' } });
  fireEvent.submit(form); fireEvent.submit(form);
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
  await act(async () => {});
  expect(changed).toHaveBeenCalledTimes(1);
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
  await act(async () => fireEvent.submit(screen.getByRole('button', { name: 'Подтвердить' }).closest('form')!));
  expect(api.changeDetentionRequestStatus).toHaveBeenLastCalledWith('tok', 7, { status: 'RELEASED', comment: 'Основания отпали' });
  expect(changed).toHaveBeenCalledTimes(1);
});
