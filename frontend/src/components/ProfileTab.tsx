import React from 'react';
import { AuthMeResponse } from '../types';
import { UserAvatar } from './UserAvatar';

interface ProfileTabProps {
  profile: AuthMeResponse;
  onSignOut: () => void;
}

export function ProfileTab({ profile, onSignOut }: ProfileTabProps) {
  return (
    <section className="tab-panel">
      <div className="profile-header">
        <UserAvatar login={profile.login} size={80} className="profile-avatar" />
        <div>
          <h2>{profile.login}</h2>
          <p className="profile-subtitle">Учётная запись MIB</p>
        </div>
      </div>
      <dl className="profile">
        <div>
          <dt>Логин</dt>
          <dd>{profile.login}</dd>
        </div>
        <div>
          <dt>Роли</dt>
          <dd>{profile.roles.join(', ') || '—'}</dd>
        </div>
        <div>
          <dt>Разрешения</dt>
          <dd>{profile.permissions.join(', ') || '—'}</dd>
        </div>
      </dl>
      <div className="profile-actions">
        <button type="button" className="secondary" onClick={onSignOut}>
          Выйти
        </button>
      </div>
    </section>
  );
}
