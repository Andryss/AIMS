import React, { useEffect, useMemo, useState } from 'react';
import * as api from '../api/client';
import { UserSummary } from '../types';
import { PickerDrawerShell } from './PickerDrawerShell';
import { UserAvatar } from './UserAvatar';

const EMPTY_EXCLUDE_IDS: number[] = [];

interface UserPickerDrawerBaseProps {
  token: string;
  open: boolean;
  title: string;
  selecting: boolean;
  onClose: () => void;
  /** Уже выбранные пользователи — не показываются в результатах поиска */
  excludeUserIds?: number[];
}

interface SingleUserPickerDrawerProps extends UserPickerDrawerBaseProps {
  multiple?: false;
  onSelect: (user: UserSummary) => void;
  onSelectMany?: never;
}

interface MultiUserPickerDrawerProps extends UserPickerDrawerBaseProps {
  multiple: true;
  onSelectMany: (users: UserSummary[]) => void;
  onSelect?: never;
}

type UserPickerDrawerProps = SingleUserPickerDrawerProps | MultiUserPickerDrawerProps;

export function UserPickerDrawer(props: UserPickerDrawerProps) {
  const {
    token,
    open,
    title,
    selecting,
    onClose,
    excludeUserIds = EMPTY_EXCLUDE_IDS,
    multiple = false,
  } = props;
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<UserSummary[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [selectedUsers, setSelectedUsers] = useState<Map<number, UserSummary>>(() => new Map());

  const excludeKey = excludeUserIds.join(',');
  const excludeSet = useMemo(() => new Set(excludeUserIds), [excludeKey]);

  useEffect(() => {
    if (!open) {
      setSearchQuery('');
      setSearchResults([]);
      setSearchLoading(false);
      setSelectedUserId(null);
      setSelectedUsers(new Map());
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
      if (!multiple) {
        setSelectedUserId(null);
      }
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(() => {
      setSearchLoading(true);
      api
        .searchUsers(token, query, 'AGENT')
        .then((response) => {
          if (!cancelled) {
            const visible = response.items.filter((user) => !excludeSet.has(user.id));
            setSearchResults(visible);
            if (!multiple) {
              setSelectedUserId((prev) => (
                prev != null && visible.some((user) => user.id === prev) ? prev : null
              ));
            }
          }
        })
        .catch(() => {
          if (!cancelled) {
            setSearchResults([]);
            if (!multiple) {
              setSelectedUserId(null);
            }
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
  }, [excludeKey, multiple, open, searchQuery, token]);

  const selectedUser = searchResults.find((user) => user.id === selectedUserId) ?? null;
  const selectedCount = multiple ? selectedUsers.size : (selectedUser ? 1 : 0);

  const toggleMultiSelect = (user: UserSummary) => {
    setSelectedUsers((prev) => {
      const next = new Map(prev);
      if (next.has(user.id)) {
        next.delete(user.id);
      } else {
        next.set(user.id, user);
      }
      return next;
    });
  };

  const handleConfirm = () => {
    if (props.multiple) {
      const users = Array.from(selectedUsers.values());
      if (users.length > 0) {
        props.onSelectMany(users);
      }
      return;
    }
    if (selectedUser) {
      props.onSelect(selectedUser);
    }
  };

  const confirmLabel = multiple
    ? (selectedCount > 0 ? `Добавить (${selectedCount})` : 'Добавить')
    : 'Выбрать';

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
            disabled={selecting || selectedCount === 0}
            onClick={handleConfirm}
          >
            {confirmLabel}
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
        {multiple && selectedCount > 0 && (
          <p className="picker-multi-hint text-muted">
            Выбрано: {selectedCount}. Можно искать и отмечать нескольких агентов.
          </p>
        )}
        {searchLoading && <p className="text-muted">Поиск…</p>}
        {!searchLoading && searchQuery.trim().length < 2 && (
          <p className="text-muted">Введите минимум 2 символа для поиска</p>
        )}
        {!searchLoading && searchQuery.trim().length >= 2 && searchResults.length === 0 && (
          <p className="text-muted">Ничего не найдено</p>
        )}
        <ul className="picker-card-list">
          {searchResults.map((user) => {
            const isSelected = multiple
              ? selectedUsers.has(user.id)
              : user.id === selectedUserId;
            return (
              <li key={user.id}>
                <button
                  type="button"
                  className={`picker-card user-picker-card${isSelected ? ' picker-card--selected' : ''}`}
                  disabled={selecting}
                  aria-pressed={isSelected}
                  onClick={() => (
                    multiple
                      ? toggleMultiSelect(user)
                      : setSelectedUserId(user.id)
                  )}
                >
                  {multiple && (
                    <span className="picker-card__check" aria-hidden>
                      {isSelected ? '✓' : ''}
                    </span>
                  )}
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
