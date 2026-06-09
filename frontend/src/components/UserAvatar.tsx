import React, { useMemo } from 'react';
import { buildIdenticon } from '../utils/identicon';

interface UserAvatarProps {
  login: string;
  size?: number;
  className?: string;
}

export function UserAvatar({ login, size = 32, className }: UserAvatarProps) {
  const identicon = useMemo(() => buildIdenticon(login), [login]);
  const cellSize = 100 / identicon.gridSize;

  const classes = ['user-avatar', className].filter(Boolean).join(' ');

  return (
    <svg
      className={classes}
      width={size}
      height={size}
      viewBox="0 0 100 100"
      role="img"
      aria-label={`Аватар ${login}`}
    >
      <rect width="100" height="100" fill={identicon.background} />
      {identicon.cells.map((cell) => (
        <rect
          key={`${cell.x}-${cell.y}`}
          x={cell.x * cellSize}
          y={cell.y * cellSize}
          width={cellSize}
          height={cellSize}
          fill={identicon.foreground}
        />
      ))}
    </svg>
  );
}
