import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockTender } from '../test/testData';
import { TenderEditorModal } from './TenderEditorModal';

jest.mock('../api/client');
beforeEach(() => jest.clearAllMocks());

it.each([false, true])('creates or prefills a tender, editing=%s', async (editing) => {
  (api.createTender as jest.Mock).mockResolvedValue(mockTender());
  (api.updateTender as jest.Mock).mockResolvedValue(mockTender());
  const onSaved = jest.fn();
  render(<TenderEditorModal token="tok" open tender={editing ? mockTender({ incidentId: 8 }) : null}
    incidentId={8} hasOffers={false} onClose={jest.fn()} onSaved={onSaved} />);
  expect(screen.getByLabelText(/Условия закупки/)).toHaveValue(editing ? 'Поставка за неделю' : '');
  expect(screen.getByLabelText(/Инцидент-основание/)).toHaveValue(8);
  fireEvent.change(screen.getByLabelText(/Тип оборудования/), { target: { value: 'NEUTRALIZER' } });
  fireEvent.change(screen.getByLabelText(/Количество оборудования/), { target: { value: '3' } });
  fireEvent.change(screen.getByLabelText(/Условия закупки/), { target: { value: '  Новые условия  ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(onSaved).toHaveBeenCalled());
  const payload = { equipmentType: 'NEUTRALIZER', quantity: 3, conditions: 'Новые условия', incidentId: 8 };
  expect(editing ? api.updateTender : api.createTender).toHaveBeenCalledWith(...(editing
    ? ['tok', 4, payload] : ['tok', payload]));
});

it('freezes type and quantity after an offer and retains conditions after a conflict', async () => {
  (api.updateTender as jest.Mock).mockRejectedValue(new Error('Тендер изменён'));
  render(<TenderEditorModal token="tok" open tender={mockTender({ status: 'COLLECTING_OFFERS' })}
    hasOffers onClose={jest.fn()} onSaved={jest.fn()} />);
  expect(screen.getByLabelText(/Тип оборудования/)).toBeDisabled();
  expect(screen.getByLabelText(/Количество оборудования/)).toBeDisabled();
  fireEvent.change(screen.getByLabelText(/Условия закупки/), { target: { value: 'Уточнение' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Тендер изменён');
  expect(screen.getByLabelText(/Условия закупки/)).toHaveValue('Уточнение');
});

it('blocks duplicate saves and ignores completion after unmount', async () => {
  let resolve!: (value: ReturnType<typeof mockTender>) => void;
  (api.updateTender as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  const onSaved = jest.fn();
  const onClose = jest.fn();
  const view = render(<TenderEditorModal token="tok" open tender={mockTender()} hasOffers={false}
    onClose={onClose} onSaved={onSaved} />);
  const form = screen.getByRole('form', { name: 'Редактировать тендер' });
  fireEvent.submit(form);
  fireEvent.submit(form);
  expect(api.updateTender).toHaveBeenCalledTimes(1);
  await userEvent.keyboard('{Escape}');
  expect(onClose).not.toHaveBeenCalled();
  view.unmount();
  await act(async () => resolve(mockTender()));
  expect(onSaved).not.toHaveBeenCalled();
});


it.each([false, true])('keeps the draft and submit guard through a same-ID refresh, pending=%s', async (pending) => {
  let resolve!: (value: ReturnType<typeof mockTender>) => void;
  (api.updateTender as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  const onSaved = jest.fn();
  const onClose = jest.fn();
  const view = render(<TenderEditorModal token="tok" open tender={mockTender()} hasOffers={false}
    onClose={onClose} onSaved={onSaved} />);
  fireEvent.change(screen.getByLabelText(/Условия закупки/), { target: { value: 'Мой черновик' } });
  const form = screen.getByRole('form', { name: 'Редактировать тендер' });
  if (pending) { fireEvent.submit(form); }
  view.rerender(<TenderEditorModal token="tok" open tender={mockTender({ conditions: 'Серверное обновление' })}
    hasOffers={false} onClose={onClose} onSaved={onSaved} />);
  expect(screen.getByLabelText(/Условия закупки/)).toHaveValue('Мой черновик');
  fireEvent.submit(form);
  expect(api.updateTender).toHaveBeenCalledTimes(1);
  await act(async () => resolve(mockTender()));
  expect(onSaved).toHaveBeenCalledTimes(1);
});
