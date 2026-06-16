import React from 'react';
import { ROLE_LABELS } from '../constants';
import { AuthMeResponse } from '../types';
import { UserAvatar } from './UserAvatar';

function formatRoles(roles: string[]): string {
  if (roles.length === 0) {
    return '—';
  }
  return roles.map((role) => ROLE_LABELS[role] ?? role).join(', ');
}

interface ProfileTabProps {
  profile: AuthMeResponse;
  onSignOut: () => void;
}

export function ProfileTab({ profile, onSignOut }: ProfileTabProps) {
  return (
    <section className="card">
      <div className="profile-header">
        <UserAvatar login={profile.login} size={80} className="profile-avatar" />
        <div>
          <h2>{profile.login}</h2>
          <p className="profile-header__subtitle">Учётная запись MIB</p>
        </div>
      </div>
      <dl className="profile">
        <div>
          <dt>Логин</dt>
          <dd>{profile.login}</dd>
        </div>
        <div>
          <dt>Роли</dt>
          <dd>{formatRoles(profile.roles)}</dd>
        </div>
        <div>
          <dt>Разрешения</dt>
          <dd>{profile.permissions.join(', ') || '—'}</dd>
        </div>
      </dl>
      <div className="profile__actions">
        <button type="button" className="btn btn--secondary" onClick={onSignOut}>
          Выйти
        </button>
      </div>
    </section>
  );
}
