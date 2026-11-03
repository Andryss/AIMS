import React from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';
import { AnalyticsPeriodFilter } from './AnalyticsPeriodFilter';

beforeEach(() => jest.useFakeTimers('modern'));
afterEach(() => jest.useRealTimers());

it('does not reinterpret an untouched bound in the repeated DST hour', () => {
  const onChange = jest.fn();
  render(<AnalyticsPeriodFilter from="2025-11-01T00:00:00Z" to="2025-11-02T01:30:00-05:00"
    onChange={onChange} onValidityChange={jest.fn()} />);
  act(() => { jest.advanceTimersByTime(350); });
  expect(onChange).not.toHaveBeenCalled();
});

it('accepts a valid period whose bounds differ by one microsecond', () => {
  const onChange = jest.fn();
  render(<AnalyticsPeriodFilter from="2025-05-01T00:00:00.000001Z" to="2025-05-01T00:00:00.000002Z"
    onChange={onChange} onValidityChange={jest.fn()} />);
  expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  act(() => { jest.advanceTimersByTime(350); });
  expect(onChange).not.toHaveBeenCalled();
});

it('preserves the exact unedited bound when the other bound changes', () => {
  const onChange = jest.fn();
  render(<AnalyticsPeriodFilter from="2025-05-01T00:00:00.123456Z" to="2025-05-02T00:00:00Z"
    onChange={onChange} onValidityChange={jest.fn()} />);
  fireEvent.change(screen.getByLabelText('До'), { target: { value: '2025-05-03T12:00' } });
  act(() => { jest.advanceTimersByTime(350); });
  expect(onChange).toHaveBeenCalledWith({ from: '2025-05-01T00:00:00.123456Z', to: new Date('2025-05-03T12:00').toISOString() });
});
