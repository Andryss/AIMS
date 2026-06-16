import React from 'react';
import { Spinner } from './Spinner';

export interface LoadingBlockProps {
  label?: string;
  inline?: boolean;
  skeleton?: boolean;
  rows?: number;
}

export function LoadingBlock({
  label = 'Загрузка…',
  inline = false,
  skeleton = false,
  rows = 3,
}: LoadingBlockProps) {
  if (skeleton) {
    return (
      <div aria-busy="true" aria-label={label}>
        <span className="skeleton skeleton--title" />
        {Array.from({ length: rows }, (_, i) => (
          <span key={i} className="skeleton skeleton--row" />
        ))}
      </div>
    );
  }

  return (
    <div
      className={`loading-block${inline ? ' loading-block--inline' : ''}`}
      role="status"
      aria-busy="true"
      aria-live="polite"
    >
      <Spinner size="sm" label={label} />
      <span>{label}</span>
    </div>
  );
}
