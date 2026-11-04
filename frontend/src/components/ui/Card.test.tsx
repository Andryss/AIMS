import React from 'react';
import { render, screen } from '@testing-library/react';
import { Card, CardHeader } from './Card';

describe('Card', () => {
  it('renders children with default card class', () => {
    render(<Card>Body</Card>);
    expect(screen.getByText('Body')).toHaveClass('card');
    expect(screen.getByText('Body')).not.toHaveClass('card--flat');
  });

  it('supports flat and custom class names', () => {
    render(
      <Card flat className="extra">
        Flat
      </Card>,
    );
    expect(screen.getByText('Flat')).toHaveClass('card', 'card--flat', 'extra');
  });
});

describe('CardHeader', () => {
  it('renders string title as heading', () => {
    render(<CardHeader title="Заголовок" />);
    expect(screen.getByRole('heading', { name: 'Заголовок' })).toBeInTheDocument();
  });

  it('renders node title and actions', () => {
    render(
      <CardHeader
        title={<span data-testid="custom-title">Custom</span>}
        actions={<button type="button">Action</button>}
      />,
    );
    expect(screen.getByTestId('custom-title')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Action' })).toBeInTheDocument();
  });
});
