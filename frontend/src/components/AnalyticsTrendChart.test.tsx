import React from 'react';
import { fireEvent, render, screen, within } from '@testing-library/react';
import { AnalyticsTrendChart } from './AnalyticsTrendChart';

const buckets = [
  { from: '2025-05-01T09:00:00Z', to: '2025-05-01T10:00:00Z', values: { created: 2, completed: 0 } },
  { from: '2025-05-01T10:00:00Z', to: '2025-05-01T11:00:00Z', values: { created: 7, completed: 3 } },
];
const series = [{ key: 'created', label: 'Создано' }, { key: 'completed', label: 'Завершено' }];

function chart() {
  render(<AnalyticsTrendChart buckets={buckets} series={series} label="Динамика" bucketUnit="HOUR" />);
  const graph = screen.getByRole('img', { name: 'Динамика' });
  jest.spyOn(graph, 'getBoundingClientRect').mockReturnValue({ left: 0, top: 0, width: 880, height: 215 } as DOMRect);
  return graph;
}

it('highlights the closest measurement and shows all series including zero', () => {
  const graph = chart();
  fireEvent(graph, new MouseEvent('pointermove', { bubbles: true, clientX: 45, clientY: 100 }));
  const tooltip = screen.getByRole('tooltip');
  expect(tooltip).toHaveTextContent('Создано');
  expect(tooltip).toHaveTextContent('Завершено');
  expect(within(tooltip).getByText('2')).toBeInTheDocument();
  expect(within(tooltip).getByText('0')).toBeInTheDocument();
  expect(graph).toHaveAttribute('aria-describedby', tooltip.id);
  fireEvent(graph, new MouseEvent('pointermove', { bubbles: true, clientX: 830, clientY: 100 }));
  expect(within(screen.getByRole('tooltip')).getByText('7')).toBeInTheDocument();
  expect(within(screen.getByRole('tooltip')).getByText('3')).toBeInTheDocument();
  fireEvent(graph, new MouseEvent('pointerout', { bubbles: true }));
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('supports keyboard selection and dismisses the tooltip with Escape', () => {
  const graph = chart();
  fireEvent.focus(graph);
  expect(within(screen.getByRole('tooltip')).getByText('2')).toBeInTheDocument();
  fireEvent.keyDown(graph, { key: 'ArrowRight' });
  expect(within(screen.getByRole('tooltip')).getByText('7')).toBeInTheDocument();
  fireEvent.keyDown(graph, { key: 'Escape' });
  expect(screen.queryByRole('tooltip')).not.toBeInTheDocument();
});

it('keeps the tapped measurement visible on a touch device', () => {
  const graph = chart();
  const event = new MouseEvent('pointerdown', { bubbles: true, clientX: 845, clientY: 100 });
  Object.defineProperty(event, 'pointerType', { value: 'touch' });
  fireEvent(graph, event);
  expect(within(screen.getByRole('tooltip')).getByText('7')).toBeInTheDocument();
});
