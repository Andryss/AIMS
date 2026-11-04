import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { useDebouncedSearch } from '../hooks/useDebouncedSearch';
import { PickerDrawerShell } from './PickerDrawerShell';

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
  const [selectedAlienId, setSelectedAlienId] = useState<number | null>(null);

  useEffect(() => {
    if (!open) {
      setSearchQuery('');
      setSelectedAlienId(null);
    }
  }, [open]);

  const { results: searchResults, loading: searchLoading } = useDebouncedSearch({
    scopeKey: token, query: searchQuery, enabled: open, retainResults: true,
    load: async (query) => (await api.searchAliens(token, query)).items,
    errorMessage: 'Не удалось найти виды',
  });

  useEffect(() => {
    setSelectedAlienId((previous) => searchQuery.trim().length >= 2 && previous != null
      && searchResults.some((alien) => alien.id === previous) ? previous : null);
  }, [searchResults, searchQuery]);

  const handleConfirm = () => {
    if (selectedAlienId != null) {
      onSelect(selectedAlienId);
    }
  };

  return (
    <PickerDrawerShell
      title="Выбор типа инопланетянина"
      titleId="alien-picker-title"
      open={open}
      selecting={selecting}
      onClose={onClose}
      footer={(
        <div className="drawer__footer">
          <button
            type="button"
            className="btn btn--primary btn--block"
            disabled={selecting || selectedAlienId == null}
            onClick={handleConfirm}
          >
            Выбрать
          </button>
        </div>
      )}
    >
      <div className="drawer__search">
        <label className="drawer__search-box" htmlFor="alien-picker-search">
          <span className="drawer__search-icon" aria-hidden>
            ⌕
          </span>
          <input
            id="alien-picker-search"
            type="search"
            className="drawer__search-input"
            placeholder="Поиск по названию…"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            aria-label="Поиск типа инопланетянина"
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
          {searchResults.map((alien) => {
            const isSelected = alien.id === selectedAlienId;
            return (
              <li key={alien.id}>
                <button
                  type="button"
                  className={`picker-card alien-picker-card${isSelected ? ' picker-card--selected' : ''}`}
                  disabled={selecting}
                  aria-pressed={isSelected}
                  onClick={() => setSelectedAlienId(alien.id)}
                >
                  <div className="picker-card__content">
                    <p className="alien-card__name">{alien.name}</p>
                    <p className="alien-card__meta">Угроза {alien.threatLevel}</p>
                    <p className="alien-card__description">{alien.description}</p>
                  </div>
                </button>
              </li>
            );
          })}
        </ul>
      </div>
    </PickerDrawerShell>
  );
}
