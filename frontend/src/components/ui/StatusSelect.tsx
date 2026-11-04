import React from 'react';

interface StatusSelectProps<T extends string> {
  value: T;
  options: readonly T[];
  labels: Record<T, string>;
  label: string;
  onChange: (value: T) => void;
  disabled?: boolean;
  busy?: boolean;
  statusClassName?: string;
  placeholder?: string;
}

/** Only renders choices; each domain owns which transitions are authorized. */
export function StatusSelect<T extends string>({ value, options, labels, label, onChange, disabled,
  busy, statusClassName, placeholder }: StatusSelectProps<T>) {
  return <select aria-label={label} value={value} disabled={disabled || busy}
    className={`status-select ${statusClassName ?? `status-chip--${value.toLowerCase()}`}${busy ? ' is-loading' : ''}`}
    onChange={(event) => {
      const next = event.target.value as T;
      if (!disabled && !busy && options.includes(next)) { onChange(next); }
    }}>
    {placeholder && !value ? <option value="" disabled>{placeholder}</option> : <option value={value}>{labels[value]}</option>}
    {options.filter((status) => status !== value).map((status) => <option key={status} value={status}>{labels[status]}</option>)}
  </select>;
}
