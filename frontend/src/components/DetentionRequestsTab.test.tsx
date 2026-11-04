import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockDetentionRequest } from '../test/testData';
import { DetentionRequestsTab } from './DetentionRequestsTab';

jest.mock('../api/client');
const page = { items: [mockDetentionRequest()], page: 0, size: 10, totalElements: 1, totalPages: 1 };
beforeEach(() => {
  jest.clearAllMocks();
  (api.listDetentionRequests as jest.Mock).mockResolvedValue(page);
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 2, login: 'agent' }] });
});
function open(path = '/detention-requests') {
  render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/detention-requests" element={<DetentionRequestsTab token="tok" />} />
    <Route path="/detention-requests/:id" element={<p>Карточка заявки</p>} />
  </Routes></MemoryRouter>);
}
it('loads URL filters, renders authors and preserves query on card navigation', async () => {
  (api.listDetentionRequests as jest.Mock).mockResolvedValue({ ...page, page: 1, totalPages: 2 });
  open('/detention-requests?q=субъект&status=NEW&incidentId=8&page=1');
  const link = await screen.findByRole('link', { name: 'Заявка #7' });
  expect(api.listDetentionRequests).toHaveBeenLastCalledWith('tok', { q: 'субъект', status: 'NEW', incidentId: 8, page: 1, size: 10 });
  expect(await screen.findByText('agent')).toBeInTheDocument();
  expect(screen.queryByRole('button', { name: /Создать/ })).not.toBeInTheDocument();
  expect(link.getAttribute('href')).toContain('incidentId=8');
  await userEvent.click(link); expect(screen.getByText('Карточка заявки')).toBeInTheDocument();
});
it('debounces search, ignores unsupported status and stale results', async () => {
  let resolve!: (value: typeof page) => void;
  (api.listDetentionRequests as jest.Mock).mockImplementationOnce(() => new Promise((done) => { resolve = done; }))
    .mockResolvedValue({ ...page, items: [mockDetentionRequest({ id: 9 })] });
  open('/detention-requests?status=constructor&page=1');
  fireEvent.change(screen.getByLabelText('Поиск заявок'), { target: { value: '  новое  ' } });
  await screen.findByRole('link', { name: 'Заявка #9' });
  await act(async () => resolve(page));
  expect(screen.queryByRole('link', { name: 'Заявка #7' })).not.toBeInTheDocument();
  await waitFor(() => expect(api.listDetentionRequests).toHaveBeenLastCalledWith('tok', expect.objectContaining({ q: 'новое', page: 0, status: undefined })));
});
it('shows the empty state without a table', async () => {
  (api.listDetentionRequests as jest.Mock).mockResolvedValue({ ...page, items: [] });
  open();
  await screen.findByText('Заявки не найдены');
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
});
it('shows the loading error', async () => {
  (api.listDetentionRequests as jest.Mock).mockRejectedValue(new Error('Нет соединения'));
  open();
  expect(await screen.findByRole('alert')).toHaveTextContent('Нет соединения');
});
