import React from 'react';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import { ThreatCalculationDetails } from './ThreatCalculationDetails';

const calculation = { calculatedThreatLevel: 7, contributions: [
  { criterion: 'ALIEN_THREAT' as const, adjustment: 4 },
  { criterion: 'EVENT_TYPE' as const, adjustment: 2 },
  { criterion: 'VICTIMS' as const, adjustment: 1 },
] };

beforeEach(() => { jest.useFakeTimers(); });
afterEach(() => { jest.runOnlyPendingTimers(); jest.useRealTimers(); });

it('shows the criteria in rows and their sum after a short hover', () => {
  render(<ThreatCalculationDetails calculation={calculation} eventType="ATTACK_ON_PEOPLE" />);
  const trigger = screen.getByRole('button', { name: 'Показать причины оценки' });
  fireEvent.mouseEnter(trigger);
  act(() => { jest.advanceTimersByTime(499); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  act(() => { jest.advanceTimersByTime(1); });
  const tooltip = screen.getByRole('tooltip');
  expect(within(tooltip).getAllByRole('term').map((element) => element.textContent))
    .toEqual(['Угроза вида', 'Нападение на людей', 'Пострадавшие']);
  expect(within(tooltip).getAllByRole('definition').map((element) => element.textContent)).toEqual(['+4', '+2', '+1']);
  expect(within(tooltip).getByText('Сумма').parentElement).toHaveTextContent('7');
  fireEvent.mouseLeave(trigger);
  fireEvent.mouseEnter(tooltip);
  act(() => { jest.advanceTimersByTime(200); });
  expect(tooltip).toBeInTheDocument();
  fireEvent.mouseLeave(tooltip);
  act(() => { jest.advanceTimersByTime(150); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('opens on click or keyboard focus and dismisses with Escape', () => {
  render(<ThreatCalculationDetails calculation={calculation} eventType="ATTACK_ON_PEOPLE" />);
  const trigger = screen.getByRole('button', { name: 'Показать причины оценки' });
  act(() => { trigger.focus(); jest.advanceTimersByTime(500); });
  expect(screen.getByRole('tooltip')).toBeInTheDocument();
  fireEvent.keyDown(trigger, { key: 'Escape' });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  act(() => { jest.advanceTimersByTime(600); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
  fireEvent.click(trigger);
  expect(screen.getByRole('tooltip')).toBeInTheDocument();
  fireEvent.mouseDown(document.body);
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it.each(['hover', 'focus'])('cancels pending opening with Escape after %s', (source) => {
  render(<ThreatCalculationDetails calculation={calculation} eventType="ATTACK_ON_PEOPLE" />);
  const trigger = screen.getByRole('button', { name: 'Показать причины оценки' });
  if (source === 'hover') { fireEvent.mouseEnter(trigger); }
  else { act(() => { trigger.focus(); }); }
  act(() => { jest.advanceTimersByTime(250); });
  fireEvent.keyDown(document, { key: 'Escape' });
  act(() => { jest.advanceTimersByTime(500); });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('formats the default base, negative and zero adjustments', () => {
  render(<ThreatCalculationDetails eventType="UNIDENTIFIED_SIGHTING" calculation={{ calculatedThreatLevel: 2,
    contributions: [{ criterion: 'DEFAULT_THREAT', adjustment: 3 }, { criterion: 'EVENT_TYPE', adjustment: -1 },
      { criterion: 'VICTIMS', adjustment: 0 }] }} />);
  fireEvent.click(screen.getByRole('button', { name: 'Показать причины оценки' }));
  const tooltip = screen.getByRole('tooltip');
  expect(within(tooltip).getByText('Базовая угроза')).toBeInTheDocument();
  expect(within(tooltip).getAllByRole('definition').map((element) => element.textContent)).toEqual(['+3', '−1', '0']);
  expect(within(tooltip).getByText('Сумма').parentElement).toHaveTextContent('2');
});

it('keeps the raw sum distinct from a score limited to the domain scale', () => {
  render(<ThreatCalculationDetails eventType="ATTACK_ON_PEOPLE" calculation={{ calculatedThreatLevel: 10,
    contributions: [{ criterion: 'ALIEN_THREAT', adjustment: 10 }, { criterion: 'EVENT_TYPE', adjustment: 2 },
      { criterion: 'VICTIMS', adjustment: 3 }] }} />);
  fireEvent.click(screen.getByRole('button', { name: 'Показать причины оценки' }));
  const tooltip = screen.getByRole('tooltip');
  expect(within(tooltip).getByText('Сумма').parentElement).toHaveTextContent('15');
  expect(within(tooltip).getByText('В пределах 1–10').parentElement).toHaveTextContent('10');
});
