import React from 'react';
import { act, fireEvent, render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { Link, MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockAlien, mockDetentionRequest } from '../test/testData';
import { DetentionRequestDetailPage } from './DetentionRequestDetailPage';

jest.mock('../api/client');
beforeEach(() => {
  jest.clearAllMocks();
  (api.getDetentionRequest as jest.Mock).mockImplementation(async (_token, id) => mockDetentionRequest({ id, alienId: 1 }));
  (api.getAlien as jest.Mock).mockResolvedValue(mockAlien());
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 2, login: 'agent' }] });
  (api.listDetentionRequestHistory as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10, totalElements: 0, totalPages: 0 });
});
function open(path = '/detention-requests/7', permissions = ['DETENTION_READ', 'DETENTION_EDIT']) {
  render(<MemoryRouter initialEntries={[path]}><Link to="/detention-requests/9">Другая заявка</Link><Routes>
    <Route path="/detention-requests/:id" element={<DetentionRequestDetailPage token="tok" permissions={permissions}
      currentUserId={2} canReadIncidents canReadAliens />} />
  </Routes></MemoryRouter>);
}
it('groups actions beside the title, displays fields and history tab, and retains list filters', async () => {
  open('/detention-requests/7?q=субъект&page=1');
  await screen.findByRole('heading', { name: 'Заявка на заключение #7' });
  const actions = within(screen.getByRole('group', { name: 'Действия заявки' }));
  expect(actions.getByRole('button', { name: 'Редактировать заявку' })).toBeInTheDocument();
  expect(actions.getByRole('link', { name: /К списку/ })).toHaveAttribute('href', '/detention-requests?q=субъект&page=1');
  expect(screen.getByText('Статус', { exact: true }).tagName).toBe('DT');
  expect(screen.getByRole('tab', { name: 'История' })).toBeInTheDocument();
  expect(await screen.findByRole('link', { name: 'Слизень' })).toHaveAttribute('href', '/aliens/1');
});
it('ignores a previous card response after navigation', async () => {
  let resolve!: (value: ReturnType<typeof mockDetentionRequest>) => void;
  (api.getDetentionRequest as jest.Mock).mockImplementationOnce(() => new Promise((done) => { resolve = done; }))
    .mockResolvedValue(mockDetentionRequest({ id: 9 }));
  open(); await userEvent.click(screen.getByRole('link', { name: 'Другая заявка' }));
  await screen.findByRole('heading', { name: 'Заявка на заключение #9' });
  await act(async () => resolve(mockDetentionRequest()));
  expect(screen.queryByRole('heading', { name: 'Заявка на заключение #7' })).not.toBeInTheDocument();
});
it.each(['RELEASED', 'REJECTED', 'CANCELLED'] as const)('does not edit a terminal request %s', async (status) => {
  (api.getDetentionRequest as jest.Mock).mockResolvedValue(mockDetentionRequest({ status }));
  open(); await screen.findByRole('heading', { name: 'Заявка на заключение #7' });
  expect(screen.queryByRole('button', { name: 'Редактировать заявку' })).not.toBeInTheDocument();
});
it('marks an overdue custody without automatically releasing it', async () => {
  (api.getDetentionRequest as jest.Mock).mockResolvedValue(mockDetentionRequest({ status: 'IN_CUSTODY',
    admittedAt: '2000-01-01T00:00:00Z', plannedReleaseAt: '2000-02-01T00:00:00Z' }));
  open(); expect(await screen.findByText('Срок заключения истёк')).toBeInTheDocument();
  expect(api.changeDetentionRequestStatus).not.toHaveBeenCalled();
});

it('keeps a changed status when the first StrictMode load finishes late', async () => {
  let resolve!: (value: ReturnType<typeof mockDetentionRequest>) => void;
  (api.getDetentionRequest as jest.Mock).mockImplementationOnce(() => new Promise((done) => { resolve = done; }))
    .mockResolvedValue(mockDetentionRequest());
  (api.changeDetentionRequestStatus as jest.Mock).mockResolvedValue(mockDetentionRequest({ status: 'ACCEPTED' }));
  render(<React.StrictMode><MemoryRouter initialEntries={['/detention-requests/7']}><Routes>
    <Route path="/detention-requests/:id" element={<DetentionRequestDetailPage token="tok"
      permissions={['DETENTION_READ', 'DETENTION_EDIT', 'DETENTION_PROCESS']} currentUserId={2} />} />
  </Routes></MemoryRouter></React.StrictMode>);
  await screen.findByRole('heading', { name: 'Заявка на заключение #7' });
  fireEvent.change(screen.getByLabelText('Статус заявки'), { target: { value: 'ACCEPTED' } });
  await screen.findByText('Ожидается прибытие');
  await act(async () => resolve(mockDetentionRequest()));
  expect(screen.getByLabelText('Статус заявки')).toHaveValue('ACCEPTED');
});

it('disables editing while a status transition is pending', async () => {
  let resolve!: (value: ReturnType<typeof mockDetentionRequest>) => void;
  (api.changeDetentionRequestStatus as jest.Mock).mockImplementation(() => new Promise((done) => { resolve = done; }));
  open('/detention-requests/7', ['DETENTION_READ', 'DETENTION_EDIT', 'DETENTION_PROCESS']);
  await screen.findByRole('heading', { name: 'Заявка на заключение #7' });
  fireEvent.change(screen.getByLabelText('Статус заявки'), { target: { value: 'ACCEPTED' } });
  expect(screen.getByRole('button', { name: 'Редактировать заявку' })).toBeDisabled();
  await act(async () => resolve(mockDetentionRequest({ status: 'ACCEPTED' })));
  expect(screen.getByRole('button', { name: 'Редактировать заявку' })).toBeEnabled();
});
