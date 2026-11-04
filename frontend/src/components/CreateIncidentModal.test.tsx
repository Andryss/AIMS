import React from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockIncident, mockMonitoringAlert } from '../test/testData';
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

  it.each([['', null], ['0', 0], ['3', 3]] as const)(
    'creates an incident with victim count %s', async (value, expected) => {
      render(<CreateIncidentModal token="tok" open onClose={onClose} onCreated={onCreated} />);
      fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value } });
      fireEvent.change(screen.getByLabelText(/Место/i), { target: { value: 'Место' } });
      fireEvent.change(screen.getByLabelText(/Описание/i), { target: { value: 'Описание' } });
      fireEvent.change(screen.getByLabelText(/Время обнаружения/i), { target: { value: '2025-06-01T12:00' } });
      await userEvent.upload(document.querySelector('input[type="file"]') as HTMLInputElement,
        new File(['image'], 'photo.jpg', { type: 'image/jpeg' }));
      await userEvent.click(screen.getByRole('button', { name: 'Создать' }));
      await waitFor(() => expect(createIncident).toHaveBeenCalledWith('tok', expect.objectContaining({ victimCount: expected })));
    });

  it.each(['-1', '1.5', '2147483648'])('rejects invalid victim count %s before uploading', (value) => {
    const { container } = render(<CreateIncidentModal token="tok" open onClose={onClose} onCreated={onCreated} />);
    fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value } });
    fireEvent.submit(container.querySelector('form')!);
    expect(screen.getByRole('alert')).toHaveTextContent('Укажите целое неотрицательное количество пострадавших');
    expect(uploadFiles).not.toHaveBeenCalled();
    expect(createIncident).not.toHaveBeenCalled();
  });

  it('resets victim count when reopened and offers all ten event types', () => {
    const props = { token: 'tok', onClose, onCreated };
    const { rerender } = render(<CreateIncidentModal {...props} open />);
    fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value: '3' } });
    expect(screen.getAllByRole('option')).toHaveLength(10);
    expect(screen.getByRole('option', { name: 'Нападение на людей' })).toBeInTheDocument();
    rerender(<CreateIncidentModal {...props} open={false} />);
    rerender(<CreateIncidentModal {...props} open />);
    expect(screen.getByLabelText('Количество пострадавших')).toHaveValue(null);
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

  it('shows note that external media are not auto-attached when creating from alert', async () => {
    const alert = mockMonitoringAlert();
    render(
      <CreateIncidentModal
        token="tok"
        open
        onClose={onClose}
        onCreated={onCreated}
        initialValues={{
          eventType: alert.eventType,
          location: alert.location,
          detectedAt: alert.detectedAt,
          description: alert.description,
          monitoringAlertId: alert.id,
          mediaUrls: alert.mediaUrls,
        }}
      />,
    );

    expect(
      screen.getByRole('heading', { name: 'Зарегистрировать инцидент по алерту' }),
    ).toBeInTheDocument();
    expect(
      screen.getByText(/не прикрепляются к инциденту автоматически/i),
    ).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /photo1\.jpg/i })).toBeInTheDocument();
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
