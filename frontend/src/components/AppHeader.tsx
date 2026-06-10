import React from 'react';
import { NavLink } from 'react-router-dom';
import { MIB_LOGO_PATH } from '../constants';
import { NotificationsPanel } from './NotificationsPanel';
import { UserAvatar } from './UserAvatar';

interface AppHeaderProps {
  token: string;
  login: string;
  unreadCount: number;
  notificationsOpen: boolean;
  showIncidentsTab: boolean;
  onToggleNotifications: () => void;
  onCloseNotifications: () => void;
  onUnreadChange: (count: number) => void;
  onNavigateToIncident?: (incidentId: number) => void;
}

export function AppHeader({
  token,
  login,
  unreadCount,
  notificationsOpen,
  showIncidentsTab,
  onToggleNotifications,
  onCloseNotifications,
  onUnreadChange,
  onNavigateToIncident,
}: AppHeaderProps) {
  return (
    <header className="app-header-bar">
      <div className="app-header-brand">
        <img src={MIB_LOGO_PATH} alt="MIB" className="app-logo-image" />
      </div>

      {showIncidentsTab && (
        <nav className="app-header-tabs" aria-label="Разделы">
          <NavLink
            to="/incidents"
            className={({ isActive }) => (isActive ? 'header-tab active' : 'header-tab')}
          >
            Инциденты
          </NavLink>
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
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            isActive ? 'profile-login-btn active' : 'profile-login-btn'
          }
        >
          {login && <UserAvatar login={login} size={28} />}
          <span>{login}</span>
        </NavLink>
      </div>
    </header>
  );
}
