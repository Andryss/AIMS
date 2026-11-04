import React from 'react';
import { act, fireEvent, render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockAlien } from '../test/testData';
import { AlienSummary } from './AlienSummary';

jest.mock('../api/client');
const mockApi = api as jest.Mocked<typeof api>;

beforeEach(() => {
  jest.clearAllMocks();
  jest.useFakeTimers();
  mockApi.batchFiles.mockResolvedValue({ items: [{ id: 8, fileName: 'ashta.png', contentType: 'image/png',
    fileSize: 5, createdAt: '' }] });
  mockApi.getFilePreviewUrl.mockResolvedValue('blob:ashta');
});
afterEach(() => { jest.runOnlyPendingTimers(); jest.useRealTimers(); });

function show() {
  return render(<MemoryRouter><AlienSummary token="tok" incidentId={8}
    alien={mockAlien({ name: 'Ашта', description: 'Полное длинное описание', photoFileIds: [8], threatLevel: 9 })} />
  </MemoryRouter>);
}

it('shows the compact species without fetching photos until a sustained hover', async () => {
  show();
  const trigger = screen.getByRole('button', { name: 'Подробнее о виде Ашта' });
  expect(screen.getByText('(угроза 9)')).toBeInTheDocument();
  expect(screen.queryByText('Полное длинное описание')).not.toBeInTheDocument();
  expect(mockApi.batchFiles).not.toHaveBeenCalled();
  fireEvent.mouseEnter(trigger);
  act(() => { jest.advanceTimersByTime(250); });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  act(() => { jest.advanceTimersByTime(350); });
  expect(await screen.findByRole('dialog', { name: 'Вид: Ашта' })).toBeInTheDocument();
  expect(screen.getByText('Полное длинное описание')).toBeInTheDocument();
  expect(await screen.findByAltText('ashta.png')).toHaveAttribute('src', 'blob:ashta');
  expect(mockApi.batchFiles).toHaveBeenCalledWith('tok', [8]);
  fireEvent.keyDown(document, { key: 'Escape' });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});

it('does not fetch photos after a short hover or unmount', async () => {
  const view = show();
  const trigger = screen.getByRole('button', { name: 'Подробнее о виде Ашта' });
  fireEvent.mouseEnter(trigger);
  act(() => { jest.advanceTimersByTime(100); });
  fireEvent.mouseLeave(trigger);
  act(() => { jest.advanceTimersByTime(600); });
  expect(mockApi.batchFiles).not.toHaveBeenCalled();
  fireEvent.mouseEnter(trigger);
  view.unmount();
  act(() => { jest.advanceTimersByTime(600); });
  expect(mockApi.batchFiles).not.toHaveBeenCalled();
});

it('supports focus, moving into the card and explicit close on touch', async () => {
  show();
  const trigger = screen.getByRole('button', { name: 'Подробнее о виде Ашта' });
  fireEvent.focus(trigger);
  act(() => { jest.advanceTimersByTime(600); });
  const card = await screen.findByRole('dialog', { name: 'Вид: Ашта' });
  await screen.findByAltText('ashta.png');
  fireEvent.mouseLeave(trigger);
  fireEvent.mouseEnter(card);
  act(() => { jest.advanceTimersByTime(300); });
  expect(card).toBeInTheDocument();
  fireEvent.click(screen.getByRole('button', { name: 'Закрыть сведения о виде' }));
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  fireEvent.click(trigger);
  expect(await screen.findByRole('dialog', { name: 'Вид: Ашта' })).toBeInTheDocument();
  await screen.findByAltText('ashta.png');
});


it('moves keyboard focus into the card and restores it without reopening on Escape', async () => {
  show();
  const trigger = screen.getByRole('button', { name: 'Подробнее о виде Ашта' });
  act(() => { trigger.focus(); });
  fireEvent.keyDown(trigger, { key: 'ArrowDown' });
  await screen.findByRole('dialog', { name: 'Вид: Ашта' });
  await screen.findByAltText('ashta.png');
  expect(screen.getByRole('button', { name: 'Закрыть сведения о виде' })).toHaveFocus();
  userEvent.tab();
  expect(screen.getByRole('button', { name: 'Увеличить ashta.png' })).toHaveFocus();
  userEvent.tab();
  expect(screen.getByRole('button', { name: 'Скачать ashta.png' })).toHaveFocus();
  fireEvent.keyDown(document, { key: 'Escape' });
  expect(trigger).toHaveFocus();
  act(() => { jest.advanceTimersByTime(600); });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
