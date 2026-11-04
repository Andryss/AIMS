import React from 'react';
import { render, screen } from '@testing-library/react';
import { UserAvatar } from './UserAvatar';

describe('UserAvatar', () => {
  it('renders svg with login label', () => {
    render(<UserAvatar login="analyst" size={48} />);
    expect(screen.getByRole('img', { name: 'Аватар analyst' })).toBeInTheDocument();
  });

  it('applies custom class name', () => {
    render(<UserAvatar login="admin" className="profile-avatar" />);
    expect(screen.getByRole('img', { name: 'Аватар admin' })).toHaveClass('user-avatar', 'profile-avatar');
  });
});
