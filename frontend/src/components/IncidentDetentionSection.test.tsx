import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockDetentionRequest } from '../test/testData';
import { IncidentDetentionSection } from './IncidentDetentionSection';

jest.mock('../api/client');
beforeEach(() => {
  jest.clearAllMocks();
  (api.listDetentionRequests as jest.Mock).mockResolvedValue({ items: [], page: 0, size: 10, totalPages: 0, totalElements: 0 });
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [] });
});
it('creates from an incident and refreshes its list', async () => {
  (api.createDetentionRequest as jest.Mock).mockResolvedValue(mockDetentionRequest());
  render(<MemoryRouter><IncidentDetentionSection token="tok" incidentId={8} incidentStatus="EXECUTING" canRead canCreate /></MemoryRouter>);
  await screen.findByText('Заявок на заключение пока нет');
  expect(screen.queryByRole('table')).not.toBeInTheDocument();
  await userEvent.click(screen.getByRole('button', { name: 'Создать заявку на заключение' }));
  fireEvent.change(screen.getByLabelText(/Имя или обозначение/), { target: { value: 'Субъект' } });
  fireEvent.change(screen.getByLabelText(/Описание субъекта/), { target: { value: 'Описание' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(api.createDetentionRequest).toHaveBeenCalledWith('tok', expect.objectContaining({ incidentId: 8 }));
  await waitFor(() => expect(api.listDetentionRequests).toHaveBeenCalledTimes(2));
});
it('keeps existing requests visible after incident completion without creation', async () => {
  (api.listDetentionRequests as jest.Mock).mockResolvedValue({ items: [mockDetentionRequest()], page: 0, size: 10, totalPages: 1, totalElements: 1 });
  render(<MemoryRouter><IncidentDetentionSection token="tok" incidentId={8} incidentStatus="EXECUTION_COMPLETED" canRead canCreate /></MemoryRouter>);
  await screen.findByRole('link', { name: 'Заявка #7' });
  expect(screen.queryByRole('button', { name: 'Создать заявку на заключение' })).not.toBeInTheDocument();
});

it.each([false, true])('respects read access without creation, canRead=%s', async (canRead) => {
  render(<MemoryRouter><IncidentDetentionSection token="tok" incidentId={8} incidentStatus="EXECUTING"
    canRead={canRead} canCreate={false} /></MemoryRouter>);
  if (canRead) { await screen.findByText('Заявок на заключение пока нет'); }
  else { expect(api.listDetentionRequests).not.toHaveBeenCalled(); }
  expect(screen.queryByRole('button', { name: 'Создать заявку на заключение' })).not.toBeInTheDocument();
});

it('resets pagination on incident navigation and ignores the old pending page', async () => {
  let resolve!: (value: object) => void;
  (api.listDetentionRequests as jest.Mock).mockImplementation(async (_token, query) => {
    if (query.page === 1) { return new Promise((done) => { resolve = done; }); }
    return { items: [mockDetentionRequest({ id: query.incidentId })], page: 0, size: 10, totalPages: 2, totalElements: 11 };
  });
  const view = render(<MemoryRouter><IncidentDetentionSection token="tok" incidentId={8} incidentStatus="EXECUTING" canRead canCreate={false} /></MemoryRouter>);
  await screen.findByRole('link', { name: 'Заявка #8' });
  await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(api.listDetentionRequests).toHaveBeenLastCalledWith('tok', { incidentId: 8, page: 1, size: 10 });
  view.rerender(<MemoryRouter><IncidentDetentionSection token="tok" incidentId={9} incidentStatus="EXECUTING" canRead canCreate={false} /></MemoryRouter>);
  await screen.findByRole('link', { name: 'Заявка #9' });
  await act(async () => resolve({ items: [mockDetentionRequest({ id: 99 })], page: 1, size: 10, totalPages: 2, totalElements: 11 }));
  expect(screen.queryByRole('link', { name: 'Заявка #99' })).not.toBeInTheDocument();
});
