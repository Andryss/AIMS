import React, { FormEvent, useState } from 'react';
import { SYSTEM_NAME } from '../constants';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

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
        <h1 className="login-card__title">{SYSTEM_NAME}</h1>
        <form onSubmit={handleSubmit} className="form">
          <FormField label="Логин" htmlFor="login-username" required>
            <input
              value={login}
              onChange={(e) => setLogin(e.target.value)}
              autoComplete="username"
              required
            />
          </FormField>
          <FormField label="Пароль" htmlFor="login-password" required>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </FormField>
          <Button id="login-submit" type="submit" variant="primary" block loading={loading}>
            Войти
          </Button>
        </form>
      </div>
    </div>
  );
}
