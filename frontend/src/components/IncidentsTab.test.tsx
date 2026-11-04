import React from 'react';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockIncident, mockIncidentList } from '../test/testData';
import { IncidentsTab } from './IncidentsTab';

jest.mock('../api/client');

const listIncidents = api.listIncidents as jest.MockedFunction<typeof api.listIncidents>;

describe('IncidentsTab', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    listIncidents.mockResolvedValue(mockIncidentList());
  });

  it('loads and displays incidents', async () => {
    render(
      <MemoryRouter>
        <IncidentsTab token="tok" roles={['OPERATOR']} canCreate canChangeStatus />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(listIncidents).toHaveBeenCalledWith('tok', 0, 10);
    });
    await waitFor(() => {
      expect(screen.getByText('Area 51')).toBeInTheDocument();
    });
    expect(screen.getByRole('button', { name: /Создать инцидент/i })).toBeInTheDocument();
  });

  it('shows error when loading fails', async () => {
    listIncidents.mockRejectedValueOnce(new Error('Network error'));
    render(
      <MemoryRouter>
        <IncidentsTab token="tok" roles={['OPERATOR']} canCreate={false} canChangeStatus={false} />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Network error')).toBeInTheDocument();
    });
  });

  it('ignores an old account response after the token changes', async () => {
    let resolveOld!: (value: ReturnType<typeof mockIncidentList>) => void;
    listIncidents.mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }))
      .mockResolvedValue(mockIncidentList({ items: [mockIncident({ location: 'Current account' })] }));
    const { rerender } = render(<MemoryRouter>
      <IncidentsTab token="old" roles={[]} canCreate={false} canChangeStatus={false} />
    </MemoryRouter>);
    rerender(<MemoryRouter>
      <IncidentsTab token="new" roles={[]} canCreate={false} canChangeStatus={false} />
    </MemoryRouter>);
    await screen.findByText('Current account');
    await act(async () => resolveOld(mockIncidentList({ items: [mockIncident({ location: 'Old account' })] })));
    expect(screen.getByText('Current account')).toBeInTheDocument();
    expect(screen.queryByText('Old account')).not.toBeInTheDocument();
  });

  it('starts on the first page whenever the account changes, including a previous account', async () => {
    listIncidents.mockImplementation(async (_token, page) => mockIncidentList({
      page, totalPages: 2, totalElements: 15,
      items: [mockIncident({ location: `${_token} page ${page}` })],
    }));
    const view = (token: string) => <MemoryRouter>
      <IncidentsTab token={token} roles={[]} canCreate={false} canChangeStatus={false} />
    </MemoryRouter>;
    const { rerender } = render(view('first'));
    await screen.findByText('first page 0');
    await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
    await screen.findByText('first page 1');
    rerender(view('second'));
    await screen.findByText('second page 0');
    rerender(view('first'));
    await screen.findByText('first page 0');
  });

  it('navigates to incident detail on row click', async () => {
    listIncidents.mockResolvedValue(
      mockIncidentList({ items: [mockIncident({ id: 5, location: 'Roswell' })] }),
    );

    render(
      <MemoryRouter initialEntries={['/incidents']}>
        <Routes>
          <Route
            path="/incidents"
            element={
              <IncidentsTab
                token="tok"
                roles={['OPERATOR']}
                canCreate={false}
                canChangeStatus={false}
              />
            }
          />
          <Route path="/incidents/:id" element={<div>Incident detail</div>} />
        </Routes>
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Roswell')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByText('Roswell'));
    expect(screen.getByText('Incident detail')).toBeInTheDocument();
  });

  it('paginates incidents', async () => {
    listIncidents
      .mockResolvedValueOnce(
        mockIncidentList({
          items: [mockIncident({ id: 1 })],
          page: 0,
          totalPages: 2,
          totalElements: 15,
        }),
      )
      .mockResolvedValueOnce(
        mockIncidentList({
          items: [mockIncident({ id: 2, location: 'Page 2' })],
          page: 1,
          totalPages: 2,
          totalElements: 15,
        }),
      );

    render(
      <MemoryRouter>
        <IncidentsTab token="tok" roles={['OPERATOR']} canCreate={false} canChangeStatus={false} />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Area 51')).toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Следующая/i })).not.toBeDisabled();
    });

    await userEvent.click(screen.getByRole('button', { name: /Следующая/i }));

    await waitFor(() => {
      expect(listIncidents).toHaveBeenLastCalledWith('tok', 1, 10);
    });
    await waitFor(() => {
      expect(screen.getByText('Page 2')).toBeInTheDocument();
    });
  });

  it('shows empty state when no incidents', async () => {
    listIncidents.mockResolvedValue(mockIncidentList({ items: [], totalElements: 0, totalPages: 0 }));

    render(
      <MemoryRouter>
        <IncidentsTab token="tok" roles={['OPERATOR']} canCreate={false} canChangeStatus={false} />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByText('Инцидентов пока нет')).toBeInTheDocument();
    });
  });

  it('opens create incident modal', async () => {
    render(
      <MemoryRouter>
        <IncidentsTab token="tok" roles={['OPERATOR']} canCreate canChangeStatus={false} />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Создать инцидент/i })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /Создать инцидент/i }));
    expect(screen.getByRole('heading', { name: 'Создать инцидент' })).toBeInTheDocument();
  });
});
