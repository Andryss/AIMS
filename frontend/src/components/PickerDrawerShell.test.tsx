import React, { useState } from 'react';
import { fireEvent, render, screen } from '@testing-library/react';
import { PickerDrawerShell } from './PickerDrawerShell';

function Drawer({ busy }: { busy: boolean }) {
  const [open, setOpen] = useState(true);
  return <PickerDrawerShell title="Выбор" titleId="picker-title" open={open} selecting={busy}
    onClose={() => setOpen(false)}><input aria-label="Поиск" /></PickerDrawerShell>;
}

it('blocks backdrop and Escape while selecting, then allows Escape to close', () => {
  const { rerender } = render(<Drawer busy />);
  fireEvent.click(screen.getByRole('presentation'));
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  fireEvent.keyDown(document, { key: 'Escape' });
  expect(screen.getByRole('dialog')).toBeInTheDocument();
  rerender(<Drawer busy={false} />);
  fireEvent.keyDown(document, { key: 'Escape' });
  expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
});
