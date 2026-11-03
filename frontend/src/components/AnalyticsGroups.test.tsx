import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { AnalyticsGroups } from './AnalyticsGroups';

it('keeps a literal OTHER location separate from remainder and unknown groups', () => {
  render(<MemoryRouter><AnalyticsGroups groups={[{ key: 'LOCATION:OTHER', name: 'OTHER', count: 2 },
    { key: 'OTHER', count: 1 }, { key: 'UNKNOWN', count: 0 }]} /></MemoryRouter>);
  expect(screen.getByText('OTHER')).toBeInTheDocument();
  expect(screen.getByText('Остальные')).toBeInTheDocument();
  expect(screen.getByText('Не определён')).toBeInTheDocument();
  expect(screen.getByText('0')).toBeInTheDocument();
});
