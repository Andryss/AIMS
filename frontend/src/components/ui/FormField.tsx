import React, { useId } from 'react';

type FieldControlProps = {
  id?: string;
  className?: string;
  'aria-invalid'?: boolean;
  'aria-describedby'?: string;
};

export interface FormFieldProps {
  label: string;
  htmlFor?: string;
  required?: boolean;
  error?: string | null;
  help?: string;
  children: React.ReactElement<FieldControlProps>;
  className?: string;
}

export function FormField({
  label,
  htmlFor,
  required,
  error,
  help,
  children,
  className = '',
}: FormFieldProps) {
  const autoId = useId();
  const fieldId = htmlFor ?? autoId;
  const errorId = error ? `${fieldId}-error` : undefined;
  const helpId = help ? `${fieldId}-help` : undefined;
  const describedBy = [helpId, errorId].filter(Boolean).join(' ') || undefined;

  const control = React.cloneElement<FieldControlProps>(children, {
    id: fieldId,
    className: ['field__control', children.props.className].filter(Boolean).join(' '),
    'aria-invalid': error ? true : undefined,
    'aria-describedby': describedBy,
  });

  return (
    <div className={`field${error ? ' field--error' : ''}${className ? ` ${className}` : ''}`}>
      <label className="field__label" htmlFor={fieldId}>
        {label}
        {required && (
          <span aria-hidden="true" className="field__required">
            {' '}
            *
          </span>
        )}
      </label>
      {help && (
        <p id={helpId} className="field__help">
          {help}
        </p>
      )}
      {control}
      {error && (
        <p id={errorId} className="field__error" role="alert">
          {error}
        </p>
      )}
    </div>
  );
}
