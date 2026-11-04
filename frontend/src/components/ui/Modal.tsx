import React, { ReactNode, useEffect, useId } from 'react';
import { useFocusTrap } from '../../hooks/useFocusTrap';
import { Button } from './Button';

interface ModalProps {
  open: boolean;
  title: ReactNode;
  onClose: () => void;
  busy?: boolean;
  children: ReactNode;
  className?: string;
}

export function Modal({ open, title, onClose, busy = false, children, className = '' }: ModalProps) {
  const titleId = useId();
  const trapRef = useFocusTrap(open);
  useEffect(() => {
    if (!open) { return; }
    const escape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !busy) { onClose(); }
    };
    document.addEventListener('keydown', escape);
    return () => document.removeEventListener('keydown', escape);
  }, [open, busy, onClose]);
  if (!open) { return null; }
  const close = () => { if (!busy) { onClose(); } };
  return <div className="modal-overlay" role="presentation" onClick={close}>
    <div className={`modal ${className}`.trim()} ref={trapRef} role="dialog" aria-modal="true"
      aria-labelledby={titleId} aria-busy={busy || undefined} onClick={(event) => event.stopPropagation()}>
      <div className="modal__header"><h2 id={titleId}>{title}</h2>
        <Button variant="ghost" aria-label="Закрыть" disabled={busy} onClick={close}>×</Button>
      </div>
      {children}
    </div>
  </div>;
}
