import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import App from './App';
import * as api from './api/client';
import { TOKEN_STORAGE_KEY, SYSTEM_NAME } from './constants';
import { mockIncidentList, mockProfile } from './test/testData';

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
      expect(screen.getByRole('button', { name: 'Уведомления' })).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: 'Уведомления' }));

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
});
