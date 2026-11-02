import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockAlien } from '../test/testData';
import { AlienEditorModal } from './AlienEditorModal';

jest.mock('../api/client');
const createAlien = api.createAlien as jest.MockedFunction<typeof api.createAlien>;
const updateAlien = api.updateAlien as jest.MockedFunction<typeof api.updateAlien>;

beforeEach(() => jest.clearAllMocks());

it.each([false, true])('saves a species with editing=%s', async (editing) => {
  const alien = mockAlien();
  const saved = mockAlien({ name: 'Новый вид', description: 'Признаки', threatLevel: 6 });
  createAlien.mockResolvedValue(saved);
  updateAlien.mockResolvedValue(saved);
  const onSaved = jest.fn();
  const onClose = jest.fn();
  render(<AlienEditorModal token="tok" open alien={editing ? alien : null}
    onSaved={onSaved} onClose={onClose} />);
  expect(screen.getByLabelText(/Название/)).toHaveValue(editing ? alien.name : '');
  expect(screen.getByLabelText(/Описание/)).toHaveValue(editing ? alien.description : '');
  expect(screen.getByLabelText(/Уровень угрозы/)).toHaveValue(editing ? 3 : null);
  fireEvent.change(screen.getByLabelText(/Название/), { target: { value: '  Новый вид  ' } });
  fireEvent.change(screen.getByLabelText(/Описание/), { target: { value: '  Признаки  ' } });
  fireEvent.change(screen.getByLabelText(/Уровень угрозы/), { target: { value: '6' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  const payload = { name: 'Новый вид', description: 'Признаки', threatLevel: 6 };
  await waitFor(() => expect(onSaved).toHaveBeenCalledWith(saved));
  if (editing) {
    expect(updateAlien).toHaveBeenCalledWith('tok', alien.id, payload);
    expect(createAlien).not.toHaveBeenCalled();
  } else {
    expect(createAlien).toHaveBeenCalledWith('tok', payload);
    expect(updateAlien).not.toHaveBeenCalled();
  }
  expect(onClose).toHaveBeenCalledTimes(1);
});

it.each(['0', '11', '1.5'])('rejects invalid threat level %s', async (level) => {
  render(<AlienEditorModal token="tok" open alien={mockAlien()}
    onSaved={jest.fn()} onClose={jest.fn()} />);
  fireEvent.change(screen.getByLabelText(/Уровень угрозы/), { target: { value: level } });
  fireEvent.submit(screen.getByRole('button', { name: 'Сохранить' }).closest('form')!);
  expect(await screen.findByRole('alert')).toHaveTextContent(/целым числом/);
  expect(updateAlien).not.toHaveBeenCalled();
});

it('rejects whitespace-only text', () => {
  render(<AlienEditorModal token="tok" open alien={mockAlien()}
    onSaved={jest.fn()} onClose={jest.fn()} />);
  fireEvent.change(screen.getByLabelText(/Название/), { target: { value: '   ' } });
  fireEvent.submit(screen.getByRole('button', { name: 'Сохранить' }).closest('form')!);
  expect(screen.getByRole('alert')).toHaveTextContent(/название и описание/);
  expect(updateAlien).not.toHaveBeenCalled();
});

it('preserves input after failure and permits retry', async () => {
  updateAlien.mockRejectedValueOnce(new Error('Нет соединения')).mockResolvedValue(mockAlien());
  render(<AlienEditorModal token="tok" open alien={mockAlien()}
    onSaved={jest.fn()} onClose={jest.fn()} />);
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Нет соединения');
  expect(screen.getByLabelText(/Название/)).toHaveValue('Слизень');
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(updateAlien).toHaveBeenCalledTimes(2));
});

it('resets fields when opening creation after editing', () => {
  const props = { token: 'tok', onSaved: jest.fn(), onClose: jest.fn() };
  const { rerender } = render(<AlienEditorModal {...props} open alien={mockAlien()} />);
  rerender(<AlienEditorModal {...props} open={false} alien={null} />);
  rerender(<AlienEditorModal {...props} open alien={null} />);
  expect(screen.getByLabelText(/Название/)).toHaveValue('');
  expect(screen.getByLabelText(/Описание/)).toHaveValue('');
  expect(screen.getByLabelText(/Уровень угрозы/)).toHaveValue(null);
});

it('closes on Escape and restores the launcher focus', async () => {
  const onClose = jest.fn();
  const props = { token: 'tok', onSaved: jest.fn(), onClose, alien: null };
  const { rerender } = render(<><button>Открыть</button><AlienEditorModal {...props} open={false} /></>);
  screen.getByRole('button', { name: 'Открыть' }).focus();
  rerender(<><button>Открыть</button><AlienEditorModal {...props} open /></>);
  expect(screen.getByRole('dialog')).toContainElement(document.activeElement as HTMLElement);
  await userEvent.keyboard('{Escape}');
  expect(onClose).toHaveBeenCalledTimes(1);
  rerender(<><button>Открыть</button><AlienEditorModal {...props} open={false} /></>);
  expect(screen.getByRole('button', { name: 'Открыть' })).toHaveFocus();
  expect(createAlien).not.toHaveBeenCalled();
});

it('blocks repeated submission and closing while saving', async () => {
  updateAlien.mockImplementation(() => new Promise(() => {}));
  const onClose = jest.fn();
  render(<AlienEditorModal token="tok" open alien={mockAlien()}
    onSaved={jest.fn()} onClose={onClose} />);
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(screen.getByRole('button', { name: /Сохранить/ })).toBeDisabled();
  await userEvent.keyboard('{Escape}');
  expect(updateAlien).toHaveBeenCalledTimes(1);
  expect(onClose).not.toHaveBeenCalled();
});
