import React from 'react';

interface AttachmentDownloadListProps {
  fileIds: number[];
  downloadingId: number | null;
  onDownload: (fileId: number) => void;
  fileLabel?: (fileId: number) => string;
}

function defaultFileLabel(fileId: number): string {
  return `Вложение №${fileId}`;
}

export function AttachmentDownloadList({
  fileIds,
  downloadingId,
  onDownload,
  fileLabel = defaultFileLabel,
}: AttachmentDownloadListProps) {
  if (fileIds.length === 0) {
    return null;
  }

  return (
    <ul className="attachment-list">
      {fileIds.map((fileId) => {
        const busy = downloadingId === fileId;
        return (
          <li key={fileId} className="attachment-card">
            <span className="attachment-card__name">{fileLabel(fileId)}</span>
            <button
              type="button"
              className="attachment-card__download btn btn--outline btn--sm"
              onClick={() => onDownload(fileId)}
              disabled={busy}
              aria-label={`Скачать ${fileLabel(fileId)}`}
            >
              {busy ? '…' : '↓'}
            </button>
          </li>
        );
      })}
    </ul>
  );
}
