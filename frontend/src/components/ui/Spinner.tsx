import React from 'react';

export interface SpinnerProps {
  size?: 'sm' | 'md';
  label?: string;
}

export function Spinner({ size = 'md', label = 'Загрузка' }: SpinnerProps) {
  return (
    <span
      className={`spinner${size === 'sm' ? ' spinner--sm' : ''}`}
      role="status"
      aria-label={label}
    />
  );
}
