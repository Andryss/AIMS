import React from 'react';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import * as api from '../api/client';
import { mockAlien } from '../test/testData';
import { AlienPickerDrawer } from './AlienPickerDrawer';

jest.mock('../api/client');

const searchAliens = api.searchAliens as jest.MockedFunction<typeof api.searchAliens>;

describe('AlienPickerDrawer', () => {
  const onClose = jest.fn();
  const onSelect = jest.fn();

  beforeEach(() => {
    jest.clearAllMocks();
    searchAliens.mockResolvedValue({ items: [mockAlien({ id: 2, name: 'Грей' })] });
  });

  it('does not render when closed', () => {
    const { container } = render(
      <AlienPickerDrawer
        token="tok"
        open={false}
        selecting={false}
        onClose={onClose}
        onSelect={onSelect}
      />,
    );
    expect(container).toBeEmptyDOMElement();
  });

  it('searches aliens and selects from card', async () => {
    render(
      <AlienPickerDrawer
        token="tok"
        open
        selecting={false}
        onClose={onClose}
        onSelect={onSelect}
      />,
    );

    expect(screen.getByRole('dialog', { name: 'Выбор типа инопланетянина' })).toBeInTheDocument();

    await userEvent.type(screen.getByLabelText('Поиск типа инопланетянина'), 'грей');

    await waitFor(() => {
      expect(searchAliens).toHaveBeenCalledWith('tok', 'грей');
    });

    await waitFor(() => {
      expect(screen.getByText('Грей')).toBeInTheDocument();
    });

    await userEvent.click(screen.getByRole('button', { name: /Грей/ }));
    await userEvent.click(screen.getByRole('button', { name: 'Выбрать' }));
    expect(onSelect).toHaveBeenCalledWith(2);
  });

  it('closes on overlay click', async () => {
    const { container } = render(
      <AlienPickerDrawer
        token="tok"
        open
        selecting={false}
        onClose={onClose}
        onSelect={onSelect}
      />,
    );

    const overlay = container.querySelector('.drawer-overlay');
    expect(overlay).not.toBeNull();
    await userEvent.click(overlay!);
    expect(onClose).toHaveBeenCalled();
  });
});
