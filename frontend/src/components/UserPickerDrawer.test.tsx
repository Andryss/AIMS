import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { UserPickerDrawer } from './UserPickerDrawer';

jest.mock('../api/client');

const searchUsers = api.searchUsers as jest.MockedFunction<typeof api.searchUsers>;

describe('UserPickerDrawer', () => {
  const onClose = jest.fn();
  const onSelect = jest.fn();
  const onSelectMany = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    searchUsers.mockResolvedValue({
      items: [
        { id: 10, login: 'agent.one' },
        { id: 11, login: 'agent.two' },
        { id: 12, login: 'agent.three' },
      ],
    });
  });

  it('selects multiple users and confirms in batch', async () => {
    render(
      <UserPickerDrawer
        token="tok"
        open
        title="Добавить исполнителей"
        selecting={false}
        multiple
        excludeUserIds={[12]}
        onClose={onClose}
        onSelectMany={onSelectMany}
      />,
    );

    await userEvent.type(screen.getByLabelText('Поиск агента'), 'agent');

    await waitFor(() => {
      expect(searchUsers).toHaveBeenCalledWith('tok', 'agent', 'AGENT');
    });

    await waitFor(() => {
      expect(screen.getByText('agent.one')).toBeInTheDocument();
      expect(screen.getByText('agent.two')).toBeInTheDocument();
      expect(screen.queryByText('agent.three')).not.toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /agent\.one/ }));
    await userEvent.click(screen.getByRole('button', { name: /agent\.two/ }));

    expect(screen.getByRole('button', { name: 'Добавить (2)' })).toBeEnabled();
    await userEvent.click(screen.getByRole('button', { name: 'Добавить (2)' }));

    expect(onSelectMany).toHaveBeenCalledWith([
      { id: 10, login: 'agent.one' },
      { id: 11, login: 'agent.two' },
    ]);
  });

  it('selects single user in default mode', async () => {
    render(
      <UserPickerDrawer
        token="tok"
        open
        title="Назначить ответственного"
        selecting={false}
        onClose={onClose}
        onSelect={onSelect}
      />,
    );

    await userEvent.type(screen.getByLabelText('Поиск агента'), 'one');

    await waitFor(() => {
      expect(screen.getByText('agent.one')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /agent\.one/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Выбрать' }));

    expect(onSelect).toHaveBeenCalledWith({ id: 10, login: 'agent.one' });
  });
});
