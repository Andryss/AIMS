import React, { FormEvent, useState } from 'react';
import { SYSTEM_NAME } from '../constants';

interface LoginPageProps {
  loading: boolean;
  onSignIn: (login: string, password: string) => Promise<void>;
}

export function LoginPage({ loading, onSignIn }: LoginPageProps) {
  const [login, setLogin] = useState('');
  const [password, setPassword] = useState('');

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    await onSignIn(login, password);
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <h1 className="login-title">{SYSTEM_NAME}</h1>
        <form onSubmit={handleSubmit} className="form">
          <label>
            Логин
            <input
              value={login}
              onChange={(e) => setLogin(e.target.value)}
              autoComplete="username"
              required
            />
          </label>
          <label>
            Пароль
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </label>
          <button type="submit" disabled={loading}>
            {loading ? 'Вход…' : 'Войти'}
          </button>
        </form>
      </div>
    </div>
  );
}
