import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import * as api from './api/client';
import { TOKEN_STORAGE_KEY, SYSTEM_NAME } from './constants';
import { mockAlien, mockEquipment, mockEquipmentMovement, mockIncident, mockIncidentList, mockProfile } from './test/testData';

jest.mock('./api/client');

const signIn = api.signIn as jest.MockedFunction<typeof api.signIn>;
const getAuthMe = api.getAuthMe as jest.MockedFunction<typeof api.getAuthMe>;
const getUnreadNotificationsCount = api.getUnreadNotificationsCount as jest.MockedFunction<
  typeof api.getUnreadNotificationsCount
>;
const listIncidents = api.listIncidents as jest.MockedFunction<typeof api.listIncidents>;
const listNotifications = api.listNotifications as jest.MockedFunction<typeof api.listNotifications>;

describe('App', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
    getUnreadNotificationsCount.mockResolvedValue({ count: 0 });
    listIncidents.mockResolvedValue(mockIncidentList({ items: [] }));
    listNotifications.mockResolvedValue({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 });
    (api.listEquipment as jest.Mock).mockResolvedValue({ items: [mockEquipment()], page: 0, size: 10,
      totalElements: 1, totalPages: 1 });
    (api.batchUsers as jest.Mock).mockResolvedValue({ items: [] });
    (api.listEquipmentMovements as jest.Mock).mockResolvedValue({ items: [mockEquipmentMovement()], page: 0,
      size: 10, totalElements: 1, totalPages: 1 });
    (api.getEquipmentMovement as jest.Mock).mockResolvedValue(mockEquipmentMovement());
    (api.getIncident as jest.Mock).mockResolvedValue(mockIncident());
    (api.listEquipmentMovementHistory as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10,
      totalElements: 0, totalPages: 0 });
  });

  it('lands the storekeeper on the warehouse and retains incident navigation', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ login: 'storekeeper', roles: ['STOREKEEPER'],
      permissions: ['INCIDENT_READ', 'EQUIPMENT_READ', 'EQUIPMENT_WRITE'] }));
    render(<MemoryRouter><App /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Склад' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Инциденты' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Добавить оборудование' })).toBeInTheDocument();
  });

  it('restores a direct movement route after loading the profile', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ roles: ['AGENT'], permissions: [
      'INCIDENT_READ', 'EQUIPMENT_READ', 'EQUIPMENT_MOVEMENT_READ',
    ] }));
    render(<MemoryRouter initialEntries={['/equipment-movements/7']}><App /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Заявка #7 — Выдача' })).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'Движение оборудования' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Редактировать заявку' })).not.toBeInTheDocument();
  });

  it.each([
    ['AGENT', ['INCIDENT_READ', 'EQUIPMENT_READ'], true],
    ['CLEANER', ['INCIDENT_READ'], false],
  ])('guards warehouse navigation for %s', async (role, permissions, visible) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ roles: [role as string], permissions: permissions as string[] }));
    render(<MemoryRouter><App /></MemoryRouter>);
    await screen.findByRole('link', { name: 'Инциденты' });
    expect(Boolean(screen.queryByRole('link', { name: 'Склад' }))).toBe(visible);
  });

  it('renders sign in form with system name and fields', () => {
    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );
    expect(screen.getByRole('heading', { name: SYSTEM_NAME })).toBeInTheDocument();
    expect(screen.getByLabelText(/Логин/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/Пароль/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /Войти/i })).toBeInTheDocument();
  });

  it('signs in and shows incidents tab for operator', async () => {
    signIn.mockResolvedValue({ accessToken: 'jwt-token' });
    getAuthMe.mockResolvedValue(mockProfile());

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText(/Логин/i), 'operator');
    await userEvent.type(screen.getByLabelText(/Пароль/i), 'operator');
    await userEvent.click(screen.getByRole('button', { name: /Войти/i }));

    await waitFor(() => {
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('jwt-token');
    });
    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Инциденты' })).toBeInTheDocument();
    });
    await userEvent.click(screen.getByRole('link', { name: 'Инциденты' }));
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инциденты' })).toBeInTheDocument();
    });
    expect(listIncidents).toHaveBeenCalled();
  });

  it('restores session from localStorage', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ login: 'analyst', roles: ['ANALYST'] }));

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(getAuthMe).toHaveBeenCalledWith('saved-token');
    });
    await waitFor(() => {
      expect(screen.getByRole('link', { name: 'Инциденты' })).toBeInTheDocument();
    });
    await userEvent.click(screen.getByRole('link', { name: 'Инциденты' }));
    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Инциденты' })).toBeInTheDocument();
    });
  });

  it('shows sign-in error on failed login', async () => {
    signIn.mockRejectedValueOnce(new Error('Неверный пароль'));

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    await userEvent.type(screen.getByLabelText(/Логин/i), 'operator');
    await userEvent.type(screen.getByLabelText(/Пароль/i), 'wrong');
    await userEvent.click(screen.getByRole('button', { name: /Войти/i }));

    await waitFor(() => {
      expect(screen.getByText('Неверный пароль')).toBeInTheDocument();
    });
  });

  it('opens notifications panel when bell is clicked', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile());
    getUnreadNotificationsCount.mockResolvedValue({ count: 2 });

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Уведомления/ })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /Уведомления/ }));

    await waitFor(() => {
      expect(screen.getByRole('heading', { name: 'Уведомления' })).toBeInTheDocument();
    });
    expect(listNotifications).toHaveBeenCalled();
  });

  it('clears invalid session when profile load fails', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'bad-token');
    getAuthMe.mockRejectedValueOnce(new Error('Unauthorized'));

    render(
      <MemoryRouter>
        <App />
      </MemoryRouter>,
    );

    await waitFor(() => {
      expect(screen.getByRole('button', { name: /Войти/i })).toBeInTheDocument();
    });
    expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
  });
  it('restores a direct species-card URL after loading the profile', async () => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ roles: ['ANALYST'], permissions: ['ALIEN_READ'] }));
    (api.getAlien as jest.Mock).mockResolvedValue(mockAlien());
    render(<MemoryRouter initialEntries={['/aliens/1']}><App /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Слизень' })).toBeInTheDocument();
    expect(api.getAlien).toHaveBeenCalledWith('saved-token', 1);
  });

  it.each([
    ['ANALYST', ['ALIEN_READ', 'ALIEN_WRITE'], true, true],
    ['ADMIN', ['ALIEN_READ', 'ALIEN_WRITE'], true, true],
    ['ANALYST', ['ALIEN_READ'], true, false],
    ['ANALYST', ['ALIEN_WRITE'], false, false],
    ['AGENT', ['ALIEN_READ', 'ALIEN_WRITE'], false, false],
    ['OPERATOR', [], false, false],
  ])('controls knowledge base for %s with %j', async (role, permissions, visible, writable) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ roles: [role as string], permissions: permissions as string[] }));
    (api.listAliens as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10,
      totalElements: 0, totalPages: 0 });
    render(<MemoryRouter initialEntries={['/aliens']}><App /></MemoryRouter>);
    await screen.findByRole('link', { name: /analyst|operator/ });
    if (visible) {
      expect(screen.getByRole('link', { name: 'База знаний' })).toBeInTheDocument();
      expect(await screen.findByRole('heading', { name: 'База знаний' })).toBeInTheDocument();
      expect(Boolean(screen.queryByRole('button', { name: 'Добавить вид' }))).toBe(writable);
    } else {
      expect(screen.queryByRole('link', { name: 'База знаний' })).not.toBeInTheDocument();
      expect(api.listAliens).not.toHaveBeenCalled();
    }
  });

  it.each(['AGENT', 'CLEANER'])('allows %s to read a species card without the knowledge-base tab', async (role) => {
    localStorage.setItem(TOKEN_STORAGE_KEY, 'saved-token');
    getAuthMe.mockResolvedValue(mockProfile({ roles: [role], permissions: ['ALIEN_READ'] }));
    (api.getAlien as jest.Mock).mockResolvedValue(mockAlien());
    render(<MemoryRouter initialEntries={['/aliens/1']}><App /></MemoryRouter>);
    expect(await screen.findByRole('heading', { name: 'Слизень' })).toBeInTheDocument();
    expect(screen.queryByRole('link', { name: 'База знаний' })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Редактировать вид' })).not.toBeInTheDocument();
    expect(screen.getByRole('link', { name: /К инцидентам/ })).toHaveAttribute('href', '/incidents');
  });

});
