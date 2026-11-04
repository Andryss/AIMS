import React from 'react';
import { render, screen, within } from '@testing-library/react';
import { HistoryTimeline } from './HistoryTimeline';
import { HistoryDiffList } from './HistoryDiffList';

type Entry = { id: number; snapshot: string };

it('compares the first row to the preceding page and subsequent rows to their immediate predecessor', () => {
  render(<HistoryTimeline entries={[{ id: 11, snapshot: 'Принята' }, { id: 12, snapshot: 'Выполнена' }]}
    previousEntry={{ id: 10, snapshot: 'Новая' }} renderHeader={(entry) => `Событие #${entry.id}`}>
    {(entry: Entry, previous) => <HistoryDiffList rows={[{ label: 'Статус', oldValue: previous?.snapshot, newValue: entry.snapshot }]}
      renderValue={(value) => value} />}
  </HistoryTimeline>);
  const rows = screen.getAllByRole('listitem');
  const first = rows.find((row) => within(row).queryByText('Событие #11'))!;
  const second = rows.find((row) => within(row).queryByText('Событие #12'))!;
  expect(within(first).getByText('Новая → Принята')).toBeInTheDocument();
  expect(within(second).getByText('Принята → Выполнена')).toBeInTheDocument();
});

it('renders a creation value without a previous arrow and only shows an empty message when requested', () => {
  const { rerender } = render(<HistoryDiffList rows={[{ label: 'Субъект', newValue: 'Альф' }]} renderValue={(value) => value} />);
  expect(screen.getByText('Альф')).toHaveTextContent(/^Альф$/);
  expect(screen.queryByText(/→/)).not.toBeInTheDocument();
  rerender(<HistoryDiffList rows={[]} renderValue={(value: string) => value} />);
  expect(screen.queryByRole('list')).not.toBeInTheDocument();
  rerender(<HistoryDiffList rows={[]} renderValue={(value: string) => value} emptyMessage="Без изменений полей" />);
  expect(screen.getByText('Без изменений полей')).toBeInTheDocument();
});
