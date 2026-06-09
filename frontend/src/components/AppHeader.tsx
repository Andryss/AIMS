import React from 'react';
import { MIB_LOGO_PATH } from '../constants';
import { NotificationsPanel } from './NotificationsPanel';
import { UserAvatar } from './UserAvatar';

interface AppHeaderProps {
  token: string;
  login: string;
  unreadCount: number;
  notificationsOpen: boolean;
  showIncidentsTab: boolean;
  profileActive: boolean;
  incidentsActive: boolean;
  onToggleNotifications: () => void;
  onCloseNotifications: () => void;
  onUnreadChange: (count: number) => void;
  onLoginClick: () => void;
  onIncidentsTabClick: () => void;
  onNavigateToIncident?: (incidentId: number) => void;
}

export function AppHeader({
  token,
  login,
  unreadCount,
  notificationsOpen,
  showIncidentsTab,
  profileActive,
  incidentsActive,
  onToggleNotifications,
  onCloseNotifications,
  onUnreadChange,
  onLoginClick,
  onIncidentsTabClick,
  onNavigateToIncident,
}: AppHeaderProps) {
  return (
    <header className="app-header-bar">
      <div className="app-header-brand">
        <img src={MIB_LOGO_PATH} alt="MIB" className="app-logo-image" />
      </div>

      {showIncidentsTab && (
        <nav className="app-header-tabs" aria-label="Разделы">
          <button
            type="button"
            className={incidentsActive ? 'header-tab active' : 'header-tab'}
            onClick={onIncidentsTabClick}
          >
            Инциденты
          </button>
        </nav>
      )}

      <div className="app-header-actions">
        <div className="notifications-wrapper">
          <button
            type="button"
            className="icon-button bell-button"
            onClick={onToggleNotifications}
            aria-label="Уведомления"
          >
            🔔
            {unreadCount > 0 && <span className="badge">{unreadCount}</span>}
          </button>
          <NotificationsPanel
            token={token}
            open={notificationsOpen}
            onClose={onCloseNotifications}
            onUnreadChange={onUnreadChange}
            onNavigateToIncident={onNavigateToIncident}
          />
        </div>
        <button
          type="button"
          className={profileActive ? 'profile-login-btn active' : 'profile-login-btn'}
          onClick={onLoginClick}
        >
          {login && <UserAvatar login={login} size={28} />}
          <span>{login}</span>
        </button>
      </div>
    </header>
  );
}
