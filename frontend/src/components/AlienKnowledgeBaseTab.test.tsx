import React from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import * as api from '../api/client';
import { AlienListResponse } from '../types';
import { mockAlien } from '../test/testData';
import { AlienKnowledgeBaseTab } from './AlienKnowledgeBaseTab';
import { AlienDetailPage } from './AlienDetailPage';

jest.mock('../api/client');
const listAliens = api.listAliens as jest.MockedFunction<typeof api.listAliens>;
const getAlien = api.getAlien as jest.MockedFunction<typeof api.getAlien>;
const createAlien = api.createAlien as jest.MockedFunction<typeof api.createAlien>;
const updateAlien = api.updateAlien as jest.MockedFunction<typeof api.updateAlien>;
const page: AlienListResponse = { items: [mockAlien()], page: 0, size: 10, totalElements: 1, totalPages: 1 };
function renderAt(path = '/aliens', canWrite = true) {
  return render(<MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/aliens" element={<AlienKnowledgeBaseTab token="tok" canWrite={canWrite} />} />
    <Route path="/aliens/:id" element={<AlienDetailPage token="tok" canWrite={canWrite} />} />
  </Routes></MemoryRouter>);
}
beforeEach(() => {
  jest.clearAllMocks();
  listAliens.mockResolvedValue(page);
  getAlien.mockResolvedValue(mockAlien());
});

it('loads a searchable table and preserves filter/page on return from a card', async () => {
  listAliens.mockResolvedValue({ ...page, page: 1, totalElements: 20, totalPages: 2 });
  renderAt('/aliens?q=слиз&page=1');
  await userEvent.click(await screen.findByRole('link', { name: 'Слизень' }));
  expect(await screen.findByRole('heading', { name: 'Слизень' })).toBeInTheDocument();
  expect(screen.getByText('Небольшое слизистое существо')).toBeInTheDocument();
  await userEvent.click(screen.getByRole('link', { name: /К списку/ }));
  await screen.findByRole('link', { name: 'Слизень' });
  expect(listAliens).toHaveBeenLastCalledWith('tok', { q: 'слиз', page: 1, size: 10 });
  expect(screen.getByLabelText('Поиск по базе знаний')).toHaveValue('слиз');
});

it('debounces search and resets pagination', async () => {
  renderAt('/aliens?page=1');
  await screen.findByRole('link', { name: 'Слизень' });
  fireEvent.change(screen.getByLabelText('Поиск по базе знаний'), { target: { value: '  червь  ' } });
  await waitFor(() => expect(listAliens).toHaveBeenLastCalledWith('tok', { q: 'червь', page: 0, size: 10 }));
});

it('ignores a stale response for an earlier query', async () => {
  let resolveOld!: (value: AlienListResponse) => void;
  listAliens.mockImplementationOnce(() => new Promise((resolve) => { resolveOld = resolve; }))
    .mockResolvedValue({ ...page, items: [mockAlien({ name: 'Червь' })] });
  renderAt();
  fireEvent.change(screen.getByLabelText('Поиск по базе знаний'), { target: { value: 'червь' } });
  await screen.findByRole('link', { name: 'Червь' });
  await act(async () => resolveOld(page));
  expect(screen.queryByRole('link', { name: 'Слизень' })).not.toBeInTheDocument();
});

it.each([true, false])('shows write controls only with permission=%s', async (canWrite) => {
  renderAt('/aliens', canWrite);
  await screen.findByRole('link', { name: 'Слизень' });
  expect(Boolean(screen.queryByRole('button', { name: 'Добавить вид' }))).toBe(canWrite);
  await userEvent.click(screen.getByRole('link', { name: 'Слизень' }));
  await screen.findByRole('heading', { name: 'Слизень' });
  expect(Boolean(screen.queryByRole('button', { name: 'Редактировать вид' }))).toBe(canWrite);
});

it('refreshes the table after creating a species', async () => {
  createAlien.mockResolvedValue(mockAlien({ id: 2, name: 'Новый вид' }));
  renderAt();
  await screen.findByRole('link', { name: 'Слизень' });
  await userEvent.click(screen.getByRole('button', { name: 'Добавить вид' }));
  await userEvent.type(screen.getByLabelText(/Название/), 'Новый вид');
  await userEvent.type(screen.getByLabelText(/Описание/), 'Признаки');
  await userEvent.type(screen.getByLabelText(/Уровень угрозы/), '4');
  listAliens.mockResolvedValue({ ...page, items: [mockAlien({ id: 2, name: 'Новый вид' })] });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('link', { name: 'Новый вид' })).toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it('updates the card after saving prefilled edit fields', async () => {
  updateAlien.mockResolvedValue(mockAlien({ name: 'Изменённый вид' }));
  renderAt('/aliens/1');
  await screen.findByRole('heading', { name: 'Слизень' });
  await userEvent.click(screen.getByRole('button', { name: 'Редактировать вид' }));
  expect(screen.getByLabelText(/Название/)).toHaveValue('Слизень');
  fireEvent.change(screen.getByLabelText(/Название/), { target: { value: 'Изменённый вид' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  expect(await screen.findByRole('heading', { name: 'Изменённый вид' })).toBeInTheDocument();
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it('shows list loading errors', async () => {
  listAliens.mockRejectedValue(new Error('Нет соединения'));
  renderAt();
  expect(await screen.findByRole('alert')).toHaveTextContent('Нет соединения');
});

it('shows empty search results', async () => {
  listAliens.mockResolvedValue({ ...page, items: [], totalElements: 0, totalPages: 0 });
  renderAt('/aliens?q=неизвестный');
  expect(await screen.findByText('Виды не найдены')).toBeInTheDocument();
});

it.each(['abc', '0', '1.5'])('rejects invalid card id %s', async (id) => {
  renderAt(`/aliens/${id}`);
  expect(await screen.findByRole('alert')).toHaveTextContent('Некорректный идентификатор вида');
  expect(getAlien).not.toHaveBeenCalled();
});

it('shows card load failures', async () => {
  getAlien.mockRejectedValue(new Error('Вид не найден'));
  renderAt('/aliens/999');
  expect(await screen.findByRole('alert')).toHaveTextContent('Вид не найден');
});

it('navigates pages while retaining the search filter', async () => {
  listAliens.mockResolvedValueOnce({ ...page, totalElements: 11, totalPages: 2 })
    .mockResolvedValue({ ...page, page: 1, totalElements: 11, totalPages: 2 });
  renderAt('/aliens?q=слиз');
  await screen.findByRole('link', { name: 'Слизень' });
  expect(screen.getByRole('button', { name: /Предыдущая/ })).toBeDisabled();
  await userEvent.click(screen.getByRole('button', { name: /Следующая/ }));
  await waitFor(() => expect(listAliens).toHaveBeenLastCalledWith('tok', { q: 'слиз', page: 1, size: 10 }));
  await waitFor(() => expect(screen.getByRole('button', { name: /Следующая/ })).toBeDisabled());
});

it('removes a renamed species from its old filtered list after returning from editing', async () => {
  updateAlien.mockResolvedValue(mockAlien({ name: 'Червь' }));
  renderAt('/aliens?q=слиз');
  await userEvent.click(await screen.findByRole('link', { name: 'Слизень' }));
  await screen.findByRole('heading', { name: 'Слизень' });
  await userEvent.click(screen.getByRole('button', { name: 'Редактировать вид' }));
  fireEvent.change(screen.getByLabelText(/Название/), { target: { value: 'Червь' } });
  await userEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await screen.findByRole('heading', { name: 'Червь' });
  listAliens.mockResolvedValue({ ...page, items: [], totalElements: 0, totalPages: 0 });
  await userEvent.click(screen.getByRole('link', { name: /К списку/ }));
  expect(await screen.findByText('Виды не найдены')).toBeInTheDocument();
  expect(screen.getByLabelText('Поиск по базе знаний')).toHaveValue('слиз');
});

it('keeps search accessible without a visible label', async () => {
  renderAt();
  await screen.findByRole('link', { name: 'Слизень' });
  expect(screen.getByRole('searchbox', { name: 'Поиск по базе знаний' })).toBeInTheDocument();
  expect(screen.queryByText('Поиск по базе знаний')).not.toBeInTheDocument();
});
