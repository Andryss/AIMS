import React, { useEffect, useRef, useState } from 'react';

interface ImagePreviewProps {
  src: string;
  alt: string;
  className: string;
  onError?: () => void;
}

export function ImagePreview({ src, alt, className, onError }: ImagePreviewProps) {
  const [open, setOpen] = useState(false);
  const dialogRef = useRef<HTMLDialogElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) { return; }
    const dialog = dialogRef.current;
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = 'hidden';
    dialog?.showModal();
    return () => {
      if (dialog?.open) { dialog.close(); }
      document.body.style.overflow = previousOverflow;
    };
  }, [open]);

  const close = () => {
    dialogRef.current?.close();
    setOpen(false);
    triggerRef.current?.focus({ preventScroll: true });
  };

  return <>
    <button ref={triggerRef} type="button" className={`image-preview-trigger ${className}`}
      aria-label={`Увеличить ${alt}`} title="Открыть изображение" onClick={() => setOpen(true)}>
      <img src={src} alt={alt} onError={onError} />
    </button>
    {open && <dialog ref={dialogRef} className="image-viewer" aria-label={`Просмотр ${alt}`}
      onCancel={(event) => { event.preventDefault(); close(); }}
      onKeyDown={(event) => {
        if (event.key === 'Escape') { event.preventDefault(); event.stopPropagation(); close(); }
      }}
      onClick={(event) => { if (event.target === event.currentTarget) { close(); } }}>
      <div className="image-viewer__toolbar">
        <span title={alt}>{alt}</span>
        <button type="button" aria-label="Закрыть изображение" onClick={close}>×</button>
      </div>
      <img className="image-viewer__image" src={src} alt={alt} />
    </dialog>}
  </>;
}
