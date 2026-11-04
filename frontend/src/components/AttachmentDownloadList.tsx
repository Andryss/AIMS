import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { FileUploadResponse } from '../types';
import { ImagePreview } from './ImagePreview';

interface AttachmentDownloadListProps {
  token?: string;
  fileIds: number[];
  downloadingId: number | null;
  onDownload: (fileId: number, fileName?: string) => void;
  onRemove?: (fileId: number) => void;
  disabled?: boolean;
  fileLabel?: (fileId: number) => string;
}

function defaultFileLabel(fileId: number): string {
  return `Вложение №${fileId}`;
}

export function AttachmentDownloadList({
  token,
  fileIds,
  downloadingId,
  onDownload,
  onRemove,
  disabled = false,
  fileLabel = defaultFileLabel,
}: AttachmentDownloadListProps) {
  const [metadata, setMetadata] = useState<Map<number, FileUploadResponse>>(new Map());
  const [previews, setPreviews] = useState<Map<number, string>>(new Map());
  const idsKey = Array.from(new Set(fileIds)).join(',');

  useEffect(() => {
    let cancelled = false;
    setMetadata(new Map());
    setPreviews(new Map());
    if (!token || !idsKey) { return; }
    const ids = idsKey.split(',').map(Number);
    const load = async () => {
      try {
        const items: FileUploadResponse[] = [];
        for (let start = 0; start < ids.length; start += 100) {
          const result = await api.batchFiles(token, ids.slice(start, start + 100));
          items.push(...result.items);
        }
        if (cancelled) { return; }
        setMetadata(new Map(items.map((file) => [file.id, file])));
        await Promise.all(items.filter((file) => file.contentType.toLowerCase().startsWith('image/'))
          .map(async (file) => {
            try {
              const url = await api.getFilePreviewUrl(token, file.id);
              if (!cancelled) { setPreviews((previous) => new Map(previous).set(file.id, url)); }
            } catch { /* Исходник остаётся доступен для отдельной попытки скачивания. */ }
          }));
      } catch { /* Список id остаётся доступен, даже если метаданные временно недоступны. */ }
    };
    load();
    return () => { cancelled = true; };
  }, [token, idsKey]);

  if (fileIds.length === 0) { return null; }

  return (
    <ul className="attachment-list">
      {fileIds.map((fileId) => {
        const busy = downloadingId === fileId;
        const name = metadata.get(fileId)?.fileName ?? fileLabel(fileId);
        const preview = previews.get(fileId);
        return (
          <li key={fileId} className="attachment-card">
            {preview ? <ImagePreview className="attachment-card__preview" src={preview} alt={name}
              onError={() => setPreviews((previous) => {
                const next = new Map(previous); next.delete(fileId); return next;
              })} /> : <span className="attachment-card__placeholder" aria-label="Превью недоступно">▧</span>}
            <span className="attachment-card__name" title={name}>{name}</span>
            <button type="button" className="attachment-card__download btn btn--outline btn--sm"
              onClick={() => onDownload(fileId, metadata.get(fileId)?.fileName)}
              disabled={busy || disabled} aria-label={`Скачать ${name}`}>
              {busy ? '…' : '↓'}
            </button>
            {onRemove && <button type="button" className="btn btn--ghost" disabled={disabled}
              aria-label={`Удалить ${name}`} onClick={() => onRemove(fileId)}>×</button>}
          </li>
        );
      })}
    </ul>
  );
}
