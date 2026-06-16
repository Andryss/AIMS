import React, { FormEvent, useId, useState } from 'react';
import * as api from '../api/client';
import { CleanupReportResponse } from '../types';
import { FileUploadField } from './FileUploadField';
import { AttachmentDownloadList } from './AttachmentDownloadList';
import { PickerDrawerShell } from './PickerDrawerShell';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { LoadingBlock } from './ui/LoadingBlock';

interface CleanupReportDrawerProps {
  token: string;
  incidentId: number;
  open: boolean;
  mode: 'create' | 'view';
  report: CleanupReportResponse | null;
  onClose: () => void;
  onCreated: (report: CleanupReportResponse) => void;
  onDownloadFile: (fileId: number) => void;
  downloadingId: number | null;
}

export function CleanupReportDrawer({
  token,
  incidentId,
  open,
  mode,
  report,
  onClose,
  onCreated,
  onDownloadFile,
  downloadingId,
}: CleanupReportDrawerProps) {
  const titleId = useId();
  const [description, setDescription] = useState('');
  const [attachmentFiles, setAttachmentFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (mode !== 'create' || submitting) {
      return;
    }
    const trimmed = description.trim();
    if (!trimmed) {
      setError('Укажите описание выполнения');
      return;
    }
    if (attachmentFiles.length === 0) {
      setError('Прикрепите хотя бы один файл');
      return;
    }

    setSubmitting(true);
    setError(null);
    try {
      const uploaded = await api.uploadFiles(token, attachmentFiles);
      const fileIds = uploaded.map((f) => f.id);
      const created = await api.createCleanupReport(token, incidentId, trimmed, fileIds);
      setDescription('');
      setAttachmentFiles([]);
      onCreated(created);
      onClose();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось сохранить отчёт';
      setError(message);
    } finally {
      setSubmitting(false);
    }
  };

  const isCreate = mode === 'create';
  const busy = submitting;

  return (
    <PickerDrawerShell
      title={`Отчёт об очистке · инцидент №${incidentId}`}
      titleId={titleId}
      open={open}
      selecting={busy}
      onClose={onClose}
    >
      {isCreate ? (
        <>
          {error && (
            <div className="alert alert--error" role="alert" aria-live="polite">
              {error}
            </div>
          )}
          <form onSubmit={(event) => void handleSubmit(event)} className="form drawer-form">
            <FormField label="Описание выполнения" required>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={4}
                disabled={busy}
                required
              />
            </FormField>
            <FileUploadField
              label="Материалы"
              files={attachmentFiles}
              onChange={setAttachmentFiles}
              disabled={busy}
            />
            <div className="modal-actions">
              <Button type="button" variant="secondary" onClick={onClose} disabled={busy}>
                Отмена
              </Button>
              <Button type="submit" variant="primary" loading={busy}>
                Сохранить
              </Button>
            </div>
          </form>
        </>
      ) : report ? (
        <div className="cleanup-report-view">
          <section className="section">
            <h3 className="section__title">Описание выполнения</h3>
            <p className="cleanup-report-view__description">{report.description}</p>
          </section>
          <section className="section">
            <h3 className="section__title">Материалы</h3>
            {report.attachmentFileIds.length > 0 ? (
              <AttachmentDownloadList
                fileIds={report.attachmentFileIds}
                downloadingId={downloadingId}
                onDownload={onDownloadFile}
              />
            ) : (
              <p className="text-muted">Вложений нет</p>
            )}
          </section>
          <div className="modal-actions cleanup-report-view__actions">
            <button type="button" className="btn btn--secondary" onClick={onClose}>
              Закрыть
            </button>
          </div>
        </div>
      ) : (
        <LoadingBlock label="Загрузка отчёта…" inline />
      )}
    </PickerDrawerShell>
  );
}
