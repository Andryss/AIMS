import React, { useCallback, useState } from 'react';
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom';
import { AuthProvider } from './auth/AuthContext';
import { deriveAccess } from './auth/access';
import { useAuthSession } from './hooks/useAuthSession';
import { AnalyticsTab } from './components/AnalyticsTab';
import { AppHeader } from './components/AppHeader';
import { AlienKnowledgeBaseTab } from './components/AlienKnowledgeBaseTab';
import { AlienDetailPage } from './components/AlienDetailPage';
import { DetentionRequestsTab } from './components/DetentionRequestsTab';
import { DetentionRequestDetailPage } from './components/DetentionRequestDetailPage';
import { TendersTab } from './components/TendersTab';
import { TenderDetailPage } from './components/TenderDetailPage';
import { EquipmentTab } from './components/EquipmentTab';
import { EquipmentDetailPage } from './components/EquipmentDetailPage';
import { EquipmentMovementsTab } from './components/EquipmentMovementsTab';
import { EquipmentMovementDetailPage } from './components/EquipmentMovementDetailPage';
import { DevStyleguidePage } from './components/DevStyleguidePage';
import { IncidentRoute } from './components/IncidentRoute';
import { IncidentsTab } from './components/IncidentsTab';
import { LoginPage } from './components/LoginPage';
import { MonitoringAlertsTab } from './components/MonitoringAlertsTab';
import { ProfileTab } from './components/ProfileTab';
import { LoadingBlock } from './components/ui/LoadingBlock';

function App() {
  const navigate = useNavigate();
  const session = useAuthSession();
  const { accessToken, profile, error, loading, unreadCount, setUnreadCount } = session;
  const [notificationsOpen, setNotificationsOpen] = useState(false);
  const access = deriveAccess(profile);
  const { showIncidentsTab, canReadAnalytics, canReadDetentions,
    canReadTenders, canCreateTenders, canReadEquipment, canWriteEquipment, canReadMovements,
    canCreateMovements, canProcessMovements, canCreateIncident, canReadMonitoringAlerts,
    canChangeIncidentStatus, canReadAliens, canWriteAliens, showKnowledgeBaseTab, defaultPath } = access;
  const roles = profile?.roles ?? [];
  const currentUserId = profile?.userId;

  const handleSignIn = async (login: string, password: string) => {
    if (await session.signIn(login, password)) { navigate('/'); }
  };
  const handleSignOut = () => {
    session.signOut();
    setNotificationsOpen(false);
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
    <AuthProvider value={{ token: accessToken, profile, access }}>
    <div className="app app--authenticated">
      <a href="#main-content" className="skip-link">
        Перейти к содержимому
      </a>

      <div className="visually-hidden" aria-live="polite" aria-atomic="true">
        {error}
      </div>

      <AppHeader
        unreadCount={unreadCount}
        notificationsOpen={notificationsOpen}
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
              <ProfileTab profile={profile} onSignOut={handleSignOut} />
            }
          />
          {canReadAnalytics && <Route path="/analytics" element={<AnalyticsTab key={accessToken}
            token={accessToken} permissions={profile.permissions} />} />}
          {canReadDetentions && <>
            <Route path="/detention-requests" element={<DetentionRequestsTab token={accessToken}
              canReadIncidents={access.canReadIncidents} />} />
            <Route path="/detention-requests/:id" element={<DetentionRequestDetailPage token={accessToken}
              permissions={profile.permissions} currentUserId={currentUserId} canReadAliens={canReadAliens}
              canReadIncidents={access.canReadIncidents} />} />
          </>}
          {canReadTenders && <>
            <Route path="/tenders" element={<TendersTab token={accessToken} canCreate={canCreateTenders} />} />
            <Route path="/tenders/:id" element={<TenderDetailPage token={accessToken} permissions={profile.permissions}
              currentUserId={currentUserId} canReadIncidents={access.canReadIncidents} canReadEquipment={canReadEquipment} />} />
          </>}
          {canReadEquipment && <>
            <Route path="/equipment" element={<EquipmentTab token={accessToken} canWrite={canWriteEquipment} canCreateTenders={canReadTenders && canCreateTenders} />} />
            <Route path="/equipment/:id" element={<EquipmentDetailPage token={accessToken}
              canWrite={canWriteEquipment} canReadTenders={canReadTenders} canReadMovements={canReadMovements} />} />
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
                  <IncidentRoute />
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
    </AuthProvider>
  );
}

export default App;
