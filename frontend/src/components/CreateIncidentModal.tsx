import React, { FormEvent, useState } from 'react';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { CreateIncidentRequest, IncidentEventType, IncidentResponse } from '../types';
import { FileUploadField } from './FileUploadField';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

const EVENT_TYPES = Object.entries(EVENT_TYPE_LABELS).map(([value, label]) => ({
  value: value as IncidentEventType,
  label,
}));

function toUtcIsoString(localDateTime: string): string {
  return new Date(localDateTime).toISOString();
}

interface CreateIncidentModalProps {
  token: string;
  open: boolean;
  onClose: () => void;
  onCreated: (incident: IncidentResponse) => void;
}

export function CreateIncidentModal({
  token,
  open,
  onClose,
  onCreated,
}: CreateIncidentModalProps) {
  const [eventType, setEventType] = useState<IncidentEventType>('UNIDENTIFIED_SIGHTING');
  const [location, setLocation] = useState('');
  const [detectedAt, setDetectedAt] = useState('');
  const [description, setDescription] = useState('');
  const [attachmentFiles, setAttachmentFiles] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const trapRef = useFocusTrap(open);

  if (!open) {
    return null;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (attachmentFiles.length === 0) {
      setError('Загрузите хотя бы одно вложение перед созданием инцидента');
      return;
    }

    setLoading(true);
    setError(null);

    try {
      const uploaded = await api.uploadFiles(token, attachmentFiles);
      const payload: CreateIncidentRequest = {
        eventType,
        location: location.trim(),
        detectedAt: toUtcIsoString(detectedAt),
        description: description.trim(),
        attachmentFileIds: uploaded.map((item) => item.id),
      };
      const incident = await api.createIncident(token, payload);
      onCreated(incident);
      onClose();
      setLocation('');
      setDetectedAt('');
      setDescription('');
      setAttachmentFiles([]);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось создать инцидент';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose} role="presentation">
      <div
        ref={trapRef}
        className="modal"
        onClick={(e) => e.stopPropagation()}
        role="dialog"
        aria-modal="true"
        aria-labelledby="create-incident-title"
      >
        <div className="modal__header">
          <h2 id="create-incident-title">Создать инцидент</h2>
          <button type="button" className="btn btn--ghost" onClick={onClose} aria-label="Закрыть">
            ×
          </button>
        </div>
        {error && (
          <div className="alert alert--error" role="alert" aria-live="polite">
            {error}
          </div>
        )}
        <form onSubmit={handleSubmit} className="form">
          <FormField label="Тип события" required>
            <select
              value={eventType}
              onChange={(e) => setEventType(e.target.value as IncidentEventType)}
            >
              {EVENT_TYPES.map((item) => (
                <option key={item.value} value={item.value}>
                  {item.label}
                </option>
              ))}
            </select>
          </FormField>
          <FormField label="Место" required>
            <input
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              required
            />
          </FormField>
          <FormField label="Время обнаружения" required>
            <input
              type="datetime-local"
              value={detectedAt}
              onChange={(e) => setDetectedAt(e.target.value)}
              required
            />
          </FormField>
          <FormField label="Описание" required>
            <textarea
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              rows={4}
              required
            />
          </FormField>
          <FileUploadField
            label="Вложения"
            files={attachmentFiles}
            onChange={setAttachmentFiles}
            disabled={loading}
          />
          <div className="modal-actions">
            <Button type="button" variant="secondary" onClick={onClose}>
              Отмена
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              Создать
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
