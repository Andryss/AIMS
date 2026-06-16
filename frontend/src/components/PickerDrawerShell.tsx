import React from 'react';

interface PickerDrawerShellProps {
  title: string;
  titleId: string;
  open: boolean;
  selecting: boolean;
  onClose: () => void;
  children: React.ReactNode;
  footer?: React.ReactNode;
}

export function PickerDrawerShell({
  title,
  titleId,
  open,
  selecting,
  onClose,
  children,
  footer,
}: PickerDrawerShellProps) {
  if (!open) {
    return null;
  }

  return (
    <div className="drawer-overlay" onClick={onClose} role="presentation">
      <div
        className="drawer"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-labelledby={titleId}
        aria-modal="true"
      >
        <div className="drawer__content">
          <div className="drawer__header">
            <h2 id={titleId}>{title}</h2>
            <button
              type="button"
              className="btn btn--ghost"
              onClick={onClose}
              disabled={selecting}
              aria-label="Закрыть"
            >
              ×
            </button>
          </div>
          {children}
          {footer}
        </div>
      </div>
    </div>
  );
}
