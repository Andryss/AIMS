import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { LoginPage } from './LoginPage';

describe('LoginPage', () => {
  it('submits login and password', async () => {
    const onSignIn = jest.fn().mockResolvedValue(undefined);
    render(<LoginPage loading={false} onSignIn={onSignIn} />);

    await userEvent.type(screen.getByLabelText(/Логин/i), 'operator');
    await userEvent.type(screen.getByLabelText(/Пароль/i), 'secret');
    await userEvent.click(screen.getByRole('button', { name: /Войти/i }));

    expect(onSignIn).toHaveBeenCalledWith('operator', 'secret');
  });

  it('shows loading state', () => {
    render(<LoginPage loading onSignIn={jest.fn()} />);
    expect(screen.getByRole('button', { name: /Вход…/i })).toBeDisabled();
  });
});
