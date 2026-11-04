import React, { useState } from 'react';
import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import * as api from '../api/client';
import { EmployeeSearch } from './EmployeeSearch';
import { UserSummary } from '../types';

jest.mock('../api/client');

const agent: UserSummary = { id: 3, login: 'agent' };

it('clears the displayed employee after an external filter reset', () => {
  const { rerender } = render(<EmployeeSearch token="tok" label="Держатель" value={agent} onChange={jest.fn()} />);
  expect(screen.getByLabelText('Держатель')).toHaveValue('agent');
  rerender(<EmployeeSearch token="tok" label="Держатель" value={null} onChange={jest.fn()} />);
  expect(screen.getByLabelText('Держатель')).toHaveValue('');
});

it('keeps the typed query when editing clears the selected employee', () => {
  function Search() {
    const [value, setValue] = useState<UserSummary | null>(agent);
    return <EmployeeSearch token="tok" label="Держатель" value={value} onChange={setValue} />;
  }
  render(<Search />);
  fireEvent.change(screen.getByLabelText('Держатель'), { target: { value: 'a' } });
  expect(screen.getByLabelText('Держатель')).toHaveValue('a');
});

it('dismisses the result immediately when choosing the matching login', async () => {
  (api.searchUsers as jest.Mock).mockResolvedValue({ items: [agent] });
  const onChange = jest.fn();
  render(<EmployeeSearch token="tok" label="Держатель" value={null} onChange={onChange} />);
  fireEvent.change(screen.getByLabelText('Держатель'), { target: { value: 'agent' } });
  fireEvent.click(await screen.findByRole('button', { name: 'agent' }));
  expect(onChange).toHaveBeenCalledWith(agent);
  await waitFor(() => expect(screen.queryByRole('list', { name: 'Найденные сотрудники' })).not.toBeInTheDocument());
  expect(screen.getByLabelText('Держатель')).toHaveValue('agent');
});
