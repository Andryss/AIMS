import React from 'react';
import { UserAvatar } from './UserAvatar';

interface UserChipProps {
  login: string;
  size?: number;
  removable?: boolean;
  removed?: boolean;
  onRemove?: () => void;
  className?: string;
}

export function UserChip({
  login,
  size = 28,
  removable = false,
  removed = false,
  onRemove,
  className,
}: UserChipProps) {
  const classes = [
    'user-chip',
    removed ? 'user-chip--removed' : '',
    className,
  ].filter(Boolean).join(' ');

  return (
    <span className={classes}>
      <UserAvatar login={login} size={size} className="user-chip-avatar" />
      <span className="user-chip-login">{login}</span>
      {removable && (
        <button
          type="button"
          className="user-chip-remove"
          aria-label={`Убрать ${login}`}
          onClick={(e) => {
            e.stopPropagation();
            onRemove?.();
          }}
        >
          ×
        </button>
      )}
    </span>
  );
}
