import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import * as api from '../api/client';
import { AttachmentDownloadList } from './AttachmentDownloadList';

jest.mock('../api/client');
const mockApi = api as jest.Mocked<typeof api>;
beforeEach(() => jest.clearAllMocks());

it('batch loads metadata and downloads only images for previews', async () => {
  mockApi.batchFiles.mockResolvedValue({ items: [
    { id: 1, fileName: 'photo.jpg', contentType: 'image/jpeg', fileSize: 10, createdAt: '' },
    { id: 2, fileName: 'report.pdf', contentType: 'application/pdf', fileSize: 30, createdAt: '' },
  ] });
  mockApi.getFilePreviewUrl.mockResolvedValue('blob:photo');
  const download = jest.fn();
  render(<AttachmentDownloadList token="token" fileIds={[1, 2]} downloadingId={null} onDownload={download} />);
  expect(await screen.findByAltText('photo.jpg')).toHaveAttribute('src', 'blob:photo');
  expect(screen.getByText('report.pdf')).toBeInTheDocument();
  expect(mockApi.batchFiles).toHaveBeenCalledWith('token', [1, 2]);
  expect(mockApi.getFilePreviewUrl).toHaveBeenCalledTimes(1);
  expect(mockApi.getFilePreviewUrl).toHaveBeenCalledWith('token', 1);
  fireEvent.click(screen.getByRole('button', { name: 'Скачать report.pdf' }));
  expect(download).toHaveBeenCalledWith(2, 'report.pdf');
});

it('keeps downloads available when metadata or image loading fails', async () => {
  mockApi.batchFiles.mockRejectedValue(new Error('offline'));
  const download = jest.fn();
  render(<AttachmentDownloadList token="token" fileIds={[4]} downloadingId={null} onDownload={download} />);
  await waitFor(() => expect(mockApi.batchFiles).toHaveBeenCalled());
  fireEvent.click(screen.getByRole('button', { name: 'Скачать Вложение №4' }));
  expect(download).toHaveBeenCalledWith(4, undefined);
});
