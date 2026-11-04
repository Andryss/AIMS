import React, { DragEvent, useEffect, useId, useRef, useState } from 'react';
import { ImagePreview } from './ImagePreview';

function formatFileSize(bytes: number): string {
  if (bytes < 1024) {
    return `${bytes} Б`;
  }
  if (bytes < 1024 * 1024) {
    return `${(bytes / 1024).toFixed(1)} КБ`;
  }
  return `${(bytes / (1024 * 1024)).toFixed(1)} МБ`;
}

function fileKey(file: File): string {
  return `${file.name}-${file.size}-${file.lastModified}`;
}

interface FileUploadFieldProps {
  label: string;
  files: File[];
  onChange: (files: File[]) => void;
  disabled?: boolean;
  accept?: string;
  maxFiles?: number;
}

export function FileUploadField({
  label,
  files,
  onChange,
  disabled = false,
  accept,
  maxFiles,
}: FileUploadFieldProps) {
  const [error, setError] = useState<string | null>(null);
  const [previews, setPreviews] = useState<Map<File, string>>(new Map());
  useEffect(() => {
    const urls = new Map<File, string>();
    files.filter((file) => file.type.startsWith('image/')).forEach((file) => {
      if (URL.createObjectURL) { urls.set(file, URL.createObjectURL(file)); }
    });
    setPreviews(urls);
    return () => urls.forEach((url) => URL.revokeObjectURL(url));
  }, [files]);

  const inputId = useId();
  const inputRef = useRef<HTMLInputElement>(null);
  const [dragActive, setDragActive] = useState(false);

  const addFiles = (incoming: FileList | File[]) => {
    const next = [...files];
    const known = new Set(next.map(fileKey));

    Array.from(incoming).forEach((file) => {
      const key = fileKey(file);
      if (!known.has(key)) {
        known.add(key);
        next.push(file);
      }
    });

    if (maxFiles != null && next.length > maxFiles) {
      setError(`Можно прикрепить не более ${maxFiles} файлов`);
      return;
    }
    setError(null);
    onChange(next);
  };

  const removeFile = (index: number) => {
    setError(null);
    onChange(files.filter((_, i) => i !== index));
  };

  const handleDragOver = (event: DragEvent) => {
    event.preventDefault();
    if (!disabled) {
      setDragActive(true);
    }
  };

  const handleDragLeave = (event: DragEvent) => {
    event.preventDefault();
    setDragActive(false);
  };

  const handleDrop = (event: DragEvent) => {
    event.preventDefault();
    setDragActive(false);
    if (disabled || event.dataTransfer.files.length === 0) {
      return;
    }
    addFiles(event.dataTransfer.files);
    event.dataTransfer.clearData();
  };

  const openPicker = () => {
    if (!disabled) {
      inputRef.current?.click();
    }
  };

  return (
    <div className="file-upload">
      <label className="file-upload__label" htmlFor={inputId}>{label}</label>
      {error && <span id={`${inputId}-error`} className="field__error" role="alert">{error}</span>}
      <input
        ref={inputRef}
        id={inputId}
        type="file"
        accept={accept}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? `${inputId}-error` : undefined}
        multiple
        className="file-upload__input"
        disabled={disabled}
        onChange={(e) => {
          if (e.target.files) {
            addFiles(e.target.files);
          }
          e.target.value = '';
        }}
      />
      <div
        className={
          dragActive
            ? 'file-upload__dropzone file-upload__dropzone--active'
            : 'file-upload__dropzone'
        }
        onDragOver={handleDragOver}
        onDragLeave={handleDragLeave}
        onDrop={handleDrop}
        onClick={openPicker}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            openPicker();
          }
        }}
        role="button"
        tabIndex={disabled ? -1 : 0}
        aria-disabled={disabled}
      >
        <span className="file-upload__icon" aria-hidden>
          +
        </span>
        <span className="file-upload__hint">
          Перетащите файлы сюда или нажмите для выбора
        </span>
        <span className="file-upload__subhint">Можно выбрать несколько файлов</span>
      </div>
      {files.length > 0 && (
        <ul className="file-upload__list">
          {files.map((file, index) => (
            <li key={fileKey(file)} className="file-upload__item">
              {previews.get(file) && <ImagePreview className="file-upload__preview" src={previews.get(file)!} alt={file.name} />}
              <div className="file-upload__item-info">
                <span className="file-upload__item-name" title={file.name}>
                  {file.name}
                </span>
                <span className="file-upload__item-size">{formatFileSize(file.size)}</span>
              </div>
              <button
                type="button"
                className="file-upload__remove btn btn--ghost"
                onClick={() => removeFile(index)}
                disabled={disabled}
                aria-label={`Удалить ${file.name}`}
              >
                ×
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
