import React from 'react';

interface AttachmentDownloadListProps {
  fileIds: number[];
  downloadingId: number | null;
  onDownload: (fileId: number) => void;
  fileLabel?: (fileId: number) => string;
}

export function AttachmentDownloadList({
  fileIds,
  downloadingId,
  onDownload,
  fileLabel = (fileId) => `Вложение №${fileId}`,
}: AttachmentDownloadListProps) {
  if (fileIds.length === 0) {
    return null;
  }

  return (
    <ul className="attachment-download-list">
      {fileIds.map((fileId) => {
        const busy = downloadingId === fileId;
        return (
          <li key={fileId} className="attachment-download-card">
            <span className="attachment-download-name">{fileLabel(fileId)}</span>
            <button
              type="button"
              className="attachment-download-button"
              disabled={busy}
              onClick={() => onDownload(fileId)}
              aria-label={busy ? `Скачивание ${fileLabel(fileId)}` : `Скачать ${fileLabel(fileId)}`}
            >
              {busy ? '…' : '↓'}
            </button>
          </li>
        );
      })}
    </ul>
  );
}
