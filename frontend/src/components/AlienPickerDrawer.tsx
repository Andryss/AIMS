import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { Alien } from '../types';

interface AlienPickerDrawerProps {
  token: string;
  open: boolean;
  selecting: boolean;
  onClose: () => void;
  onSelect: (alienId: number) => void;
}

export function AlienPickerDrawer({
  token,
  open,
  selecting,
  onClose,
  onSelect,
}: AlienPickerDrawerProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [searchResults, setSearchResults] = useState<Alien[]>([]);
  const [searchLoading, setSearchLoading] = useState(false);

  useEffect(() => {
    if (!open) {
      setSearchQuery('');
      setSearchResults([]);
      setSearchLoading(false);
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
      return;
    }

    let cancelled = false;
    const timer = window.setTimeout(() => {
      setSearchLoading(true);
      api
        .searchAliens(token, query)
        .then((response) => {
          if (!cancelled) {
            setSearchResults(response.items);
          }
        })
        .catch(() => {
          if (!cancelled) {
            setSearchResults([]);
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

  if (!open) {
    return null;
  }

  return (
    <div className="drawer-overlay" onClick={onClose} role="presentation">
      <div
        className="drawer drawer-bottom"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-labelledby="alien-picker-title"
        aria-modal="true"
      >
        <div className="drawer-content">
          <div className="drawer-header">
            <h2 id="alien-picker-title">Выбор типа инопланетянина</h2>
            <button type="button" className="icon-button" onClick={onClose} aria-label="Закрыть">
              ×
            </button>
          </div>

          <div className="drawer-search">
            <label className="drawer-search-box" htmlFor="alien-picker-search">
              <span className="drawer-search-icon" aria-hidden>
                ⌕
              </span>
              <input
                id="alien-picker-search"
                type="search"
                className="drawer-search-input"
                placeholder="Поиск по названию…"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                aria-label="Поиск типа инопланетянина"
                autoFocus
              />
            </label>
          </div>

          <div className="drawer-body">
            {searchLoading && <p className="panel-muted">Поиск…</p>}
            {!searchLoading && searchQuery.trim().length < 2 && (
              <p className="panel-muted">Введите минимум 2 символа для поиска</p>
            )}
            {!searchLoading && searchQuery.trim().length >= 2 && searchResults.length === 0 && (
              <p className="panel-muted">Ничего не найдено</p>
            )}
            <ul className="alien-card-list">
              {searchResults.map((alien) => (
                <li key={alien.id} className="alien-card">
                  <div className="alien-card-content">
                    <p className="alien-card-name">{alien.name}</p>
                    <p className="alien-card-meta">Угроза {alien.threatLevel}/10</p>
                    <p className="alien-card-description">{alien.description}</p>
                  </div>
                  <button
                    type="button"
                    className="alien-card-select"
                    disabled={selecting}
                    onClick={() => onSelect(alien.id)}
                  >
                    Выбрать
                  </button>
                </li>
              ))}
            </ul>
          </div>
        </div>
      </div>
    </div>
  );
}
