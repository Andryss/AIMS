import React from 'react';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockAlien, mockIncident, mockEquipmentMovement } from '../test/testData';
import { IncidentDetailPage } from './IncidentDetailPage';
import { AlienDetailPage } from './AlienDetailPage';

jest.mock('../api/client', () => ({
  ...jest.createMockFromModule<object>('../api/client'),
  ApiError: jest.requireActual('../api/client').ApiError,
}));

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
    canClassifyAlien?: boolean;
    canCalculateThreat?: boolean;
    canEdit?: boolean;
    canComment?: boolean;
    canAssign?: boolean;
    canReadCleanupReport?: boolean;
    canCreateCleanupReport?: boolean;
    canChangeCleanupStatus?: boolean;
    canReadEquipmentMovements?: boolean;
  } = {},
) {
  const {
    canReadAliens = true,
    canLinkAlien = true,
    canClassifyAlien = false,
    canCalculateThreat = false,
    canEdit = false,
    canComment = true,
    canAssign = false,
    canReadCleanupReport = false,
    canCreateCleanupReport = false,
    canChangeCleanupStatus = false,
    canReadEquipmentMovements = false,
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
              canClassifyAlien={canClassifyAlien}
              canCalculateThreat={canCalculateThreat}
              canEdit={canEdit}
              canComment={canComment}
              canAssign={canAssign}
              canReadCleanupReport={canReadCleanupReport}
              canCreateCleanupReport={canCreateCleanupReport}
              canChangeCleanupStatus={canChangeCleanupStatus}
              canReadEquipmentMovements={canReadEquipmentMovements}
            />
          }
        />
        <Route path="/aliens/:id" element={<AlienDetailPage token="tok" canWrite={false} />} />
      </Routes>
    </MemoryRouter>,
  );
}

it('places detention after equipment and opens the incident request list', async () => {
  getIncident.mockResolvedValue(mockIncident({ id: 8, status: 'EXECUTING', alienId: undefined }));
  batchUsers.mockResolvedValue({ items: [] });
  listIncidentComments.mockResolvedValue({ items: [], page: 0, size: 50, totalPages: 0, totalElements: 0 });
  (api.listDetentionRequests as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10, totalPages: 0, totalElements: 0 });
  render(<MemoryRouter initialEntries={['/incidents/8']}><Routes><Route path="/incidents/:id" element={
    <IncidentDetailPage token="tok" roles={['AGENT']} canChangeStatus={false} canReadAliens={false} canLinkAlien={false}
      canComment={false} canAssign={false} canReadCleanupReport={false} canCreateCleanupReport={false}
      canChangeCleanupStatus={false} canReadEquipmentMovements canReadDetentions canCreateDetentions />
  } /></Routes></MemoryRouter>);
  await screen.findByRole('tab', { name: 'Заключение' });
  expect(screen.getAllByRole('tab').map((tab) => tab.textContent?.trim())).toEqual(['Комментарии', 'История', 'Оборудование', 'Заключение']);
  await userEvent.click(screen.getByRole('tab', { name: 'Заключение' }));
  expect(await screen.findByText('Заявок на заключение пока нет')).toBeInTheDocument();
  expect(api.listDetentionRequests).toHaveBeenCalledWith('tok', { incidentId: 8, page: 0, size: 10 });
});

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

  it('shows equipment after comments and history in its own tab', async () => {
    getIncident.mockResolvedValue(mockIncident({ alienId: undefined }));
    (api.listEquipmentMovements as jest.Mock).mockResolvedValue({ items: [mockEquipmentMovement()],
      page: 0, size: 100, totalElements: 1, totalPages: 1 });
    renderAt('/incidents/1', { canReadEquipmentMovements: true });
    await screen.findByRole('heading', { name: 'Инцидент #1' });
    expect(screen.getAllByRole('tab').map((tab) => tab.textContent)).toEqual(['Комментарии', 'История', 'Оборудование']);
    expect(screen.queryByRole('link', { name: 'Заявка #7' })).not.toBeInTheDocument();

    await userEvent.click(screen.getByRole('tab', { name: 'Оборудование' }));
    expect(await within(screen.getByRole('tabpanel', { name: 'Оборудование' }))
      .findByRole('link', { name: 'Заявка #7' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('tab', { name: 'Комментарии' }));
    expect(screen.queryByRole('link', { name: 'Заявка #7' })).not.toBeInTheDocument();
  });

  it('renders employees with avatars in incident history field changes', async () => {
    getIncident.mockResolvedValue(mockIncident({ alienId: undefined }));
    batchUsers.mockResolvedValue({ items: [{ id: 3, login: 'agent' }, { id: 4, login: 'agent2' },
      { id: 5, login: 'operator' }] });
    listIncidentHistory.mockResolvedValue({ items: [{ id: 1, changedAt: '2026-11-02T12:00:00Z', changedByUserId: 5,
      snapshot: mockIncident({ responsibleUserId: 3, executorUserIds: [4] }) }],
    page: 0, size: 50, totalElements: 1, totalPages: 1 });
    renderAt('/incidents/1');
    await userEvent.click(await screen.findByRole('tab', { name: 'История' }));
    const panel = screen.getByRole('tabpanel', { name: 'История' });
    expect(await within(panel).findByRole('img', { name: 'Аватар agent' })).toBeInTheDocument();
    expect(within(panel).getByRole('img', { name: 'Аватар agent2' })).toBeInTheDocument();
  });


  it('opens the linked species card and returns to the source incident', async () => {
    getIncident.mockResolvedValue(mockIncident({ id: 3, alienId: 1, status: 'READY_FOR_EXECUTION' }));
    getAlien.mockResolvedValue(mockAlien());
    renderAt('/incidents/3');
    const link = await screen.findByRole('link', { name: 'Открыть карточку вида' });
    expect(link).toHaveAttribute('href', '/aliens/1');
    await userEvent.click(link);
    expect(await screen.findByRole('heading', { name: 'Слизень' })).toBeInTheDocument();
    await userEvent.click(screen.getByRole('link', { name: /К инциденту/ }));
    expect(await screen.findByRole('heading', { name: 'Инцидент #3' })).toBeInTheDocument();
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
    expect(screen.getByText(/\(угроза 3\)/)).toBeInTheDocument();
  });

  it('replaces a linked alien during analysis and refreshes loaded history', async () => {
    getIncident.mockResolvedValue(mockIncident({ id: 4, status: 'READY_FOR_ANALYSIS', alienId: 1 }));
    getAlien.mockResolvedValueOnce(mockAlien({ id: 1, name: 'Слизень' }))
      .mockResolvedValue(mockAlien({ id: 2, name: 'Слизистый червь' }));
    searchAliens.mockResolvedValue({ items: [mockAlien({ id: 2, name: 'Слизистый червь' })] });
    putIncidentAlien.mockResolvedValue(mockIncident({ id: 4, status: 'READY_FOR_ANALYSIS', alienId: 2 }));
    renderAt('/incidents/4');

    await screen.findByText('Слизень');
    await userEvent.click(screen.getByRole('tab', { name: 'История' }));
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(1));
    await screen.findByText('История изменений пуста');
    await userEvent.click(screen.getByRole('button', { name: 'Изменить вид' }));
    await userEvent.type(screen.getByLabelText('Поиск типа инопланетянина'), 'червь');
    const dialog = screen.getByRole('dialog', { name: 'Выбор типа инопланетянина' });
    await userEvent.click(await within(dialog).findByRole('button', { name: /Слизистый червь/ }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Выбрать' }));

    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(putIncidentAlien).toHaveBeenCalledWith('tok', 4, 2);
    expect(screen.getByText('Слизистый червь')).toBeInTheDocument();
    expect(screen.queryByText('Слизень')).not.toBeInTheDocument();
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(2));
  });

  it.each([
    ['READY_FOR_EXECUTION', true],
    ['EXECUTION_COMPLETED', true],
    ['READY_FOR_ANALYSIS', false],
  ] as const)('prevents replacing alien in %s with link permission %s', async (status, canLinkAlien) => {
    getIncident.mockResolvedValue(mockIncident({ id: 4, status, alienId: 1 }));
    getAlien.mockResolvedValue(mockAlien());
    renderAt('/incidents/4', { canLinkAlien });
    await screen.findByText('Слизень');
    expect(screen.queryByRole('button', { name: 'Изменить вид' })).not.toBeInTheDocument();
    expect(putIncidentAlien).not.toHaveBeenCalled();
  });

  it('keeps the original alien and shows an error if replacement fails', async () => {
    getIncident.mockResolvedValue(mockIncident({ id: 4, status: 'READY_FOR_ANALYSIS', alienId: 1 }));
    getAlien.mockResolvedValue(mockAlien({ id: 1, name: 'Слизень' }));
    searchAliens.mockResolvedValue({ items: [mockAlien({ id: 2, name: 'Слизистый червь' })] });
    putIncidentAlien.mockRejectedValueOnce(new Error('Не удалось сохранить классификацию'));
    renderAt('/incidents/4');
    await screen.findByText('Слизень');
    await userEvent.click(screen.getByRole('button', { name: 'Изменить вид' }));
    await userEvent.type(screen.getByLabelText('Поиск типа инопланетянина'), 'червь');
    const dialog = screen.getByRole('dialog', { name: 'Выбор типа инопланетянина' });
    await userEvent.click(await within(dialog).findByRole('button', { name: /Слизистый червь/ }));
    await userEvent.click(within(dialog).getByRole('button', { name: 'Выбрать' }));
    await screen.findByText('Не удалось сохранить классификацию');
    expect(screen.getByText('Слизень')).toBeInTheDocument();
    expect(screen.getByRole('dialog')).toBeInTheDocument();
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


it('keeps the linked type compact and accepts the inline classified type with history refresh', async () => {
  jest.clearAllMocks();
  getIncident.mockResolvedValue(mockIncident({ id: 8, status: 'READY_FOR_ANALYSIS', alienId: 1 }));
  getAlien.mockResolvedValue(mockAlien({ id: 1, name: 'Слизень', description: 'Очень большое описание исходного вида' }));
  const candidate = mockAlien({ id: 6, name: 'Ашта', threatLevel: 9, description: 'Подробное описание ашта' });
  (api.classifyIncidentAlien as jest.Mock).mockResolvedValue({ status: 'MATCH', confidence: 95.6, alien: candidate });
  putIncidentAlien.mockResolvedValue(mockIncident({ id: 8, status: 'READY_FOR_ANALYSIS', alienId: 6 }));
  listIncidentComments.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
  listIncidentHistory.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
  batchUsers.mockResolvedValue({ items: [] });
  renderAt('/incidents/8', { canClassifyAlien: true });
  await screen.findByText('Слизень');
  expect(screen.queryByText('Очень большое описание исходного вида')).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('tab', { name: 'История' }));
  await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(1));
  await userEvent.click(screen.getByRole('button', { name: 'Распознать вид' }));
  await screen.findByText('95,6%');
  expect(screen.getByText('Слизень')).toBeInTheDocument();
  expect(putIncidentAlien).not.toHaveBeenCalled();
  await userEvent.click(screen.getByRole('button', { name: 'Принять' }));
  await waitFor(() => expect(screen.queryByText('Слизень')).not.toBeInTheDocument());
  expect(screen.getByText('Ашта')).toBeInTheDocument();
  await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(2));
});

it.each([
  ['READY_FOR_ANALYSIS', false], ['EXECUTING', true],
] as const)('hides classification for status=%s permission=%s', async (status, permission) => {
  jest.clearAllMocks();
  getIncident.mockResolvedValue(mockIncident({ status, alienId: 1 }));
  getAlien.mockResolvedValue(mockAlien());
  renderAt('/incidents/1', { canClassifyAlien: permission });
  await screen.findByRole('heading', { name: 'Инцидент #1' });
  expect(screen.queryByRole('button', { name: 'Распознать вид' })).not.toBeInTheDocument();
});

describe('incident threat assessment', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    listIncidentComments.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    listIncidentHistory.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    batchUsers.mockResolvedValue({ items: [] });
  });

  it.each([[null, 'Неизвестно'], [0, '0'], [2, '2']] as const)(
    'displays victim count %s without an edit action', async (victimCount, expected) => {
      getIncident.mockResolvedValue(mockIncident({ victimCount, threatLevel: 6 }));
      renderAt('/incidents/1');
      const label = await screen.findByText('Количество пострадавших');
      expect(within(label.closest('.data-list__row') as HTMLElement).getByText(expected)).toBeInTheDocument();
      const threat = screen.getByText('Уровень угрозы').closest('.data-list__row') as HTMLElement;
      expect(within(threat).getByText('6')).toBeInTheDocument();
      expect(within(threat).queryByRole('button')).not.toBeInTheDocument();
    });

  it('refreshes the saved value and history only after accepting the proposal', async () => {
    const incident = mockIncident({ id: 8, status: 'READY_FOR_ANALYSIS', eventType: 'ATTACK_ON_PEOPLE',
      victimCount: 2, threatLevel: 3, alienId: 6 });
    getIncident.mockResolvedValue(incident);
    getAlien.mockResolvedValue(mockAlien({ id: 6, threatLevel: 4 }));
    (api.calculateIncidentThreat as jest.Mock).mockResolvedValue({ calculatedThreatLevel: 7, contributions: [
      { criterion: 'ALIEN_THREAT', adjustment: 4 }, { criterion: 'EVENT_TYPE', adjustment: 2 },
      { criterion: 'VICTIMS', adjustment: 1 },
    ] });
    (api.setIncidentThreatLevel as jest.Mock).mockResolvedValue({ ...incident, threatLevel: 8 });
    renderAt('/incidents/8', { canCalculateThreat: true });
    await screen.findByRole('button', { name: 'Рассчитать угрозу' });
    await userEvent.click(screen.getByRole('tab', { name: 'История' }));
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(1));
    await userEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
    await screen.findByLabelText('Уровень угрозы');
    await userEvent.click(screen.getByRole('button', { name: 'Увеличить угрозу' }));
    expect(api.setIncidentThreatLevel).not.toHaveBeenCalled();
    await userEvent.click(screen.getByRole('button', { name: 'Принять' }));
    await waitFor(() => expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument());
    const threat = screen.getByText('Уровень угрозы').closest('.data-list__row') as HTMLElement;
    expect(within(threat).getByText('8')).toBeInTheDocument();
    expect(api.setIncidentThreatLevel).toHaveBeenCalledWith('tok', 8, 8);
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(2));
  });

  it('does not offer calculation for a completed incident', async () => {
    getIncident.mockResolvedValue(mockIncident({ status: 'EXECUTION_COMPLETED', threatLevel: 6 }));
    renderAt('/incidents/1', { canCalculateThreat: true });
    await screen.findByRole('heading', { name: 'Инцидент #1' });
    expect(screen.queryByRole('button', { name: 'Рассчитать угрозу' })).not.toBeInTheDocument();
  });
});

describe('threat history refresh', () => {
  const snapshot = { status: 'READY_FOR_ANALYSIS' as const, eventType: 'CONTACT_SUSPECT' as const,
    location: 'Полигон', detectedAt: '2025-06-01T12:00:00Z', description: 'До расчёта',
    attachmentFileIds: [], executorUserIds: [], victimCount: 0, threatLevel: 3 };
  const record = (id: number, description = snapshot.description, threatLevel = 3) => ({
    id, changedByUserId: 1, changedAt: `2025-06-01T12:00:${String(id % 60).padStart(2, '0')}Z`,
    snapshot: { ...snapshot, description, threatLevel },
  });
  const page = (items: ReturnType<typeof record>[], totalPages = 1, index = 0) => ({
    items, page: index, size: 50, totalElements: totalPages === 2 ? 51 : items.length, totalPages,
  });

  beforeEach(() => {
    jest.clearAllMocks();
    getIncident.mockResolvedValue(mockIncident({ ...snapshot, id: 8 }));
    listIncidentComments.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    batchUsers.mockResolvedValue({ items: [] });
    (api.calculateIncidentThreat as jest.Mock).mockResolvedValue({ calculatedThreatLevel: 7, contributions: [] });
    (api.setIncidentThreatLevel as jest.Mock).mockResolvedValue(mockIncident({ ...snapshot, id: 8, threatLevel: 7 }));
  });

  async function calculateAndAccept() {
    await userEvent.click(screen.getByRole('button', { name: 'Рассчитать угрозу' }));
    await screen.findByLabelText('Уровень угрозы');
    await userEvent.click(screen.getByRole('button', { name: 'Принять' }));
  }

  it('does not let a delayed history request replace the post-accept history', async () => {
    let release!: (value: ReturnType<typeof page>) => void;
    const fresh = page([record(1), record(2, 'Свежая история после оценки', 7)]);
    listIncidentHistory.mockReturnValueOnce(new Promise((resolve) => { release = resolve; }))
      .mockResolvedValue(fresh);
    renderAt('/incidents/8', { canCalculateThreat: true });
    await screen.findByRole('button', { name: 'Рассчитать угрозу' });
    await userEvent.click(screen.getByRole('tab', { name: 'История' }));
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(1));
    await calculateAndAccept();
    await screen.findByText('Свежая история после оценки', { exact: false });
    await act(async () => { release(page([record(1)])); });
    expect(screen.getByText('Свежая история после оценки', { exact: false })).toBeInTheDocument();
  });

  it('loads the accepted entry beyond the first history page', async () => {
    const first = Array.from({ length: 50 }, (_,index) => record(index + 1));
    listIncidentHistory.mockResolvedValueOnce(page(first))
      .mockResolvedValueOnce(page(first, 2))
      .mockResolvedValueOnce(page([record(51, 'Оценка на второй странице', 7)], 2, 1));
    renderAt('/incidents/8', { canCalculateThreat: true });
    await screen.findByRole('button', { name: 'Рассчитать угрозу' });
    await userEvent.click(screen.getByRole('tab', { name: 'История' }));
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(1));
    await calculateAndAccept();
    await screen.findByText('Оценка на второй странице', { exact: false });
    expect(listIncidentHistory).toHaveBeenLastCalledWith('tok', 8, 1, 50);
  });

  it('refreshes the card and removes calculation actions after a completion conflict', async () => {
    listIncidentHistory.mockResolvedValue(page([]));
    (api.setIncidentThreatLevel as jest.Mock).mockRejectedValueOnce(new api.ApiError(409, { humanMessage: 'Инцидент завершён' }));
    getIncident.mockResolvedValueOnce(mockIncident({ ...snapshot, id: 8 }))
      .mockResolvedValueOnce(mockIncident({ ...snapshot, id: 8, status: 'EXECUTION_COMPLETED', threatLevel: 6 }));
    renderAt('/incidents/8', { canCalculateThreat: true });
    await screen.findByRole('button', { name: 'Рассчитать угрозу' });
    await calculateAndAccept();
    const threat = screen.getByText('Уровень угрозы').closest('.data-list__row') as HTMLElement;
    await within(threat).findByText('6');
    expect(screen.queryByRole('button', { name: 'Рассчитать угрозу' })).not.toBeInTheDocument();
  });
});


describe('incident clarification', () => {
  const incident = mockIncident({ id: 8, status: 'DRAFT', description: 'Исходное описание',
    location: 'Исходное место', victimCount: null, attachmentFileIds: [1] });

  beforeEach(() => {
    jest.clearAllMocks();
    getIncident.mockResolvedValue(incident);
    listIncidentComments.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    listIncidentHistory.mockResolvedValue({ items: [], page: 0, size: 50, totalElements: 0, totalPages: 0 });
    batchUsers.mockResolvedValue({ items: [] });
    (api.batchFiles as jest.Mock).mockResolvedValue({ items: [] });
    (api.updateIncident as jest.Mock).mockResolvedValue({ ...incident, description: 'Уточнённое описание',
      updatedAt: '2025-06-02T12:00:00Z' });
  });

  it.each(['DRAFT', 'CLARIFICATION_REQUIRED'] as const)('opens the editor in %s and updates the card', async (status) => {
    getIncident.mockResolvedValue({ ...incident, status });
    (api.updateIncident as jest.Mock).mockResolvedValue({ ...incident, status, description: 'Уточнённое описание' });
    renderAt('/incidents/8', { canEdit: true });
    await userEvent.click(await screen.findByRole('button', { name: 'Редактировать инцидент' }));
    const editor = screen.getByRole('dialog', { name: 'Редактировать инцидент' });
    expect(within(editor).getByLabelText(/Описание/)).toHaveValue('Исходное описание');
    fireEvent.change(within(editor).getByLabelText(/Описание/), { target: { value: 'Уточнённое описание' } });
    await userEvent.click(within(editor).getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.getByText('Уточнённое описание')).toBeInTheDocument();
  });

  it.each([
    { status: 'DRAFT', canEdit: false }, { status: 'READY_FOR_ANALYSIS', canEdit: true },
    { status: 'READY_FOR_EXECUTION', canEdit: true }, { status: 'REANALYSIS_REQUIRED', canEdit: true },
    { status: 'PREPARATION_FOR_EXECUTION', canEdit: true }, { status: 'PREPARED_FOR_EXECUTION', canEdit: true },
    { status: 'EXECUTING', canEdit: true }, { status: 'EXECUTION_COMPLETED', canEdit: true },
  ] as const)('hides the editor outside its permission or statuses: %j', async ({ status, canEdit }) => {
    getIncident.mockResolvedValue({ ...incident, status });
    renderAt('/incidents/8', { canEdit });
    await screen.findByText('Исходное описание');
    expect(screen.queryByRole('button', { name: 'Редактировать инцидент' })).not.toBeInTheDocument();
  });

  it('reloads the opened history after saving and discards the threat proposal', async () => {
    (api.calculateIncidentThreat as jest.Mock).mockResolvedValue({ calculatedThreatLevel: 7, contributions: [] });
    renderAt('/incidents/8', { canEdit: true, canCalculateThreat: true });
    await userEvent.click(await screen.findByRole('button', { name: 'Рассчитать угрозу' }));
    await screen.findByRole('button', { name: 'Принять' });
    await userEvent.click(screen.getByRole('tab', { name: 'История' }));
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(1));
    await userEvent.click(screen.getByRole('button', { name: 'Редактировать инцидент' }));
    fireEvent.change(screen.getByLabelText(/Описание/), { target: { value: 'Уточнённое описание' } });
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await screen.findByText('Уточнённое описание');
    await waitFor(() => expect(listIncidentHistory).toHaveBeenCalledTimes(2));
    expect(screen.queryByRole('button', { name: 'Принять' })).not.toBeInTheDocument();
  });

  it('closes the editor and refreshes the card after a status conflict', async () => {
    (api.updateIncident as jest.Mock).mockRejectedValueOnce(new api.ApiError(409, { humanMessage: 'Передан аналитику' }));
    getIncident.mockResolvedValueOnce(incident).mockResolvedValueOnce({ ...incident, status: 'READY_FOR_ANALYSIS' });
    renderAt('/incidents/8', { canEdit: true });
    await userEvent.click(await screen.findByRole('button', { name: 'Редактировать инцидент' }));
    await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
    await waitFor(() => expect(screen.queryByRole('dialog')).not.toBeInTheDocument());
    expect(screen.queryByRole('button', { name: 'Редактировать инцидент' })).not.toBeInTheDocument();
    expect(getIncident).toHaveBeenCalledTimes(2);
  });

  it('still offers comments after completion', async () => {
    getIncident.mockResolvedValue({ ...incident, status: 'EXECUTION_COMPLETED' });
    renderAt('/incidents/8', { canEdit: true, canComment: true });
    expect(await screen.findByRole('button', { name: 'Отправить' })).toBeInTheDocument();
    expect(screen.getByLabelText('Добавить комментарий')).toBeInTheDocument();
  });
});
