import React, { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
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
