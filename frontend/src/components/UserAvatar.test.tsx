import React from 'react';
import { render, screen } from '@testing-library/react';
import { UserAvatar } from './UserAvatar';

describe('UserAvatar', () => {
  it('renders svg with login label', () => {
    render(<UserAvatar login="analyst" size={48} />);
    expect(screen.getByRole('img', { name: 'Аватар analyst' })).toBeInTheDocument();
  });

  it('applies custom class name', () => {
    const { container } = render(<UserAvatar login="admin" className="profile-avatar" />);
    expect(container.querySelector('.user-avatar.profile-avatar')).toBeInTheDocument();
  });
});
