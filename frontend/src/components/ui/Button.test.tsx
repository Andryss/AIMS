import React from 'react';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { Button } from './Button';

describe('Button', () => {
  it('renders button with variant and size classes', () => {
    render(<Button variant="outline" size="sm">Save</Button>);
    const button = screen.getByRole('button', { name: 'Save' });
    expect(button).toHaveTextContent('Save');
    expect(button).toHaveClass('btn', 'btn--outline', 'btn--sm');
  });

  it('renders loading state', () => {
    render(<Button loading>Wait</Button>);
    expect(screen.getByRole('button', { name: 'Wait' })).toBeDisabled();
    expect(screen.getByRole('button')).toHaveAttribute('aria-busy', 'true');
  });

  it('renders router link when to is provided', () => {
    render(
      <MemoryRouter>
        <Button to="/incidents" variant="ghost" block>
          Open
        </Button>
      </MemoryRouter>,
    );
    const link = screen.getByRole('link', { name: 'Open' });
    expect(link).toHaveAttribute('href', '/incidents');
    expect(link).toHaveClass('btn', 'btn--ghost', 'btn--block');
  });

  it('marks link as disabled', () => {
    render(
      <MemoryRouter>
        <Button to="/x" disabled>
          Blocked
        </Button>
      </MemoryRouter>,
    );
    expect(screen.getByRole('link', { name: 'Blocked' })).toHaveAttribute('aria-disabled', 'true');
  });
});
