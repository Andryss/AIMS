import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import './App.css';
import * as api from './api/client';
import { AppHeader } from './components/AppHeader';
import { IncidentDetailPage } from './components/IncidentDetailPage';
import { IncidentsTab } from './components/IncidentsTab';
import { LoginPage } from './components/LoginPage';
import { ProfileTab } from './components/ProfileTab';
import { INCIDENTS_TAB_ROLES, TOKEN_STORAGE_KEY } from './constants';
import { AuthMeResponse } from './types';

function App() {
  const navigate = useNavigate();
  const [accessToken, setAccessToken] = useState<string | null>(
    () => localStorage.getItem(TOKEN_STORAGE_KEY),
  );
  const [profile, setProfile] = useState<AuthMeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);

  const showIncidentsTab = useMemo(
    () =>
      profile?.roles.some((role) =>
        INCIDENTS_TAB_ROLES.includes(role as (typeof INCIDENTS_TAB_ROLES)[number]),
      ) ?? false,
    [profile],
  );

  const canCreateIncident = profile?.permissions.includes('INCIDENT_CREATE') ?? false;
  const canChangeIncidentStatus = profile?.permissions.includes('INCIDENT_STATUS_CHANGE') ?? false;
  const canReadAliens = profile?.permissions.includes('ALIEN_READ') ?? false;
  const canLinkAlien = profile?.permissions.includes('INCIDENT_ALIEN_LINK') ?? false;
  const canComment = profile?.permissions.includes('INCIDENT_COMMENT') ?? false;
  const canAssign = profile?.permissions.includes('INCIDENT_ASSIGN') ?? false;
  const roles = profile?.roles ?? [];

  const loadProfile = useCallback(async (token: string) => {
    const me = await api.getAuthMe(token);
    setProfile(me);
    return me;
  }, []);

  const refreshUnreadCount = useCallback(async (token: string) => {
    try {
      const response = await api.getUnreadNotificationsCount(token);
      setUnreadCount(response.count);
    } catch {
      setUnreadCount(0);
    }
  }, []);

  useEffect(() => {
    if (!accessToken) {
      setProfile(null);
      setUnreadCount(0);
      return;
    }

    setLoading(true);
    setError(null);
    Promise.all([loadProfile(accessToken), refreshUnreadCount(accessToken)])
      .catch((err: unknown) => {
        const message = err instanceof Error ? err.message : 'Не удалось загрузить профиль';
        setError(message);
        setAccessToken(null);
        localStorage.removeItem(TOKEN_STORAGE_KEY);
      })
      .finally(() => setLoading(false));
  }, [accessToken, loadProfile, refreshUnreadCount]);

  const handleSignIn = async (login: string, password: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.signIn({ login, password });
      setAccessToken(response.accessToken);
      localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
      navigate('/incidents');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ошибка входа';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleSignOut = () => {
    setAccessToken(null);
    setProfile(null);
    setUnreadCount(0);
    setNotificationsOpen(false);
    localStorage.removeItem(TOKEN_STORAGE_KEY);
    navigate('/');
  };

  const handleNavigateToIncident = useCallback(
    (incidentId: number) => {
      navigate(`/incidents/${incidentId}`);
      setNotificationsOpen(false);
    },
    [navigate],
  );

  if (!accessToken) {
    return (
      <div className="app">
        {error && <div className="alert alert-error app-alert">{error}</div>}
        <LoginPage loading={loading} onSignIn={handleSignIn} />
      </div>
    );
  }

  return (
    <div className="app app-authenticated">
      <AppHeader
        token={accessToken}
        login={profile?.login ?? ''}
        unreadCount={unreadCount}
        notificationsOpen={notificationsOpen}
        showIncidentsTab={showIncidentsTab}
        onToggleNotifications={() => setNotificationsOpen((prev) => !prev)}
        onCloseNotifications={() => setNotificationsOpen(false)}
        onUnreadChange={setUnreadCount}
        onNavigateToIncident={showIncidentsTab ? handleNavigateToIncident : undefined}
      />

      {error && <div className="alert alert-error app-alert">{error}</div>}

      <main className="main-content">
        <Routes>
          <Route
            path="/"
            element={
              showIncidentsTab ? (
                <Navigate to="/incidents" replace />
              ) : (
                <Navigate to="/profile" replace />
              )
            }
          />
          <Route
            path="/profile"
            element={
              profile ? (
                <ProfileTab profile={profile} onSignOut={handleSignOut} />
              ) : (
                <p className="panel-muted">Загрузка…</p>
              )
            }
          />
          {showIncidentsTab && (
            <>
              <Route
                path="/incidents"
                element={
                  <IncidentsTab
                    token={accessToken}
                    roles={roles}
                    canCreate={canCreateIncident}
                    canChangeStatus={canChangeIncidentStatus}
                  />
                }
              />
              <Route
                path="/incidents/:id"
                element={
                  <IncidentDetailPage
                    token={accessToken}
                    roles={roles}
                    canChangeStatus={canChangeIncidentStatus}
                    canReadAliens={canReadAliens}
                    canLinkAlien={canLinkAlien}
                    canComment={canComment}
                    canAssign={canAssign}
                  />
                }
              />
            </>
          )}
          <Route
            path="*"
            element={
              <Navigate to={showIncidentsTab ? '/incidents' : '/profile'} replace />
            }
          />
        </Routes>
      </main>
    </div>
  );
}

export default App;
