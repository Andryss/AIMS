import React from 'react';

export interface StatusChipProps {
  status: string;
  label: string;
  className?: string;
}

export function StatusChip({ status, label, className = '' }: StatusChipProps) {
  const modifier = status.replace(/-/g, '_');
  return (
    <span
      className={`status-chip status-chip--${modifier}${className ? ` ${className}` : ''}`}
    >
      {label}
    </span>
  );
}
