import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockIncident } from '../test/testData';
import { CreateIncidentModal } from './CreateIncidentModal';

jest.mock('../api/client');

const uploadFiles = api.uploadFiles as jest.MockedFunction<typeof api.uploadFiles>;
const createIncident = api.createIncident as jest.MockedFunction<typeof api.createIncident>;

describe('CreateIncidentModal', () => {
  const onClose = jest.fn();
  const onCreated = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    uploadFiles.mockResolvedValue([
      {
        id: 10,
        fileName: 'evidence.jpg',
        contentType: 'image/jpeg',
        fileSize: 4,
        createdAt: '2025-06-01T12:00:00Z',
      },
    ]);
    createIncident.mockResolvedValue(mockIncident({ id: 99 }));
  });

  it('does not render when closed', () => {
    const { container } = render(
      <CreateIncidentModal token="tok" open={false} onClose={onClose} onCreated={onCreated} />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('requires attachment before submit', async () => {
    render(<CreateIncidentModal token="tok" open onClose={onClose} onCreated={onCreated} />);

    await userEvent.type(screen.getByLabelText(/Место/i), 'Nevada');
    await userEvent.type(screen.getByLabelText(/Время обнаружения/i), '2025-06-01T12:00');
    await userEvent.type(screen.getByLabelText(/Описание/i), 'Lights in the sky');
    await userEvent.click(screen.getByRole('button', { name: /Создать/i }));

    expect(screen.getByText('Загрузите хотя бы одно вложение перед созданием инцидента')).toBeInTheDocument();
    expect(createIncident).not.toHaveBeenCalled();
  });

  it('creates incident with uploaded files', async () => {
    render(<CreateIncidentModal token="tok" open onClose={onClose} onCreated={onCreated} />);

    const file = new File(['data'], 'evidence.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(input, file);

    await userEvent.type(screen.getByLabelText(/Место/i), 'Roswell');
    fireEvent.change(screen.getByLabelText(/Время обнаружения/i), {
      target: { value: '2025-06-01T12:00' },
    });
    await userEvent.type(screen.getByLabelText(/Описание/i), 'Crash debris');
    await userEvent.click(screen.getByRole('button', { name: /Создать/i }));

    await waitFor(() => {
      expect(uploadFiles).toHaveBeenCalled();
    });
    await waitFor(() => {
      expect(createIncident).toHaveBeenCalled();
    });
    expect(onCreated).toHaveBeenCalledWith(expect.objectContaining({ id: 99 }));
    expect(onClose).toHaveBeenCalled();
  });

  it('shows API error on create failure', async () => {
    createIncident.mockRejectedValueOnce(new Error('Server error'));
    render(<CreateIncidentModal token="tok" open onClose={onClose} onCreated={onCreated} />);

    const file = new File(['data'], 'evidence.jpg', { type: 'image/jpeg' });
    const input = document.querySelector('input[type="file"]') as HTMLInputElement;
    await userEvent.upload(input, file);
    await userEvent.type(screen.getByLabelText(/Место/i), 'Roswell');
    fireEvent.change(screen.getByLabelText(/Время обнаружения/i), {
      target: { value: '2025-06-01T12:00' },
    });
    await userEvent.type(screen.getByLabelText(/Описание/i), 'Crash debris');
    await userEvent.click(screen.getByRole('button', { name: /^Создать$/i }));

    await waitFor(() => {
      expect(screen.getByText('Server error')).toBeInTheDocument();
    });
  });

  it('closes on overlay click', async () => {
    const { container } = render(
      <CreateIncidentModal token="tok" open onClose={onClose} onCreated={onCreated} />,
    );

    const overlay = container.querySelector('.modal-overlay');
    expect(overlay).not.toBeNull();
    await userEvent.click(overlay!);
    expect(onClose).toHaveBeenCalled();
  });
});
