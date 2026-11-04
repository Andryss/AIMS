import React, { useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { useDebouncedSearch } from '../hooks/useDebouncedSearch';
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
  const editingSelection = useRef(false);

  const selectedId = value?.id;
  const selectedLogin = value?.login;

  useEffect(() => {
    if (selectedId != null && selectedLogin) { setQuery(selectedLogin); }
    else if (!editingSelection.current) { setQuery(''); }
    editingSelection.current = false;
  }, [selectedId, selectedLogin]);

  const { results, loading, error, clearResults } = useDebouncedSearch({
    scopeKey: token, query, enabled: !disabled && query !== selectedLogin,
    load: async (searchQuery) => (await api.searchUsers(token, searchQuery)).items,
    errorMessage: 'Не удалось найти сотрудников',
  });

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
        onClick={() => { setQuery(''); clearResults(); onChange(null); }}>Очистить</Button>}
      {loading && <LoadingBlock label="Поиск сотрудников…" inline />}
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {results.length > 0 && (
        <ul className="equipment-search-results" aria-label="Найденные сотрудники">
          {results.map((user) => <li key={user.id}>
            <Button variant="ghost" aria-label={user.login}
              onClick={() => { onChange(user); setQuery(user.login); clearResults(); }}>
              <UserChip login={user.login} size={24} />
            </Button>
          </li>)}
        </ul>
      )}
    </div>
  );
}
