import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { UserChip } from './UserChip';

describe('UserChip', () => {
  it('renders login and avatar', () => {
    render(<UserChip login="operator" />);
    expect(screen.getByText('operator')).toBeInTheDocument();
    expect(screen.getByRole('img', { name: 'Аватар operator' })).toBeInTheDocument();
  });

  it('applies removed and custom classes', () => {
    const { container } = render(
      <UserChip login="analyst" removed className="picker-chip" />,
    );
    expect(container.querySelector('.user-chip.user-chip--removed.picker-chip')).toBeInTheDocument();
  });

  it('calls onRemove when removable', async () => {
    const onRemove = jest.fn();
    render(<UserChip login="agent" removable onRemove={onRemove} />);
    await userEvent.click(screen.getByRole('button', { name: 'Убрать agent' }));
    expect(onRemove).toHaveBeenCalledTimes(1);
  });
});
