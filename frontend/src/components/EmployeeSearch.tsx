import React, { useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { UserSummary } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';
import { UserChip } from './UserChip';

interface EmployeeSearchProps {
  token: string;
  label: string;
  value: UserSummary | null;
  onChange: (user: UserSummary | null) => void;
  disabled?: boolean;
  required?: boolean;
}

export function EmployeeSearch({ token, label, value, onChange, disabled, required }: EmployeeSearchProps) {
  const [query, setQuery] = useState(value?.login ?? '');
  const [results, setResults] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const editingSelection = useRef(false);

  const selectedId = value?.id;
  const selectedLogin = value?.login;

  useEffect(() => {
    if (selectedId != null && selectedLogin) { setQuery(selectedLogin); }
    else if (!editingSelection.current) { setQuery(''); }
    editingSelection.current = false;
  }, [selectedId, selectedLogin]);

  useEffect(() => {
    setResults([]);
    setError(null);
    setLoading(false);
    if (disabled || query.trim().length < 2 || query === selectedLogin) { return; }

    let cancelled = false;
    const timer = window.setTimeout(() => {
      setLoading(true);
      api.searchUsers(token, query.trim())
        .then((response) => { if (!cancelled) { setResults(response.items); } })
        .catch((err: unknown) => {
          if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось найти сотрудников'); }
        })
        .finally(() => { if (!cancelled) { setLoading(false); } });
    }, 300);

    return () => { cancelled = true; window.clearTimeout(timer); };
  }, [token, query, disabled, selectedLogin]);

  return (
    <div className="employee-search">
      <FormField label={label} required={required}>
        <input type="search" value={query} disabled={disabled} autoComplete="off"
          placeholder="Начните вводить логин" onChange={(event) => {
            setQuery(event.target.value);
            if (value) { editingSelection.current = true; onChange(null); }
          }} />
      </FormField>
      {value && <UserChip login={value.login} size={24} />}
      {value && !disabled && <Button variant="ghost" size="sm" aria-label={`Очистить: ${label}`}
        onClick={() => { setQuery(''); setResults([]); onChange(null); }}>Очистить</Button>}
      {loading && <LoadingBlock label="Поиск сотрудников…" inline />}
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {results.length > 0 && (
        <ul className="equipment-search-results" aria-label="Найденные сотрудники">
          {results.map((user) => <li key={user.id}>
            <Button variant="ghost" aria-label={user.login}
              onClick={() => { onChange(user); setQuery(user.login); setResults([]); }}>
              <UserChip login={user.login} size={24} />
            </Button>
          </li>)}
        </ul>
      )}
    </div>
  );
}
