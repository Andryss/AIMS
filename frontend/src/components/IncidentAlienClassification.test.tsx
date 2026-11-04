import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockAlien, mockIncident } from '../test/testData';
import { IncidentAlienClassification } from './IncidentAlienClassification';

jest.mock('../api/client');
const mockApi = api as jest.Mocked<typeof api>;
const incident = mockIncident({ id: 8, status: 'READY_FOR_ANALYSIS', attachmentFileIds: [12, 13] });
const alien = mockAlien({ id: 6, name: 'Ашта', threatLevel: 9, description: 'Подробности во всплывающей карточке' });

beforeEach(() => {
  jest.clearAllMocks();
  mockApi.classifyIncidentAlien.mockResolvedValue({ status: 'MATCH', confidence: 95.6094, alien });
  mockApi.putIncidentAlien.mockResolvedValue({ ...incident, alienId: 6 });
});
function show(extra = {}) {
  const onAccepted = jest.fn();
  const props = { token: 'tok', incident, canAccept: true, onAccepted, ...extra };
  return { ...render(<MemoryRouter><IncidentAlienClassification {...props} /></MemoryRouter>), props, onAccepted };
}

it('recognizes by incident ID and links only after explicit acceptance', async () => {
  const { onAccepted } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  expect(await screen.findByText('95,6%')).toBeInTheDocument();
  expect(screen.getByText('Ашта')).toBeInTheDocument();
  expect(screen.queryByText(alien.description)).not.toBeInTheDocument();
  expect(mockApi.classifyIncidentAlien).toHaveBeenCalledWith('tok', 8);
  expect(mockApi.putIncidentAlien).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  await waitFor(() => expect(onAccepted).toHaveBeenCalledWith(expect.objectContaining({ alienId: 6 }), alien));
  expect(mockApi.putIncidentAlien).toHaveBeenCalledWith('tok', 8, 6);
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
});

it('dismisses the proposal locally and supports another attempt', async () => {
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
  fireEvent.click(screen.getByRole('button', { name: 'Отклонить' }));
  expect(screen.queryByText('Ашта')).not.toBeInTheDocument();
  expect(mockApi.putIncidentAlien).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
  expect(mockApi.classifyIncidentAlien).toHaveBeenCalledTimes(2);
});

it('shows no-match confidence with no accept action', async () => {
  mockApi.classifyIncidentAlien.mockResolvedValue({ status: 'NO_MATCH', confidence: 15.4, alien: null });
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  expect(await screen.findByText('15,4%')).toBeInTheDocument();
  expect(screen.getByText('Не удалось подобрать вид')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Отклонить' }));
  expect(mockApi.putIncidentAlien).not.toHaveBeenCalled();
});

it('retains a proposal after a save failure, without duplicating classification', async () => {
  mockApi.putIncidentAlien.mockRejectedValueOnce(new Error('Статус изменился')).mockResolvedValueOnce({ ...incident, alienId: 6 });
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Статус изменился');
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  await waitFor(() => expect(mockApi.putIncidentAlien).toHaveBeenCalledTimes(2));
  expect(mockApi.classifyIncidentAlien).toHaveBeenCalledTimes(1);
});

it('ignores a late response after navigation and clears old results', async () => {
  let resolve!: (value: Awaited<ReturnType<typeof api.classifyIncidentAlien>>) => void;
  mockApi.classifyIncidentAlien.mockReturnValue(new Promise((done) => { resolve = done; }));
  const { rerender, props, onAccepted } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  rerender(<MemoryRouter><IncidentAlienClassification {...props} incident={{ ...incident, id: 9 }} /></MemoryRouter>);
  await act(async () => { resolve({ status: 'MATCH', confidence: 99, alien }); });
  expect(screen.queryByText('Ашта')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
  expect(onAccepted).not.toHaveBeenCalled();
});

it.each([{ canAccept: false }, { incident: { ...incident, status: 'EXECUTING' as const } }])(
  'cannot accept without current permissions/status: %j', async (props) => {
    show(props);
    const button = screen.queryByRole('button', { name: 'Распознать вид' });
    if (button && !button.hasAttribute('disabled')) {
      fireEvent.click(button);
      await screen.findByText('95,6%');
    }
    expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
    expect(mockApi.putIncidentAlien).not.toHaveBeenCalled();
  });

it('shows technical errors separately and allows retry', async () => {
  mockApi.classifyIncidentAlien.mockRejectedValueOnce(new Error('Распознавание недоступно'));
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Распознавание недоступно');
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
});


it('does not apply a late save response to another incident', async () => {
  let resolve!: (value: ReturnType<typeof mockIncident>) => void;
  mockApi.putIncidentAlien.mockReturnValue(new Promise((done) => { resolve = done; }));
  const { rerender, props, onAccepted } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  rerender(<MemoryRouter><IncidentAlienClassification {...props} incident={{ ...incident, id: 9 }} /></MemoryRouter>);
  await act(async () => { resolve({ ...incident, alienId: 6 }); });
  expect(onAccepted).not.toHaveBeenCalled();
  expect(screen.queryByText('Ашта')).not.toBeInTheDocument();
});

it('clears a completed proposal when leaving the page', async () => {
  const view = show();
  fireEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
  view.unmount();
  show();
  expect(screen.queryByText('95,6%')).not.toBeInTheDocument();
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
  expect(mockApi.putIncidentAlien).not.toHaveBeenCalled();
});
