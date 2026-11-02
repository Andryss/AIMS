import React, { useCallback, useEffect, useMemo, useState } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import * as api from './api/client';
import { AppHeader } from './components/AppHeader';
import { AlienKnowledgeBaseTab } from './components/AlienKnowledgeBaseTab';
import { AlienDetailPage } from './components/AlienDetailPage';
import { EquipmentTab } from './components/EquipmentTab';
import { EquipmentDetailPage } from './components/EquipmentDetailPage';
import { EquipmentMovementsTab } from './components/EquipmentMovementsTab';
import { EquipmentMovementDetailPage } from './components/EquipmentMovementDetailPage';
import { DevStyleguidePage } from './components/DevStyleguidePage';
import { IncidentDetailPage } from './components/IncidentDetailPage';
import { IncidentsTab } from './components/IncidentsTab';
import { LoginPage } from './components/LoginPage';
import { MonitoringAlertsTab } from './components/MonitoringAlertsTab';
import { ProfileTab } from './components/ProfileTab';
import { LoadingBlock } from './components/ui/LoadingBlock';
import { INCIDENTS_TAB_ROLES, KNOWLEDGE_BASE_TAB_ROLES, MONITORING_ALERTS_TAB_ROLES, TOKEN_STORAGE_KEY } from './constants';
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

  const canReadEquipment = profile?.permissions.includes('EQUIPMENT_READ') ?? false;
  const canWriteEquipment = profile?.permissions.includes('EQUIPMENT_WRITE') ?? false;
  const canReadMovements = profile?.permissions.includes('EQUIPMENT_MOVEMENT_READ') ?? false;
  const canCreateMovements = profile?.permissions.includes('EQUIPMENT_MOVEMENT_CREATE') ?? false;
  const canProcessMovements = (profile?.permissions.includes('EQUIPMENT_MOVEMENT_PROCESS') ?? false)
    && (profile?.roles.some((role) => role === 'STOREKEEPER' || role === 'ADMIN') ?? false);

  const canCreateIncident = profile?.permissions.includes('INCIDENT_CREATE') ?? false;
  const canReadMonitoringAlerts = useMemo(
    () =>
      (profile?.permissions.includes('MONITORING_ALERT_READ') ?? false) &&
      (profile?.roles.some((role) =>
        MONITORING_ALERTS_TAB_ROLES.includes(role as (typeof MONITORING_ALERTS_TAB_ROLES)[number]),
      ) ??
        false),
    [profile],
  );
  const canChangeIncidentStatus = profile?.permissions.includes('INCIDENT_STATUS_CHANGE') ?? false;
  const canReadAliens = profile?.permissions.includes('ALIEN_READ') ?? false;
  const canWriteAliens = profile?.permissions.includes('ALIEN_WRITE') ?? false;
  const showKnowledgeBaseTab = canReadAliens && (profile?.roles.some((role) =>
    KNOWLEDGE_BASE_TAB_ROLES.includes(role as (typeof KNOWLEDGE_BASE_TAB_ROLES)[number])) ?? false);
  const canLinkAlien = profile?.permissions.includes('INCIDENT_ALIEN_LINK') ?? false;
  const canComment = profile?.permissions.includes('INCIDENT_COMMENT') ?? false;
  const canAssign = profile?.permissions.includes('INCIDENT_ASSIGN') ?? false;
  const canReadCleanupReport = profile?.permissions.includes('CLEANUP_REPORT_READ') ?? false;
  const canCreateCleanupReport = profile?.permissions.includes('CLEANUP_REPORT_CREATE') ?? false;
  const canChangeCleanupStatus = profile?.permissions.includes('CLEANUP_STATUS_CHANGE') ?? false;
  const roles = profile?.roles ?? [];
  const currentUserId = profile?.userId;
  const defaultPath = roles.includes('STOREKEEPER') && canReadEquipment ? '/equipment'
    : showIncidentsTab ? '/incidents' : '/profile';

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
      navigate('/');
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
        {error && (
          <div className="alert alert--error app__alert" role="alert" aria-live="polite">
            {error}
          </div>
        )}
        <LoginPage loading={loading} onSignIn={handleSignIn} />
      </div>
    );
  }

  if (!profile) {
    return <div className="app"><LoadingBlock label="Загрузка профиля…" /></div>;
  }

  return (
    <div className="app app--authenticated">
      <a href="#main-content" className="skip-link">
        Перейти к содержимому
      </a>

      <div className="visually-hidden" aria-live="polite" aria-atomic="true">
        {error}
      </div>

      <AppHeader
        token={accessToken}
        login={profile?.login ?? ''}
        unreadCount={unreadCount}
        notificationsOpen={notificationsOpen}
        showIncidentsTab={showIncidentsTab}
        showMonitoringAlertsTab={canReadMonitoringAlerts}
        showKnowledgeBaseTab={showKnowledgeBaseTab}
        showEquipmentTab={canReadEquipment}
        showEquipmentMovementsTab={canReadMovements}
        onToggleNotifications={() => setNotificationsOpen((prev) => !prev)}
        onCloseNotifications={() => setNotificationsOpen(false)}
        onUnreadChange={setUnreadCount}
        onNavigateToIncident={showIncidentsTab ? handleNavigateToIncident : undefined}
      />

      {error && (
        <div className="alert alert--error app__alert" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      <main id="main-content" className="main-content">
        <Routes>
          <Route
            path="/"
            element={
              <Navigate to={defaultPath} replace />
            }
          />
          <Route
            path="/profile"
            element={
              profile ? (
                <ProfileTab profile={profile} onSignOut={handleSignOut} />
              ) : (
                <LoadingBlock label="Загрузка профиля…" />
              )
            }
          />
          {canReadEquipment && <>
            <Route path="/equipment" element={<EquipmentTab token={accessToken} canWrite={canWriteEquipment} />} />
            <Route path="/equipment/:id" element={<EquipmentDetailPage token={accessToken}
              canWrite={canWriteEquipment} canReadMovements={canReadMovements} />} />
          </>}
          {canReadMovements && <>
            <Route path="/equipment-movements" element={<EquipmentMovementsTab token={accessToken} />} />
            <Route path="/equipment-movements/:id" element={<EquipmentMovementDetailPage token={accessToken}
              canCreate={canCreateMovements} canProcess={canProcessMovements} currentUserId={currentUserId}
              canReadEquipment={canReadEquipment} />} />
          </>}
          {showKnowledgeBaseTab && (
            <Route path="/aliens" element={<AlienKnowledgeBaseTab token={accessToken} canWrite={canWriteAliens} />} />
          )}
          {canReadAliens && (
            <Route path="/aliens/:id" element={<AlienDetailPage token={accessToken}
              canWrite={canWriteAliens} canBrowseList={showKnowledgeBaseTab} />} />
          )}
          {canReadMonitoringAlerts && (
            <Route
              path="/monitoring/alerts"
              element={
                <MonitoringAlertsTab
                  token={accessToken}
                  canCreate={canCreateIncident}
                />
              }
            />
          )}
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
                    currentUserId={currentUserId}
                    canChangeStatus={canChangeIncidentStatus}
                    canReadAliens={canReadAliens}
                    canLinkAlien={canLinkAlien}
                    canComment={canComment}
                    canAssign={canAssign}
                    canReadCleanupReport={canReadCleanupReport}
                    canCreateCleanupReport={canCreateCleanupReport}
                    canChangeCleanupStatus={canChangeCleanupStatus}
                    canReadEquipmentMovements={canReadMovements}
                    canCreateEquipmentMovements={canCreateMovements}
                  />
                }
              />
            </>
          )}
          {process.env.NODE_ENV === 'development' && (
            <Route path="/dev/ui" element={<DevStyleguidePage />} />
          )}
          <Route
            path="*"
            element={
              <Navigate to={defaultPath} replace />
            }
          />
        </Routes>
      </main>
    </div>
  );
}

export default App;
