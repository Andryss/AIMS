import type { FileUploadResponse } from '../types';
import { API_PREFIX, parseError, requestJson } from './http';
import { readFileBlob, readFileUrl, rememberFile } from '../utils/fileCache';

export async function uploadFile(token: string, file: File): Promise<FileUploadResponse> {
  const remember = rememberFile(token, file);
  const formData = new FormData();
  formData.append('file', file);
  const metadata = await requestJson<FileUploadResponse>(
    '/files',
    {
      method: 'POST',
      body: formData,
    },
    token,
  );
  remember(metadata.id);
  return metadata;
}

export function batchFiles(token: string, ids: number[]): Promise<{ items: FileUploadResponse[] }> {
  return requestJson('/files/batch', { method: 'POST', body: JSON.stringify({ ids }) }, token);
}

async function fetchFileBlob(token: string, fileId: number): Promise<Blob> {
  const response = await fetch(`${API_PREFIX}/files/${fileId}`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok) { throw await parseError(response); }
  return response.blob();
}

export function getFileBlob(token: string, fileId: number): Promise<Blob> {
  return readFileBlob(token, fileId, () => fetchFileBlob(token, fileId));
}

export function getFilePreviewUrl(token: string, fileId: number): Promise<string> {
  return readFileUrl(token, fileId, () => fetchFileBlob(token, fileId));
}

export function uploadFiles(token: string, files: File[]): Promise<FileUploadResponse[]> {
  return Promise.all(files.map((file) => uploadFile(token, file)));
}

export async function downloadFile(
  token: string,
  fileId: number,
  fileName?: string,
): Promise<void> {
  let resolvedName = fileName || `file-${fileId}`;
  const blob = await readFileBlob(token, fileId, async () => {
    const response = await fetch(`${API_PREFIX}/files/${fileId}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    if (!response.ok) { throw await parseError(response); }
    const disposition = response.headers.get('Content-Disposition');
    const match = disposition?.match(/filename="?([^"]+)"?/i);
    resolvedName = match?.[1] || resolvedName;
    return response.blob();
  });

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = resolvedName;
  link.click();
  URL.revokeObjectURL(url);
}
