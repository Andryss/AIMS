import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { mockProfile } from '../test/testData';
import { ProfileTab } from './ProfileTab';

describe('ProfileTab', () => {
  it('renders profile and signs out', async () => {
    const onSignOut = jest.fn();
    render(
      <ProfileTab
        profile={mockProfile({ roles: ['OPERATOR'], permissions: ['INCIDENT_READ'] })}
        onSignOut={onSignOut}
      />,
    );

    expect(screen.getByRole('heading', { name: 'operator' })).toBeInTheDocument();
    expect(screen.getByText('OPERATOR')).toBeInTheDocument();
    expect(screen.getByText('INCIDENT_READ')).toBeInTheDocument();

    await userEvent.click(screen.getByRole('button', { name: /Выйти/i }));
    expect(onSignOut).toHaveBeenCalled();
  });

  it('shows dash when roles and permissions are empty', () => {
    render(
      <ProfileTab
        profile={mockProfile({ roles: [], permissions: [] })}
        onSignOut={jest.fn()}
      />,
    );

    const dashes = screen.getAllByText('—');
    expect(dashes).toHaveLength(2);
  });
});
