import React from 'react';
import { render, screen } from '@testing-library/react';
import { MonitoringAlertMediaLinks } from './MonitoringAlertMediaLinks';

describe('MonitoringAlertMediaLinks', () => {
  it('renders nothing when urls are empty', () => {
    const { container } = render(<MonitoringAlertMediaLinks urls={[]} />);
    expect(container).toBeEmptyDOMElement();
  });

  it('renders file name from url path', () => {
    render(<MonitoringAlertMediaLinks urls={['https://example.com/evidence/photo1.jpg']} />);
    expect(screen.getByRole('link', { name: /photo1\.jpg/i })).toHaveAttribute(
      'href',
      'https://example.com/evidence/photo1.jpg',
    );
  });

  it('falls back to numbered label for invalid urls', () => {
    render(<MonitoringAlertMediaLinks urls={['not-a-valid-url']} />);
    expect(screen.getByRole('link', { name: /Материал 1/i })).toHaveAttribute('href', 'not-a-valid-url');
  });

  it('applies compact layout class', () => {
    const { container } = render(
      <MonitoringAlertMediaLinks urls={['https://example.com/a.png']} compact />,
    );
    expect(container.querySelector('.monitoring-alert-media--compact')).toBeInTheDocument();
  });
});
