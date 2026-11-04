import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockIncident, mockMonitoringAlert } from '../test/testData';
import { IncidentEditorModal } from './IncidentEditorModal';

jest.mock('../api/client', () => ({
  ...jest.createMockFromModule<object>('../api/client'),
  ApiError: jest.requireActual('../api/client').ApiError,
  updateIncident: jest.fn(),
}));

const uploadFiles = api.uploadFiles as jest.MockedFunction<typeof api.uploadFiles>;
const createIncident = api.createIncident as jest.MockedFunction<typeof api.createIncident>;

describe('IncidentEditorModal', () => {
  const onClose = jest.fn();
  const onSaved = jest.fn();

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
      <IncidentEditorModal token="tok" open={false} onClose={onClose} onSaved={onSaved} />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('requires attachment before submit', async () => {
    render(<IncidentEditorModal token="tok" open onClose={onClose} onSaved={onSaved} />);

    await userEvent.type(screen.getByLabelText(/Место/i), 'Nevada');
    await userEvent.type(screen.getByLabelText(/Время обнаружения/i), '2025-06-01T12:00');
    await userEvent.type(screen.getByLabelText(/Описание/i), 'Lights in the sky');
    await userEvent.click(screen.getByRole('button', { name: /Создать/i }));

    expect(screen.getByText('Загрузите хотя бы одно вложение перед созданием инцидента')).toBeInTheDocument();
    expect(createIncident).not.toHaveBeenCalled();
  });

  it('creates incident with uploaded files', async () => {
    render(<IncidentEditorModal token="tok" open onClose={onClose} onSaved={onSaved} />);

    const file = new File(['data'], 'evidence.jpg', { type: 'image/jpeg' });
    const input = screen.getByLabelText(/вложения/i);
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
    expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ id: 99 }));
    expect(onClose).toHaveBeenCalled();
  });

  it.each([['', null], ['0', 0], ['3', 3]] as const)(
    'creates an incident with victim count %s', async (value, expected) => {
      render(<IncidentEditorModal token="tok" open onClose={onClose} onSaved={onSaved} />);
      fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value } });
      fireEvent.change(screen.getByLabelText(/Место/i), { target: { value: 'Место' } });
      fireEvent.change(screen.getByLabelText(/Описание/i), { target: { value: 'Описание' } });
      fireEvent.change(screen.getByLabelText(/Время обнаружения/i), { target: { value: '2025-06-01T12:00' } });
      await userEvent.upload(screen.getByLabelText(/вложения/i),
        new File(['image'], 'photo.jpg', { type: 'image/jpeg' }));
      await userEvent.click(screen.getByRole('button', { name: 'Создать' }));
      await waitFor(() => expect(createIncident).toHaveBeenCalledWith('tok', expect.objectContaining({ victimCount: expected })));
    });

  it.each(['-1', '1.5', '2147483648'])('rejects invalid victim count %s before uploading', (value) => {
    render(<IncidentEditorModal token="tok" open onClose={onClose} onSaved={onSaved} />);
    fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value } });
    fireEvent.submit(screen.getByRole('form', { name: 'Создать инцидент' }));
    expect(screen.getByRole('alert')).toHaveTextContent('Укажите целое неотрицательное количество пострадавших');
    expect(uploadFiles).not.toHaveBeenCalled();
    expect(createIncident).not.toHaveBeenCalled();
  });

  it('resets victim count when reopened and offers all ten event types', () => {
    const props = { token: 'tok', onClose, onSaved };
    const { rerender } = render(<IncidentEditorModal {...props} open />);
    fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value: '3' } });
    expect(screen.getAllByRole('option')).toHaveLength(10);
    expect(screen.getByRole('option', { name: 'Нападение на людей' })).toBeInTheDocument();
    rerender(<IncidentEditorModal {...props} open={false} />);
    rerender(<IncidentEditorModal {...props} open />);
    expect(screen.getByLabelText('Количество пострадавших')).toHaveValue(null);
  });

  it('shows API error on create failure', async () => {
    createIncident.mockRejectedValueOnce(new Error('Server error'));
    render(<IncidentEditorModal token="tok" open onClose={onClose} onSaved={onSaved} />);

    const file = new File(['data'], 'evidence.jpg', { type: 'image/jpeg' });
    const input = screen.getByLabelText(/вложения/i);
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
      <IncidentEditorModal
        token="tok"
        open
        onClose={onClose}
        onSaved={onSaved}
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
    render(<IncidentEditorModal token="tok" open onClose={onClose} onSaved={onSaved} />);

    const overlay = screen.getByRole('presentation');
    expect(overlay).not.toBeNull();
    await userEvent.click(overlay!);
    expect(onClose).toHaveBeenCalled();
  });
});

describe('editing incident details', () => {
  const incident = mockIncident({ status: 'CLARIFICATION_REQUIRED', eventType: 'ATTACK_ON_PEOPLE',
    location: 'Старое место', description: 'Старое описание', victimCount: 0,
    detectedAt: '2025-05-01T12:34:56.123456Z', attachmentFileIds: [1], monitoringAlertId: 42 });
  const updateIncident = api.updateIncident as jest.Mock;
  const onSaved = jest.fn();
  const onClose = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    updateIncident.mockResolvedValue({ ...incident, description: 'Уточнение' });
    uploadFiles.mockResolvedValue([{ id: 10, fileName: 'new.jpg', contentType: 'image/jpeg', fileSize: 4,
      createdAt: '2025-06-01T12:00:00Z' }]);
    (api.batchFiles as jest.Mock).mockResolvedValue({ items: [] });
  });

  function edit(extra = {}) {
    return render(<IncidentEditorModal token="tok" open incident={incident} onSaved={onSaved}
      onClose={onClose} {...extra} />);
  }

  it('prefills the current values and saves without new files or losing time precision', async () => {
    edit();
    expect(screen.getByRole('heading', { name: 'Редактировать инцидент' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Тип события/)).toHaveValue('ATTACK_ON_PEOPLE');
    expect(screen.getByLabelText(/Место/)).toHaveValue('Старое место');
    expect(screen.getByLabelText('Количество пострадавших')).toHaveValue(0);
    expect(screen.getByRole('button', { name: 'Скачать Вложение №1' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Удалить Вложение/ })).not.toBeInTheDocument();
    fireEvent.change(screen.getByLabelText(/Описание/), { target: { value: 'Уточнение' } });
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));

    await waitFor(() => expect(onSaved).toHaveBeenCalledWith(expect.objectContaining({ description: 'Уточнение' })));
    expect(updateIncident).toHaveBeenCalledWith('tok', incident.id, {
      eventType: 'ATTACK_ON_PEOPLE', location: 'Старое место', description: 'Уточнение',
      detectedAt: '2025-05-01T12:34:56.123456Z', victimCount: 0, additionalAttachmentFileIds: [],
    });
    expect(uploadFiles).not.toHaveBeenCalled();
    expect(createIncident).not.toHaveBeenCalled();
    expect(onClose).toHaveBeenCalled();
  });

  it.each([['', null], ['0', 0], ['3', 3]] as const)('updates victim count %s', async (value, expected) => {
    edit();
    fireEvent.change(screen.getByLabelText('Количество пострадавших'), { target: { value } });
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(updateIncident).toHaveBeenCalledWith('tok', incident.id,
      expect.objectContaining({ victimCount: expected })));
  });

  it('uploads only newly selected files and converts an edited local time to UTC', async () => {
    edit();
    const file = new File(['photo'], 'new.jpg', { type: 'image/jpeg' });
    await userEvent.upload(screen.getByLabelText(/вложения/i), file);
    fireEvent.change(screen.getByLabelText(/Время обнаружения/), { target: { value: '2025-05-02T15:45' } });
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(updateIncident).toHaveBeenCalledWith('tok', incident.id, expect.objectContaining({
      additionalAttachmentFileIds: [10], detectedAt: new Date('2025-05-02T15:45').toISOString(),
    })));
    expect(uploadFiles).toHaveBeenCalledWith('tok', [file]);
  });

  it('counts existing attachments against the limit', async () => {
    edit({ incident: { ...incident, attachmentFileIds: [1, 2, 3, 4, 5, 6, 7, 8, 9] } });
    await userEvent.upload(screen.getByLabelText(/вложения/i),
      [new File(['a'], 'a.jpg'), new File(['b'], 'b.jpg')]);
    expect(screen.getByRole('alert')).toHaveTextContent('не более 1 файлов');
    expect(uploadFiles).not.toHaveBeenCalled();
  });

  it('resets the draft and new files after cancellation and reopening', async () => {
    const props = { token: 'tok', incident, onSaved, onClose };
    const view = render(<IncidentEditorModal {...props} open />);
    fireEvent.change(screen.getByLabelText(/Описание/), { target: { value: 'Черновик правок' } });
    await userEvent.upload(screen.getByLabelText(/вложения/i), new File(['a'], 'new.jpg'));
    await userEvent.click(screen.getByRole('button', { name: 'Отмена' }));
    expect(updateIncident).not.toHaveBeenCalled();
    view.rerender(<IncidentEditorModal {...props} open={false} />);
    view.rerender(<IncidentEditorModal {...props} open />);
    expect(screen.getByLabelText(/Описание/)).toHaveValue('Старое описание');
    expect(screen.queryByText('new.jpg')).not.toBeInTheDocument();
  });

  it('retains entered data after a failed save', async () => {
    updateIncident.mockRejectedValueOnce(new Error('Нет связи'));
    edit();
    fireEvent.change(screen.getByLabelText(/Описание/), { target: { value: 'Уточнение' } });
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Нет связи');
    expect(screen.getByLabelText(/Описание/)).toHaveValue('Уточнение');
    expect(onSaved).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
  });

  it('reuses uploaded ids when retrying a lost save response', async () => {
    updateIncident.mockRejectedValueOnce(new Error('Ответ потерян'));
    const metadata = { fileName: 'new.jpg', contentType: 'image/jpeg', fileSize: 4, createdAt: '2025-06-01T12:00:00Z' };
    uploadFiles.mockResolvedValueOnce([{ ...metadata, id: 10 }]).mockResolvedValueOnce([{ ...metadata, id: 11 }]);
    edit();
    await userEvent.upload(screen.getByLabelText(/вложения/i),
      new File(['photo'], 'new.jpg', { type: 'image/jpeg' }));
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await screen.findByRole('alert');
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(uploadFiles).toHaveBeenCalledTimes(1);
    expect(updateIncident).toHaveBeenLastCalledWith('tok', incident.id,
      expect.objectContaining({ additionalAttachmentFileIds: [10] }));
  });

  it('counts unique existing ids when accepting another photo', async () => {
    edit({ incident: { ...incident, attachmentFileIds: Array(10).fill(1) } });
    await userEvent.upload(screen.getByLabelText(/вложения/i),
      new File(['photo'], 'new.jpg', { type: 'image/jpeg' }));
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(updateIncident).toHaveBeenCalledWith('tok', incident.id,
      expect.objectContaining({ additionalAttachmentFileIds: [10] })));
  });

  it('applies an editable conflict refresh without discarding the draft or locking file removal', async () => {
    updateIncident.mockRejectedValueOnce(new api.ApiError(409, { humanMessage: 'Повторите сохранение' }));
    (api.getIncident as jest.Mock).mockResolvedValue({ ...incident, status: 'DRAFT',
      detectedAt: '2025-05-03T15:00:00Z', attachmentFileIds: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] });
    uploadFiles.mockResolvedValueOnce([{ id: 20, fileName: 'new.jpg', contentType: 'image/jpeg', fileSize: 4,
      createdAt: '2025-06-01T12:00:00Z' }]);
    function Editor() {
      const [current, setCurrent] = React.useState<typeof incident>({ ...incident, status: 'DRAFT' });
      return <IncidentEditorModal token="tok" open incident={current} onSaved={onSaved} onClose={onClose}
        onConflict={setCurrent} />;
    }
    render(<Editor />);
    fireEvent.change(screen.getByLabelText(/Описание/), { target: { value: 'Несохранённое уточнение' } });
    await userEvent.upload(screen.getByLabelText(/вложения/i),
      new File(['photo'], 'new.jpg', { type: 'image/jpeg' }));
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    expect(await screen.findByRole('button', { name: 'Скачать Вложение №10' })).toBeInTheDocument();
    expect(screen.getByLabelText(/Описание/)).toHaveValue('Несохранённое уточнение');
    expect(screen.getByText('new.jpg')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Удалить new.jpg' })).toBeEnabled();
    expect(onClose).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Удалить new.jpg' }));
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(onSaved).toHaveBeenCalledTimes(1));
    expect(updateIncident).toHaveBeenLastCalledWith('tok', incident.id, expect.objectContaining({
      description: 'Несохранённое уточнение', detectedAt: incident.detectedAt, additionalAttachmentFileIds: [],
    }));
  });

  it('refreshes and closes if the server reports a status conflict', async () => {
    updateIncident.mockRejectedValueOnce(new api.ApiError(409, { humanMessage: 'Уже передан аналитику' }));
    (api.getIncident as jest.Mock).mockResolvedValue({ ...incident, status: 'READY_FOR_ANALYSIS' });
    const onConflict = jest.fn();
    edit({ onConflict });
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(onConflict).toHaveBeenCalledWith(expect.objectContaining({ status: 'READY_FOR_ANALYSIS' })));
    expect(onClose).toHaveBeenCalled();
  });

  it.each([{ token: 'next' }, { incident: { ...incident, id: 999 } }, { open: false }])(
    'ignores a late save when the form scope changes: %j', async (change) => {
      let finish!: (value: typeof incident) => void;
      updateIncident.mockReturnValueOnce(new Promise((resolve) => { finish = resolve; }));
      const props = { token: 'tok', incident, open: true, onSaved, onClose };
      const view = render(<IncidentEditorModal {...props} />);
      await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
      expect(screen.getByRole('button', { name: 'Сохранить' })).toBeDisabled();
      view.rerender(<IncidentEditorModal {...props} {...change} />);
      await act(async () => { finish(incident); });
      expect(onSaved).not.toHaveBeenCalled();
      expect(onClose).not.toHaveBeenCalled();
    });
});
