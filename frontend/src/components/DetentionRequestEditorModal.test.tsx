import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockAlien, mockDetentionRequest } from '../test/testData';
import { DetentionRequestEditorModal } from './DetentionRequestEditorModal';

jest.mock('../api/client');
beforeEach(() => {
  jest.clearAllMocks();
  (api.getAlien as jest.Mock).mockResolvedValue(mockAlien());
  (api.searchAliens as jest.Mock).mockResolvedValue({ items: [mockAlien()] });
});

it('creates with the fixed incident and preselected species', async () => {
  (api.createDetentionRequest as jest.Mock).mockResolvedValue(mockDetentionRequest());
  const saved = jest.fn();
  render(<DetentionRequestEditorModal token="tok" open incidentId={8} initialAlienId={1}
    onClose={jest.fn()} onSaved={saved} />);
  fireEvent.change(screen.getByLabelText(/Имя или обозначение/), { target: { value: '  Субъект 7  ' } });
  fireEvent.change(screen.getByLabelText(/Описание субъекта/), { target: { value: '  Задержан у вокзала  ' } });
  expect(screen.getByLabelText(/Инцидент-основание/)).toBeDisabled();
  expect(screen.queryByLabelText(/Дата прибытия/)).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(api.createDetentionRequest).toHaveBeenCalledWith('tok', { incidentId: 8, alienId: 1,
    subjectName: 'Субъект 7', subjectDescription: 'Задержан у вокзала', durationDays: 30 });
  await waitFor(() => expect(saved).toHaveBeenCalledTimes(1));
});

it('prefills editing, requires a changed-duration reason and preserves the draft after failure', async () => {
  (api.updateDetentionRequest as jest.Mock).mockRejectedValue(new Error('Конфликт состояния'));
  render(<DetentionRequestEditorModal token="tok" open incidentId={8} request={mockDetentionRequest()}
    onClose={jest.fn()} onSaved={jest.fn()} />);
  expect(screen.getByLabelText(/Имя или обозначение/)).toHaveValue('Субъект 7');
  fireEvent.change(screen.getByLabelText(/Срок заключения/), { target: { value: '15' } });
  const form = screen.getByRole('button', { name: 'Сохранить' }).closest('form')!;
  fireEvent.submit(form);
  expect(api.updateDetentionRequest).not.toHaveBeenCalled();
  fireEvent.change(screen.getByLabelText(/Причина/), { target: { value: 'Пересмотр срока' } });
  fireEvent.submit(form);
  expect(await screen.findByRole('alert')).toHaveTextContent('Конфликт состояния');
  expect(screen.getByLabelText(/Срок заключения/)).toHaveValue(15);
});

it('keeps a same-ID draft and ignores the save after unmounting', async () => {
  let resolve!: (value: ReturnType<typeof mockDetentionRequest>) => void;
  (api.updateDetentionRequest as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  const saved = jest.fn(); const close = jest.fn();
  const view = render(<DetentionRequestEditorModal token="tok" open incidentId={8} request={mockDetentionRequest()}
    onClose={close} onSaved={saved} />);
  fireEvent.change(screen.getByLabelText(/Описание субъекта/), { target: { value: 'Мой черновик' } });
  view.rerender(<DetentionRequestEditorModal token="tok" open incidentId={8}
    request={mockDetentionRequest({ subjectDescription: 'Серверное обновление' })} onClose={close} onSaved={saved} />);
  expect(screen.getByLabelText(/Описание субъекта/)).toHaveValue('Мой черновик');
  const form = screen.getByRole('button', { name: 'Сохранить' }).closest('form')!;
  fireEvent.submit(form); fireEvent.submit(form);
  expect(api.updateDetentionRequest).toHaveBeenCalledTimes(1);
  await userEvent.keyboard('{Escape}'); expect(close).not.toHaveBeenCalled();
  view.unmount(); await act(async () => resolve(mockDetentionRequest()));
  expect(saved).not.toHaveBeenCalled();
});

it('searches species and can clear the choice', async () => {
  render(<DetentionRequestEditorModal token="tok" open incidentId={8} onClose={jest.fn()} onSaved={jest.fn()} />);
  fireEvent.change(screen.getByLabelText('Поиск вида'), { target: { value: 'сли' } });
  await userEvent.click(await screen.findByRole('button', { name: /Выбрать.*Слизень/ }));
  expect(screen.getByRole('button', { name: 'Убрать вид' })).toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Убрать вид' }));
  expect(screen.queryByRole('button', { name: 'Убрать вид' })).not.toBeInTheDocument();
});

it('omits an unselected species in the full replacement payload', async () => {
  (api.updateDetentionRequest as jest.Mock).mockResolvedValue(mockDetentionRequest());
  render(<DetentionRequestEditorModal token="tok" open incidentId={8} request={mockDetentionRequest({ alienId: 1 })}
    onClose={jest.fn()} onSaved={jest.fn()} />);
  await userEvent.click(screen.getByRole('button', { name: 'Убрать вид' }));
  await act(async () => fireEvent.submit(screen.getByRole('button', { name: 'Сохранить' }).closest('form')!));
  expect((api.updateDetentionRequest as jest.Mock).mock.calls[0][2]).not.toHaveProperty('alienId');
});
