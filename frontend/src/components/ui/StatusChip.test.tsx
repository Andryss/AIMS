import React from 'react';
import { render, screen } from '@testing-library/react';
import { StatusChip } from './StatusChip';

describe('StatusChip', () => {
  it('renders label with status modifier', () => {
    render(<StatusChip status="in-progress" label="В работе" />);
    expect(screen.getByText('В работе')).toBeInTheDocument();
    expect(screen.getByText('В работе')).toHaveClass('status-chip', 'status-chip--in_progress');
  });

  it('applies custom class name', () => {
    render(
      <StatusChip status="new" label="Новый" className="detail-status" />,
    );
    expect(screen.getByText('Новый')).toHaveClass('status-chip', 'status-chip--new', 'detail-status');
  });
});
