import React from 'react';
import { render, screen } from '@testing-library/react';
import { StatusChip } from './StatusChip';

describe('StatusChip', () => {
  it('renders label with status modifier', () => {
    const { container } = render(<StatusChip status="in-progress" label="В работе" />);
    expect(screen.getByText('В работе')).toBeInTheDocument();
    expect(container.querySelector('.status-chip.status-chip--in_progress')).toBeInTheDocument();
  });

  it('applies custom class name', () => {
    const { container } = render(
      <StatusChip status="new" label="Новый" className="detail-status" />,
    );
    expect(container.querySelector('.status-chip.status-chip--new.detail-status')).toBeInTheDocument();
  });
});
