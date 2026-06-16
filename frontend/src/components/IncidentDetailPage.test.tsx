import React from 'react';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockAlien, mockIncident } from '../test/testData';
import { IncidentDetailPage } from './IncidentDetailPage';

jest.mock('../api/client');

const getIncident = api.getIncident as jest.MockedFunction<typeof api.getIncident>;
const getAlien = api.getAlien as jest.MockedFunction<typeof api.getAlien>;
const searchAliens = api.searchAliens as jest.MockedFunction<typeof api.searchAliens>;
const putIncidentAlien = api.putIncidentAlien as jest.MockedFunction<typeof api.putIncidentAlien>;
const downloadFile = api.downloadFile as jest.MockedFunction<typeof api.downloadFile>;
const listIncidentComments = api.listIncidentComments as jest.MockedFunction<
  typeof api.listIncidentComments
>;
const listIncidentHistory = api.listIncidentHistory as jest.MockedFunction<
  typeof api.listIncidentHistory
>;
const batchUsers = api.batchUsers as jest.MockedFunction<typeof api.batchUsers>;
const getCleanupReport = api.getCleanupReport as jest.MockedFunction<typeof api.getCleanupReport>;

function renderAt(
  path: string,
  options: {
    canReadAliens?: boolean;
    canLinkAlien?: boolean;
    canComment?: boolean;
    canAssign?: boolean;
    canReadCleanupReport?: boolean;
    canCreateCleanupReport?: boolean;
    canChangeCleanupStatus?: boolean;
  } = {},
) {
  const {
    canReadAliens = true,
    canLinkAlien = true,
    canComment = true,
    canAssign = false,
    canReadCleanupReport = false,
    canCreateCleanupReport = false,
    canChangeCleanupStatus = false,
  } = options;
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route
          path="/incidents/:id"
          element={
            <IncidentDetailPage
              token="tok"
              roles={['ANALYST']}
              canChangeStatus
              canReadAliens={canReadAliens}
              canLinkAlien={canLinkAlien}
              canComment={canComment}
              canAssign={canAssign}
              canReadCleanupReport={canReadCleanupReport}
              canCreateCleanupReport={canCreateCleanupReport}
              canChangeCleanupStatus={canChangeCleanupStatus}
            />
          }
        />
      </Routes>
    </MemoryRouter>,
  );
}

describe('IncidentDetailPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    listIncidentComments.mockResolvedValue({
      items: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
    });
    listIncidentHistory.mockResolvedValue({
      items: [],
      page: 0,
      size: 50,
      totalElements: 0,
      totalPages: 0,
    });
    batchUsers.mockResolvedValue({ items: [] });
  });

  it('loads incident and linked alien', async () => {
    getIncident.mockResolvedValue(
      mockIncident({ id: 3, alienId: 1, status: 'READY_FOR_EXECUTION' }),
    );
    getAlien.mockResolvedValue(mockAlien());

    renderAt('/incidents/3');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инцидент #3' })).toBeInTheDocument();
    });
    expect(listIncidentComments).toHaveBeenCalledWith('tok', 3);
    expect(getAlien).toHaveBeenCalledWith('tok', 1);
    expect(screen.getByText('Слизень')).toBeInTheDocument();
  });

  it('loads comments on mount and history lazily on tab switch', async () => {
    getIncident.mockResolvedValue(mockIncident({ id: 7 }));
    listIncidentComments.mockResolvedValue({
      items: [
        {
          id: 10,
          incidentId: 7,
          authorUserId: 2,
          text: 'Проверено',
          createdAt: '2025-06-02T12:00:00Z',
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });
    listIncidentHistory.mockResolvedValue({
      items: [
        {
          id: 1,
          changedAt: '2025-06-01T10:00:00Z',
          changedByUserId: 1,
          snapshot: {
            status: 'DRAFT',
            eventType: 'UNIDENTIFIED_SIGHTING',
            location: 'Test',
            detectedAt: '2025-06-01T10:00:00Z',
            description: 'Desc',
            attachmentFileIds: [],
            alienId: null,
            responsibleUserId: null,
            executorUserIds: [],
          },
        },
      ],
      page: 0,
      size: 50,
      totalElements: 1,
      totalPages: 1,
    });

    batchUsers.mockResolvedValue({ items: [{ id: 2, login: 'analyst' }] });

    renderAt('/incidents/7');

    await waitFor(() => {
      expect(screen.getByText('analyst')).toBeInTheDocument();
      expect(screen.getByText('Проверено')).toBeInTheDocument();
    });
    expect(listIncidentHistory).not.toHaveBeenCalled();

    await userEvent.click(screen.getByRole('tab', { name: 'История' }));

    await waitFor(() => {
      expect(listIncidentHistory).toHaveBeenCalledWith('tok', 7);
    });

    await waitFor(() => {
      expect(screen.getByText(/Создание инцидента/)).toBeInTheDocument();
    });
  });

  it('hides comment form without INCIDENT_COMMENT permission', async () => {
    getIncident.mockResolvedValue(mockIncident({ id: 8 }));

    renderAt('/incidents/8', { canComment: false });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инцидент #8' })).toBeInTheDocument();
    });
    expect(screen.queryByLabelText('Добавить комментарий')).not.toBeInTheDocument();
  });

  it('shows error for invalid id', async () => {
    renderAt('/incidents/abc');
    await waitFor(() => {
      expect(screen.getByText('Некорректный идентификатор инцидента')).toBeInTheDocument();
    });
    expect(getIncident).not.toHaveBeenCalled();
  });

  it('searches and links alien when allowed', async () => {
    getIncident.mockResolvedValue(
      mockIncident({ id: 4, status: 'READY_FOR_ANALYSIS', alienId: undefined }),
    );
    searchAliens.mockResolvedValue({ items: [mockAlien({ id: 2, name: 'Грей' })] });
    putIncidentAlien.mockResolvedValue(
      mockIncident({ id: 4, status: 'READY_FOR_ANALYSIS', alienId: 2 }),
    );
    getAlien.mockResolvedValue(mockAlien({ id: 2, name: 'Грей' }));

    renderAt('/incidents/4');

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инцидент #4' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Выбрать' }));

    await userEvent.type(screen.getByLabelText('Поиск типа инопланетянина'), 'грей');

    await waitFor(() => {
      expect(searchAliens).toHaveBeenCalledWith('tok', 'грей');
    });

    await waitFor(() => {
      expect(screen.getByText('Грей')).toBeInTheDocument();
    });

    const dialog = screen.getByRole('dialog', { name: 'Выбор типа инопланетянина' });
    await userEvent.click(within(dialog).getByRole('button', { name: /Грей/ }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Выбрать' }));

    await waitFor(() => {
      expect(putIncidentAlien).toHaveBeenCalledWith('tok', 4, 2);
    });
    await waitFor(() => {
      expect(getAlien).toHaveBeenCalledWith('tok', 2);
    });
    expect(screen.getByText(/угроза 3\/10/)).toBeInTheDocument();
  });

  it('shows error when incident load fails', async () => {
    getIncident.mockRejectedValueOnce(new Error('Not found'));

    renderAt('/incidents/9');

    await waitFor(() => {
      expect(screen.getByText('Not found')).toBeInTheDocument();
    });
  });

  it('shows error when download fails', async () => {
    downloadFile.mockRejectedValueOnce(new Error('Download failed'));
    getIncident.mockResolvedValue(mockIncident({ id: 2, attachmentFileIds: [15] }));

    renderAt('/incidents/2');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Скачать Вложение №15' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Скачать Вложение №15' }));

    await waitFor(() => {
      expect(screen.getByText('Download failed')).toBeInTheDocument();
    });
  });

  it('does not fetch alien when canReadAliens is false', async () => {
    getIncident.mockResolvedValue(mockIncident({ id: 6, alienId: 1 }));

    renderAt('/incidents/6', { canReadAliens: false });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инцидент #6' })).toBeInTheDocument();
    });
    expect(getAlien).not.toHaveBeenCalled();
    expect(screen.queryByText('Тип инопланетянина')).not.toBeInTheDocument();
  });

  it('ignores load result after unmount', async () => {
    getIncident.mockImplementation(() => new Promise(() => {}));

    const { unmount } = renderAt('/incidents/5');
    unmount();

    expect(screen.queryByText('Не удалось загрузить инцидент')).not.toBeInTheDocument();
  });

  it('shows error when linking alien fails', async () => {
    getIncident.mockResolvedValue(
      mockIncident({ id: 4, status: 'READY_FOR_ANALYSIS', alienId: undefined }),
    );
    searchAliens.mockResolvedValue({ items: [mockAlien({ id: 2, name: 'Грей' })] });
    putIncidentAlien.mockRejectedValueOnce(new Error('Link failed'));

    renderAt('/incidents/4');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Выбрать' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Выбрать' }));
    await userEvent.type(screen.getByLabelText('Поиск типа инопланетянина'), 'грей');

    await waitFor(() => {
      expect(screen.getByText('Грей')).toBeInTheDocument();
    });

    const dialog = screen.getByRole('dialog', { name: 'Выбор типа инопланетянина' });
    await userEvent.click(within(dialog).getByRole('button', { name: /Грей/ }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Выбрать' }));

    await waitFor(() => {
      expect(screen.getByText('Link failed')).toBeInTheDocument();
    });
  });

  it('downloads attachment file', async () => {
    downloadFile.mockResolvedValue(undefined);
    getIncident.mockResolvedValue(mockIncident({ id: 2, attachmentFileIds: [15] }));

    renderAt('/incidents/2');

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Скачать Вложение №15' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Скачать Вложение №15' }));

    await waitFor(() => {
      expect(downloadFile).toHaveBeenCalledWith('tok', 15, 'attachment-15');
    });
  });

  it('loads cleanup report when cleanupReportId is set', async () => {
    getIncident.mockResolvedValue(
      mockIncident({
        id: 11,
        status: 'EXECUTING',
        cleanupReportId: 99,
      }),
    );
    getCleanupReport.mockResolvedValue({
      id: 99,
      incidentId: 11,
      description: 'Убраны следы',
      attachmentFileIds: [3],
      createdByUserId: 5,
      createdAt: '2025-06-03T10:00:00Z',
    });

    renderAt('/incidents/11', { canReadCleanupReport: true });

    await waitFor(() => {
      expect(getCleanupReport).toHaveBeenCalledWith('tok', 11);
    });
    expect(screen.getByText('Отчёт об очистке')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Открыть' })).toBeInTheDocument();
  });

  it('shows attach report button for cleaner on executing incident', async () => {
    getIncident.mockResolvedValue(
      mockIncident({ id: 12, status: 'EXECUTING', cleanupReportId: null }),
    );

    renderAt('/incidents/12', { canCreateCleanupReport: true });

    await waitFor(() => {
      expect(screen.getByRole('button', { name: 'Прикрепить отчёт' })).toBeInTheDocument();
    });
    expect(getCleanupReport).not.toHaveBeenCalled();
  });

  it('hides cleanup block before executing status', async () => {
    getIncident.mockResolvedValue(
      mockIncident({ id: 13, status: 'PREPARED_FOR_EXECUTION' }),
    );

    renderAt('/incidents/13', {
      canReadCleanupReport: true,
      canCreateCleanupReport: true,
      canChangeCleanupStatus: true,
    });

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инцидент #13' })).toBeInTheDocument();
    });
    expect(screen.queryByText('Статус очистки')).not.toBeInTheDocument();
    expect(screen.queryByText('Отчёт об очистке')).not.toBeInTheDocument();
  });
});
