import React from 'react';
import { render, screen } from '@testing-library/react';
import { Card, CardHeader } from './Card';

describe('Card', () => {
  it('renders children with default card class', () => {
    const { container } = render(<Card>Body</Card>);
    expect(container.querySelector('.card')).toHaveTextContent('Body');
    expect(container.querySelector('.card--flat')).not.toBeInTheDocument();
  });

  it('supports flat and custom class names', () => {
    const { container } = render(
      <Card flat className="extra">
        Flat
      </Card>,
    );
    expect(container.querySelector('.card.card--flat.extra')).toBeInTheDocument();
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
