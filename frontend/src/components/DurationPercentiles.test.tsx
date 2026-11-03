import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { DurationPercentiles } from './DurationPercentiles';

it('keeps an empty sample distinct from a real zero', () => {
  const view = render(<DurationPercentiles value={{ sampleCount: 0, excludedSampleCount: 2 }} />);
  expect(screen.getByText('Нет данных')).toBeInTheDocument();
  expect(screen.getByText('Выборка: 0')).toBeInTheDocument();
  expect(screen.getByText('Исключено: 2')).toBeInTheDocument();

  view.rerender(<DurationPercentiles value={{ sampleCount: 1, excludedSampleCount: 0,
    p50Seconds: 0, p90Seconds: 0, p95Seconds: 0, p99Seconds: 0 }} />);
  expect(screen.queryByText('Нет данных')).not.toBeInTheDocument();
  expect(screen.getAllByText('0 с')).toHaveLength(4);
});

it.each([['P50', '30 с'], ['P90', '46 с'], ['P95', '48 с'], ['P99', '49,6 с']])(
  'shows %s with the same duration units', (label, expected) => {
    render(<DurationPercentiles value={{ sampleCount: 5, excludedSampleCount: 0,
      p50Seconds: 30, p90Seconds: 46, p95Seconds: 48, p99Seconds: 49.6 }} />);
    expect(within(screen.getByLabelText(label)).getByText(expected)).toBeInTheDocument();
    expect(screen.getByText('Выборка: 5')).toBeInTheDocument();
  },
);
