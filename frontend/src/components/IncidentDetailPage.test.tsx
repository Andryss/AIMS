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

function renderAt(
  path: string,
  options: { canReadAliens?: boolean; canLinkAlien?: boolean } = {},
) {
  const { canReadAliens = true, canLinkAlien = true } = options;
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
    expect(getAlien).toHaveBeenCalledWith('tok', 1);
    expect(screen.getByText('Слизень')).toBeInTheDocument();
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
      expect(screen.getByRole('button', { name: 'Файл #15' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Файл #15' }));

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
      expect(screen.getByRole('button', { name: 'Файл #15' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Файл #15' }));

    await waitFor(() => {
      expect(downloadFile).toHaveBeenCalledWith('tok', 15, 'attachment-15');
    });
  });
});
