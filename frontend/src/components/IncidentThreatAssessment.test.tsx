import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import * as api from '../api/client';
import { mockIncident } from '../test/testData';
import { IncidentThreatAssessment } from './IncidentThreatAssessment';

jest.mock('../api/client', () => ({
  ...jest.requireActual('../api/client'),
  getIncident: jest.fn(),
  calculateIncidentThreat: jest.fn(),
  setIncidentThreatLevel: jest.fn(),
}));
const mockApi = api as jest.Mocked<typeof api>;
const incident = mockIncident({ id: 8, status: 'READY_FOR_ANALYSIS', eventType: 'ATTACK_ON_PEOPLE',
  alienId: 6, victimCount: 2, threatLevel: 3 });
const result = { calculatedThreatLevel: 7, contributions: [
  { criterion: 'ALIEN_THREAT' as const, adjustment: 4 },
  { criterion: 'EVENT_TYPE' as const, adjustment: 2 },
  { criterion: 'VICTIMS' as const, adjustment: 1 },
] };

beforeEach(() => {
  jest.clearAllMocks();
  mockApi.calculateIncidentThreat.mockResolvedValue(result);
  mockApi.setIncidentThreatLevel.mockResolvedValue({ ...incident, threatLevel: 8 });
});

function show(extra = {}) {
  const onUpdated = jest.fn();
  const props = { token: 'tok', incident, canCalculate: true, onUpdated, alienThreatLevel: 4, ...extra };
  return { ...render(<IncidentThreatAssessment {...props} />), props, onUpdated };
}

it('shows a proposal beside the calculation button and saves only after explicit acceptance', async () => {
  const { onUpdated } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  const level = await screen.findByLabelText('Уровень угрозы');
  expect(level).toHaveTextContent('7');
  expect(screen.getByRole('button', { name: 'Показать причины оценки' })).toHaveTextContent('?');
  expect(level.closest('button')).toBeNull();
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  expect(screen.queryByText('Угроза вида')).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Изменить угрозу' })).toHaveTextContent('Изменить');
  expect(screen.queryByText(/Предлагаемая угроза|Корректировка аналитика|Итог|Расчёт:/)).not.toBeInTheDocument();
  expect(screen.getByRole('button', { name: 'Рассчитать угрозу' })).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Увеличить угрозу' }));
  expect(level).toHaveTextContent('8');
  expect(mockApi.setIncidentThreatLevel).not.toHaveBeenCalled();
  expect(mockApi.calculateIncidentThreat).toHaveBeenCalledWith('tok', 8);
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ id: 8, threatLevel: 8 })));
  expect(mockApi.setIncidentThreatLevel).toHaveBeenCalledWith('tok', 8, 8);
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
});

it('rejects locally and restores the previously saved level', async () => {
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  await screen.findByLabelText('Уровень угрозы');
  fireEvent.click(screen.getByRole('button', { name: 'Уменьшить угрозу' }));
  fireEvent.click(screen.getByRole('button', { name: 'Отклонить' }));
  expect(screen.getByText('3')).toBeInTheDocument();
  expect(mockApi.setIncidentThreatLevel).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  expect(await screen.findByLabelText('Уровень угрозы')).toHaveTextContent('7');
});

it.each([1, 10])('keeps the adjustable value inside 1–10, starting at %s', async (level) => {
  mockApi.calculateIncidentThreat.mockResolvedValue({ calculatedThreatLevel: level, contributions: [] });
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  const value = await screen.findByLabelText('Уровень угрозы');
  const increase = screen.getByRole('button', { name: 'Увеличить угрозу' });
  const decrease = screen.getByRole('button', { name: 'Уменьшить угрозу' });
  expect(level === 1 ? decrease : increase).toBeDisabled();
  fireEvent.click(level === 1 ? decrease : increase);
  expect(value).toHaveTextContent(String(level));
  fireEvent.click(level === 1 ? increase : decrease);
  expect(value).toHaveTextContent(level === 1 ? '2' : '9');
});

it.each([{ canCalculate: false }, { incident: { ...incident, status: 'EXECUTION_COMPLETED' as const } }])(
  'shows only the saved value without calculation access: %j', (extra) => {
    show(extra);
    expect(screen.getByText('3')).toBeInTheDocument();
    expect(screen.queryByRole('button')).not.toBeInTheDocument();
    expect(mockApi.calculateIncidentThreat).not.toHaveBeenCalled();
  });

it('can retry a failed calculation', async () => {
  mockApi.calculateIncidentThreat.mockRejectedValueOnce(new Error('Расчёт недоступен'));
  show({ incident: { ...incident, threatLevel: null } });
  expect(screen.getByText('—')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Расчёт недоступен');
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  expect(await screen.findByLabelText('Уровень угрозы')).toHaveTextContent('7');
});

it('retains the selected value after a failed save for retry', async () => {
  mockApi.setIncidentThreatLevel.mockRejectedValueOnce(new Error('Сервер недоступен'));
  const { onUpdated } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  await screen.findByLabelText('Уровень угрозы');
  fireEvent.click(screen.getByRole('button', { name: 'Увеличить угрозу' }));
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Сервер недоступен');
  expect(screen.getByLabelText('Уровень угрозы')).toHaveTextContent('8');
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  await waitFor(() => expect(onUpdated).toHaveBeenCalledTimes(1));
  expect(mockApi.calculateIncidentThreat).toHaveBeenCalledTimes(1);
});

it('discards a proposal if saving reports that the incident is completed', async () => {
  mockApi.setIncidentThreatLevel.mockRejectedValueOnce(new api.ApiError(409, { humanMessage: 'Инцидент завершён' }));
  mockApi.getIncident.mockResolvedValue({ ...incident, status: 'EXECUTION_COMPLETED', threatLevel: 6 });
  const { onUpdated } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  await screen.findByLabelText('Уровень угрозы');
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  expect(await screen.findByRole('alert')).toHaveTextContent('Инцидент завершён');
  await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ status: 'EXECUTION_COMPLETED', threatLevel: 6 })));
  expect(mockApi.getIncident).toHaveBeenCalledWith('tok', 8);
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
});

it.each([{ token: 'other' }, { incident: { ...incident, id: 9 } }, { canCalculate: false },
  { incident: { ...incident, victimCount: 3 } }, { incident: { ...incident, alienId: 7 } },
  { alienThreatLevel: 5 }])('ignores late calculations after the scope changes: %j', async (change) => {
  let resolve!: (value: typeof result) => void;
  mockApi.calculateIncidentThreat.mockReturnValue(new Promise((done) => { resolve = done; }));
  const { rerender, props, onUpdated } = show();
  const button = screen.getByRole('button', { name: 'Рассчитать угрозу' });
  fireEvent.click(button);
  expect(button).toBeDisabled();
  fireEvent.click(button);
  expect(mockApi.calculateIncidentThreat).toHaveBeenCalledTimes(1);
  rerender(<IncidentThreatAssessment {...props} {...change} />);
  await act(async () => { resolve(result); });
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
  expect(onUpdated).not.toHaveBeenCalled();
});

it('ignores a late save response after navigation', async () => {
  let resolve!: (value: typeof incident) => void;
  mockApi.setIncidentThreatLevel.mockReturnValue(new Promise((done) => { resolve = done; }));
  const { rerender, props, onUpdated } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  await screen.findByLabelText('Уровень угрозы');
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  expect(screen.getByRole('button', { name: 'Отклонить' })).toBeDisabled();
  rerender(<IncidentThreatAssessment {...props} incident={{ ...incident, id: 9 }} />);
  await act(async () => { resolve({ ...incident, threatLevel: 7 }); });
  expect(onUpdated).not.toHaveBeenCalled();
  expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
});

it('edits the current level without invoking calculation', async () => {
  mockApi.setIncidentThreatLevel.mockResolvedValueOnce({ ...incident, threatLevel: 4 });
  const { onUpdated } = show();
  fireEvent.click(screen.getByRole('button', { name: 'Изменить угрозу' }));
  expect(screen.getByLabelText('Уровень угрозы')).toHaveTextContent('3');
  expect(screen.getAllByText('3')).toHaveLength(1);
  expect(screen.getAllByRole('button').map((button) => button.getAttribute('aria-label') || button.textContent))
    .toEqual(['Уменьшить угрозу', 'Увеличить угрозу', 'Принять', 'Отклонить', 'Рассчитать угрозу']);
  expect(mockApi.calculateIncidentThreat).not.toHaveBeenCalled();
  expect(screen.queryByRole('button', { name: 'Показать причины оценки' })).not.toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Увеличить угрозу' }));
  fireEvent.click(screen.getByRole('button', { name: 'Принять' }));
  await waitFor(() => expect(onUpdated).toHaveBeenCalledWith(expect.objectContaining({ threatLevel: 4 })));
  expect(mockApi.setIncidentThreatLevel).toHaveBeenCalledWith('tok', 8, 4);
});

it('can discard a manual edit and calculate instead', async () => {
  show();
  fireEvent.click(screen.getByRole('button', { name: 'Изменить угрозу' }));
  fireEvent.click(screen.getByRole('button', { name: 'Увеличить угрозу' }));
  fireEvent.click(screen.getByRole('button', { name: 'Отклонить' }));
  expect(screen.getByText('3')).toBeInTheDocument();
  expect(mockApi.setIncidentThreatLevel).not.toHaveBeenCalled();
  fireEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
  expect(await screen.findByLabelText('Уровень угрозы')).toHaveTextContent('7');
});
