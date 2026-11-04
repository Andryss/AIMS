import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { AlienEditorModal } from './AlienEditorModal';
import { mockAlien } from '../test/testData';

jest.mock('../api/client');
const mockApi = api as jest.Mocked<typeof api>;

beforeEach(() => { jest.clearAllMocks(); URL.createObjectURL = jest.fn(() => 'blob:local'); URL.revokeObjectURL = jest.fn(); });

it('prefills photos and sends an empty array after explicit removal', async () => {
  mockApi.getFilePreviewUrl.mockResolvedValue('blob:old');
  mockApi.batchFiles.mockResolvedValue({ items: [{ id: 8, fileName: 'old.jpg', contentType: 'image/jpeg', fileSize: 3, createdAt: '' }] });
  const onSaved = jest.fn();
  mockApi.updateAlien.mockResolvedValue(mockAlien());
  render(<AlienEditorModal token="tok" open alien={{ ...mockAlien(), photoFileIds: [8] }} onClose={jest.fn()} onSaved={onSaved} />);
  fireEvent.click(await screen.findByRole('button', { name: 'Удалить old.jpg' }));
  fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(mockApi.updateAlien).toHaveBeenCalledWith('tok', 1, expect.objectContaining({ photoFileIds: [] })));
});

it('reuses successful uploads after a save failure', async () => {
  mockApi.uploadFile.mockResolvedValue({ id: 12, fileName: 'photo.png', contentType: 'image/png', fileSize: 5, createdAt: '' });
  mockApi.updateAlien.mockRejectedValueOnce(new Error('Не удалось сохранить')).mockResolvedValueOnce(mockAlien());
  render(<AlienEditorModal token="tok" open alien={mockAlien()} onClose={jest.fn()} onSaved={jest.fn()} />);
  const input = screen.getByLabelText('Фотографии');
  await userEvent.upload(input, new File(['photo'], 'photo.png', { type: 'image/png' }));
  expect(await screen.findByAltText('photo.png')).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await screen.findByRole('alert');
  fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(mockApi.updateAlien).toHaveBeenCalledTimes(2));
  expect(mockApi.uploadFile).toHaveBeenCalledTimes(1);
  expect(mockApi.updateAlien).toHaveBeenLastCalledWith('tok', 1, expect.objectContaining({ photoFileIds: [12] }));
});
