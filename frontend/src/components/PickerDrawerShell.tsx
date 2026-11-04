import React, { useEffect } from 'react';
import { useFocusTrap } from '../hooks/useFocusTrap';

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
  const trapRef = useFocusTrap(open);

  useEffect(() => {
    if (!open) { return; }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !selecting) { onClose(); }
    };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [open, selecting, onClose]);

  const close = () => { if (!selecting) { onClose(); } };

  if (!open) {
    return null;
  }

  return (
    <div className="drawer-overlay" onClick={close} role="presentation">
      <div
        ref={trapRef}
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
              onClick={close}
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
