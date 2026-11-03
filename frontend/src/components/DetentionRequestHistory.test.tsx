import React from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import * as api from '../api/client';
import { mockDetentionRequest } from '../test/testData';
import { DetentionRequestHistory } from './DetentionRequestHistory';

jest.mock('../api/client');
it('compares page boundaries and renders changed fields, users and reasons', async () => {
  (api.batchUsers as jest.Mock).mockResolvedValue({ items: [{ id: 2, login: 'agent' }, { id: 3, login: 'guard' }] });
  const before = { id: 1, changedByUserId: 2, changedAt: '2026-11-03T12:00:00Z', snapshot: { request: mockDetentionRequest() } };
  const after = { ...before, id: 2, changedByUserId: 3, snapshot: { request: mockDetentionRequest({ durationDays: 15,
    reviewedByUserId: 3, status: 'ACCEPTED' }), transitionComment: 'Пересмотр срока' } };
  (api.listDetentionRequestHistory as jest.Mock).mockImplementation(async (_token, _id, page) => ({
    items: [page ? after : before], page, size: 10, totalPages: 2, totalElements: 11,
  }));
  render(<MemoryRouter><DetentionRequestHistory token="tok" requestId={7} /></MemoryRouter>);
  await screen.findByText('30'); await userEvent.click(screen.getByRole('button', { name: 'Следующая →' }));
  expect(await screen.findByText('30 → 15')).toBeInTheDocument();
  expect(screen.queryByText('Описание субъекта:')).not.toBeInTheDocument();
  expect(screen.getByText('Новая → Принята')).toBeInTheDocument();
  expect(screen.getByText('Пересмотр срока')).toBeInTheDocument();
  expect((await screen.findAllByText('guard')).length).toBeGreaterThan(0);
});
