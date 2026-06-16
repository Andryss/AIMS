import React from 'react';
import { NavLink } from 'react-router-dom';
import { MIB_LOGO_PATH } from '../constants';
import { NotificationsPanel } from './NotificationsPanel';
import { UserAvatar } from './UserAvatar';

function BellIcon() {
  return (
    <svg
      className="icon icon--bell"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 9-3 9h18s-3-2-3-9" />
      <path d="M13.73 21a2 2 0 0 1-3.46 0" />
    </svg>
  );
}

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
    <header className="app-header">
      <div className="app-header__brand">
        <img src={MIB_LOGO_PATH} alt="MIB" className="app-header__logo" />
      </div>

      {showIncidentsTab && (
        <nav className="app-header__nav" aria-label="Разделы">
          <NavLink
            to="/incidents"
            className={({ isActive }) =>
              isActive ? 'app-header__tab app-header__tab--active' : 'app-header__tab'
            }
          >
            Инциденты
          </NavLink>
        </nav>
      )}

      <div className="app-header__actions">
        <div className="notifications">
          <button
            type="button"
            className="btn btn--ghost app-header__bell"
            onClick={onToggleNotifications}
            aria-label={`Уведомления${unreadCount > 0 ? `, непрочитанных: ${unreadCount}` : ''}`}
            aria-expanded={notificationsOpen}
          >
            <BellIcon />
            {unreadCount > 0 && <span className="app-header__badge">{unreadCount}</span>}
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
            isActive ? 'app-header__profile app-header__profile--active' : 'app-header__profile'
          }
        >
          {login && <UserAvatar login={login} size={28} />}
          <span>{login}</span>
        </NavLink>
      </div>
    </header>
  );
}
