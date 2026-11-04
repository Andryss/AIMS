import React, { useState } from 'react';
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react';
import * as api from '../api/client';
import { CleanupReportResponse } from '../types';
import { CleanupReportDrawer } from './CleanupReportDrawer';

jest.mock('../api/client');

it('does not apply a report saved from the preceding incident drawer', async () => {
  let finish!: (report: CleanupReportResponse) => void;
  (api.uploadFiles as jest.Mock).mockResolvedValue([{ id: 4 }]);
  (api.createCleanupReport as jest.Mock).mockReturnValue(new Promise((resolve) => { finish = resolve; }));
  function Drawer({ incidentId }: { incidentId: number }) {
    const [saved, setSaved] = useState<CleanupReportResponse | null>(null);
    return <><CleanupReportDrawer token="token" incidentId={incidentId} open mode="create" report={null}
      onClose={() => {}} onCreated={setSaved} onDownloadFile={() => {}} downloadingId={null} />
      <output aria-label="Сохраненный отчёт">{saved?.incidentId ?? 'Нет'}</output></>;
  }
  const { rerender } = render(<Drawer incidentId={8} />);
  fireEvent.change(screen.getByLabelText(/Описание выполнения/), { target: { value: 'Описание' } });
  fireEvent.drop(screen.getByRole('button', { name: /Перетащите файлы/ }), {
    dataTransfer: { files: [new File(['photo'], 'photo.png', { type: 'image/png' })], clearData: jest.fn() },
  });
  fireEvent.click(screen.getByRole('button', { name: 'Сохранить' }));
  await waitFor(() => expect(api.createCleanupReport).toHaveBeenCalled());
  rerender(<Drawer incidentId={9} />);
  await act(async () => finish({ id: 1, incidentId: 8, description: 'Описание', attachmentFileIds: [4],
    createdByUserId: 1, createdAt: '2026-11-05T00:00:00Z' }));
  expect(screen.getByLabelText('Сохраненный отчёт')).toHaveTextContent('Нет');
});
