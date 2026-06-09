import React from 'react';
import { render, screen } from '@testing-library/react';
import App from './App';
import { SYSTEM_NAME } from './constants';

test('renders sign in form with system name and fields', () => {
  render(<App />);
  expect(screen.getByRole('heading', { name: SYSTEM_NAME })).toBeInTheDocument();
  expect(screen.getByLabelText(/Логин/i)).toBeInTheDocument();
  expect(screen.getByLabelText(/Пароль/i)).toBeInTheDocument();
  expect(screen.getByRole('button', { name: /Войти/i })).toBeInTheDocument();
  expect(screen.queryByText(/демо/i)).not.toBeInTheDocument();
});
