import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockTenderParticipant } from '../test/testData';
import { TenderParticipantEditorModal } from './TenderParticipantEditorModal';

jest.mock('../api/client');
beforeEach(() => jest.clearAllMocks());

it.each([false, true])('creates or updates an offer, editing=%s', async (editing) => {
  (api.createTenderParticipant as jest.Mock).mockResolvedValue(mockTenderParticipant());
  (api.updateTenderParticipant as jest.Mock).mockResolvedValue(mockTenderParticipant());
  const onSaved = jest.fn();
  render(<TenderParticipantEditorModal token="tok" open tenderId={4}
    participant={editing ? mockTenderParticipant() : null} onClose={jest.fn()} onSaved={onSaved} />);
  expect(screen.getByLabelText(/Организация/)).toHaveValue(editing ? 'ООО Орбита' : '');
  fireEvent.change(screen.getByLabelText(/Организация/), { target: { value: '  ООО Сириус  ' } });
  fireEvent.change(screen.getByLabelText(/Предложение/), { target: { value: '  Пять дней  ' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(onSaved).toHaveBeenCalled());
  expect(editing ? api.updateTenderParticipant : api.createTenderParticipant).toHaveBeenCalledWith(...(editing
    ? ['tok', 4, 7, { name: 'ООО Сириус', proposal: 'Пять дней' }]
    : ['tok', 4, { name: 'ООО Сириус', proposal: 'Пять дней' }]));
});

it('keeps the edited proposal after a conflict', async () => {
  (api.updateTenderParticipant as jest.Mock).mockRejectedValue(new Error('Сбор предложений завершён'));
  render(<TenderParticipantEditorModal token="tok" open tenderId={4} participant={mockTenderParticipant()}
    onClose={jest.fn()} onSaved={jest.fn()} />);
  fireEvent.change(screen.getByLabelText(/Предложение/), { target: { value: 'Уточнение' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Сбор предложений завершён');
  expect(screen.getByLabelText(/Предложение/)).toHaveValue('Уточнение');
});
