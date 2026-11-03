import React from 'react';
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { mockTender } from '../test/testData';
import { TendersTab } from './TendersTab';

jest.mock('../api/client');
const page = { items: [mockTender()], page: 0, size: 10, totalElements: 1, totalPages: 1 };
beforeEach(() => {
  jest.clearAllMocks();
  (api.listTenders as jest.Mock).mockResolvedValue(page);
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 2, login: 'manager' }] });
});

function open(path = '/tenders', canCreate = true) {
  render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/tenders" element={<TendersTab token="tok" canCreate={canCreate} />} />
    <Route path="/tenders/:id" element={<p>Карточка тендера</p>} />
  </Routes></MemoryRouter>);
}

it('loads URL filters and opens the card while retaining them', async () => {
  (api.listTenders as jest.Mock).mockResolvedValue({ ...page, page: 1, totalPages: 2, totalElements: 12 });
  open('/tenders?q=неделя&status=NEED&equipmentType=NEURALYZER&incidentId=8&page=1');
  const link = await screen.findByRole('link', { name: 'Тендер #4' });
  const url = new URL(link.getAttribute('href')!, 'http://localhost');
  expect(url.pathname).toBe('/tenders/4');
  expect(Object.fromEntries(url.searchParams)).toEqual({ q: 'неделя', status: 'NEED',
    equipmentType: 'NEURALYZER', incidentId: '8', page: '1' });
  expect(await screen.findByText('manager')).toBeInTheDocument();
  expect(screen.getByRole('columnheader', { name: 'Количество оборудования' })).toBeInTheDocument();
  expect(api.listTenders).toHaveBeenLastCalledWith('tok', { q: 'неделя', status: 'NEED',
    equipmentType: 'NEURALYZER', incidentId: 8, page: 1, size: 10 });
  await userEvent.click(link);
  expect(screen.getByText('Карточка тендера')).toBeInTheDocument();
});

it('resets pagination for search and ignores unsupported URL enum values', async () => {
  open('/tenders?status=constructor&equipmentType=toString&page=1');
  await screen.findByRole('link', { name: 'Тендер #4' });
  expect(api.listTenders).toHaveBeenCalledWith('tok', expect.objectContaining({
    status: undefined, equipmentType: undefined,
  }));
  fireEvent.change(screen.getByLabelText('Поиск тендеров'), { target: { value: '  новая поставка  ' } });
  await waitFor(() => expect(api.listTenders).toHaveBeenLastCalledWith('tok',
    expect.objectContaining({ q: 'новая поставка', page: 0 })));
});

it('ignores old query results and corrects a page outside the current result', async () => {
  let resolve!: (value: typeof page) => void;
  (api.listTenders as jest.Mock).mockImplementationOnce(() => new Promise((done) => { resolve = done; }))
    .mockResolvedValue({ ...page, items: [mockTender({ id: 5 })] });
  open();
  fireEvent.change(screen.getByLabelText('Поиск тендеров'), { target: { value: 'новое' } });
  await screen.findByRole('link', { name: 'Тендер #5' });
  await act(async () => resolve(page));
  expect(screen.queryByRole('link', { name: 'Тендер #4' })).not.toBeInTheDocument();
});

it.each([true, false])('guards creation for canCreate=%s', async (canCreate) => {
  open('/tenders', canCreate);
  await screen.findByRole('link', { name: 'Тендер #4' });
  expect(Boolean(screen.queryByRole('button', { name: 'Создать тендер' }))).toBe(canCreate);
});

it('corrects a stale page and then loads the next page', async () => {
  (api.listTenders as jest.Mock).mockImplementation(async (_token, query) => ({ ...page, page: query.page,
    totalElements: 12, totalPages: 2 }));
  open('/tenders?page=9');
  await screen.findByRole('link', { name: 'Тендер #4' });
  expect(api.listTenders).toHaveBeenLastCalledWith('tok', expect.objectContaining({ page: 1 }));
  await userEvent.click(screen.getByRole('button', { name: '← Предыдущая' }));
  await waitFor(() => expect(api.listTenders).toHaveBeenLastCalledWith('tok', expect.objectContaining({ page: 0 })));
});

it.each([false, true])('shows a list empty result or error, failed=%s', async (failed) => {
  if (failed) { (api.listTenders as jest.Mock).mockRejectedValue(new Error('Нет соединения')); }
  else { (api.listTenders as jest.Mock).mockResolvedValue({ ...page, items: [], totalElements: 0, totalPages: 0 }); }
  open();
  if (failed) { expect(await screen.findByRole('alert')).toHaveTextContent('Нет соединения'); }
  else { expect(await screen.findByText('Тендеры не найдены')).toBeInTheDocument(); }
});


it('opens the created card when saving the warehouse create query', async () => {
  (api.createTender as jest.Mock).mockResolvedValue(mockTender());
  open('/tenders?create=1');
  const modal = within(screen.getByRole('dialog'));
  fireEvent.change(modal.getByLabelText(/Тип оборудования/), { target: { value: 'NEURALYZER' } });
  await userEvent.click(modal.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByText('Карточка тендера')).toBeInTheDocument();
});
