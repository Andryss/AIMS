import React from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { Modal } from './Modal';

it('rejects backdrop, Escape and close button while busy, then closes when unlocked', () => {
  const close = jest.fn();
  const { rerender } = render(<Modal open title="Сохранение" busy onClose={close}><input aria-label="Поле" /></Modal>);
  fireEvent.click(screen.getByRole('presentation'));
  fireEvent.keyDown(document, { key: 'Escape' });
  fireEvent.click(screen.getByRole('button', { name: 'Закрыть' }));
  expect(close).not.toHaveBeenCalled();
  rerender(<Modal open title="Сохранение" onClose={close}><input aria-label="Поле" /></Modal>);
  fireEvent.click(screen.getByRole('dialog'));
  expect(close).not.toHaveBeenCalled();
  fireEvent.keyDown(document, { key: 'Escape' });
  fireEvent.click(screen.getByRole('presentation'));
  expect(close).toHaveBeenCalledTimes(2);
});

it('traps tab navigation and returns focus to the opener after hiding', () => {
  const opener = document.createElement('button');
  document.body.appendChild(opener);
  opener.focus();
  const { rerender } = render(<Modal open title="Карточка" onClose={jest.fn()}><input aria-label="Последнее поле" /></Modal>);
  const close = screen.getByRole('button', { name: 'Закрыть' });
  const input = screen.getByLabelText('Последнее поле');
  expect(close).toHaveFocus();
  fireEvent.keyDown(document, { key: 'Tab', shiftKey: true });
  expect(input).toHaveFocus();
  fireEvent.keyDown(document, { key: 'Tab' });
  expect(close).toHaveFocus();
  rerender(<Modal open={false} title="Карточка" onClose={jest.fn()}><input /></Modal>);
  expect(opener).toHaveFocus();
  opener.remove();
});
