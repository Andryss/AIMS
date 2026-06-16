import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { UserSummary } from '../types';
import { PickerDrawerShell } from './PickerDrawerShell';
import { UserAvatar } from './UserAvatar';

interface UserPickerDrawerProps {
  token: string;
  open: boolean;
  title: string;
  selecting: boolean;
  onClose: () => void;
  onSelect: (user: UserSummary) => void;
}

export function UserPickerDrawer({
  token,
  open,
  title,
  selecting,
  onClose,
  onSelect,
}: UserPickerDrawerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);

  useEffect(() => {
    if (!open) {
      setSearchQuery('');
      setSearchResults([]);
      setSearchLoading(false);
      setSelectedUserId(null);
    }
  }, [open]);

  useEffect(() => {
    if (!open) {
      return;
    }

    const query = searchQuery.trim();
    if (query.length < 2) {
      setSearchResults([]);
      setSearchLoading(false);
      setSelectedUserId(null);
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(() => {
      setSearchLoading(true);
      api
        .searchUsers(token, query, 'AGENT')
        .then((response) => {
          if (!cancelled) {
            setSearchResults(response.items);
            setSelectedUserId((prev) => (
              prev != null && response.items.some((user) => user.id === prev) ? prev : null
            ));
          }
        })
        .catch(() => {
          if (!cancelled) {
            setSearchResults([]);
            setSelectedUserId(null);
          }
        })
        .finally(() => {
          if (!cancelled) {
            setSearchLoading(false);
          }
        });
    }, 300);

    return () => {
      cancelled = true;
      window.clearTimeout(timer);
    };
  }, [open, searchQuery, token]);

  const selectedUser = searchResults.find((user) => user.id === selectedUserId) ?? null;

  const handleConfirm = () => {
    if (selectedUser) {
      onSelect(selectedUser);
    }
  };

  return (
    <PickerDrawerShell
      title={title}
      titleId="user-picker-title"
      open={open}
      selecting={selecting}
      onClose={onClose}
      footer={(
        <div className="drawer__footer">
          <button
            type="button"
            className="btn btn--primary btn--block"
            disabled={selecting || selectedUser == null}
            onClick={handleConfirm}
          >
            Выбрать
          </button>
        </div>
      )}
    >
      <div className="drawer__search">
        <label className="drawer__search-box" htmlFor="user-picker-search">
          <span className="drawer__search-icon" aria-hidden>
            ⌕
          </span>
          <input
            id="user-picker-search"
            type="search"
            className="drawer__search-input"
            placeholder="Поиск агента (мин. 2 символа)"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Поиск агента"
            disabled={selecting}
            autoFocus
          />
        </label>
      </div>

      <div className="drawer__body">
        {searchLoading && <p className="text-muted">Поиск…</p>}
        {!searchLoading && searchQuery.trim().length < 2 && (
          <p className="text-muted">Введите минимум 2 символа для поиска</p>
        )}
        {!searchLoading && searchQuery.trim().length >= 2 && searchResults.length === 0 && (
          <p className="text-muted">Ничего не найдено</p>
        )}
        <ul className="picker-card-list">
          {searchResults.map((user) => {
            const isSelected = user.id === selectedUserId;
            return (
              <li key={user.id}>
                <button
                  type="button"
                  className={`picker-card user-picker-card${isSelected ? ' picker-card--selected' : ''}`}
                  disabled={selecting}
                  aria-pressed={isSelected}
                  onClick={() => setSelectedUserId(user.id)}
                >
                  <UserAvatar login={user.login} size={40} />
                  <span className="user-picker-card__login">{user.login}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </PickerDrawerShell>
  );
}
