import React, { useCallback, useEffect, useMemo, useState } from 'react';
import './App.css';
import * as api from './api/client';
import { AppHeader } from './components/AppHeader';
import { IncidentsTab } from './components/IncidentsTab';
import { LoginPage } from './components/LoginPage';
import { ProfileTab } from './components/ProfileTab';
import { INCIDENTS_TAB_ROLES, TOKEN_STORAGE_KEY } from './constants';
import { AuthMeResponse } from './types';

type ActiveView = 'profile' | 'incidents';

function App() {
  const [accessToken, setAccessToken] = useState<string | null>(
    () => localStorage.getItem(TOKEN_STORAGE_KEY),
  );
  const [profile, setProfile] = useState<AuthMeResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [activeView, setActiveView] = useState<ActiveView>('incidents');
  const [unreadCount, setUnreadCount] = useState(0);
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const [openIncidentId, setOpenIncidentId] = useState<number | null>(null);

  const showIncidentsTab = useMemo(
    () => profile?.roles.some((role) => INCIDENTS_TAB_ROLES.includes(role as typeof INCIDENTS_TAB_ROLES[number])) ?? false,
    [profile],
  );

  const canCreateIncident = profile?.permissions.includes('INCIDENT_CREATE') ?? false;
  const canChangeIncidentStatus = profile?.permissions.includes('INCIDENT_STATUS_CHANGE') ?? false;

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

  useEffect(() => {
    if (!showIncidentsTab && activeView === 'incidents') {
      setActiveView('profile');
    }
  }, [showIncidentsTab, activeView]);

  const handleSignIn = async (login: string, password: string) => {
    setLoading(true);
    setError(null);

    try {
      const response = await api.signIn({ login, password });
      setAccessToken(response.accessToken);
      localStorage.setItem(TOKEN_STORAGE_KEY, response.accessToken);
      setActiveView('incidents');
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
    setOpenIncidentId(null);
    setActiveView('incidents');
    localStorage.removeItem(TOKEN_STORAGE_KEY);
  };

  const handleNavigateToIncident = useCallback((incidentId: number) => {
    setActiveView('incidents');
    setOpenIncidentId(incidentId);
    setNotificationsOpen(false);
  }, []);

  const handleOpenIncidentHandled = useCallback(() => {
    setOpenIncidentId(null);
  }, []);

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
        profileActive={activeView === 'profile'}
        incidentsActive={activeView === 'incidents'}
        onToggleNotifications={() => setNotificationsOpen((prev) => !prev)}
        onCloseNotifications={() => setNotificationsOpen(false)}
        onUnreadChange={setUnreadCount}
        onLoginClick={() => setActiveView('profile')}
        onIncidentsTabClick={() => setActiveView('incidents')}
        onNavigateToIncident={showIncidentsTab ? handleNavigateToIncident : undefined}
      />

      {error && <div className="alert alert-error app-alert">{error}</div>}

      <main className="main-content">
        {activeView === 'profile' && profile && (
          <ProfileTab profile={profile} onSignOut={handleSignOut} />
        )}
        {activeView === 'incidents' && showIncidentsTab && (
          <IncidentsTab
            token={accessToken}
            canCreate={canCreateIncident}
            canChangeStatus={canChangeIncidentStatus}
            openIncidentId={openIncidentId}
            onOpenIncidentHandled={handleOpenIncidentHandled}
          />
        )}
      </main>
    </div>
  );
}

export default App;
