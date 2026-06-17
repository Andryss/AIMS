import React, { FormEvent, useEffect, useState } from 'react';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { useFocusTrap } from '../hooks/useFocusTrap';
import {
  CreateIncidentInitialValues,
  CreateIncidentRequest,
  IncidentEventType,
  IncidentResponse,
} from '../types';
import { FileUploadField } from './FileUploadField';
import { MonitoringAlertMediaLinks } from './MonitoringAlertMediaLinks';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

const EVENT_TYPES = Object.entries(EVENT_TYPE_LABELS).map(([value, label]) => ({
  value: value as IncidentEventType,
  label,
}));

function toUtcIsoString(localDateTime: string): string {
  return new Date(localDateTime).toISOString();
}

function toDatetimeLocalValue(iso: string): string {
  const date = new Date(iso);
  const pad = (value: number) => String(value).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

interface CreateIncidentModalProps {
  token: string;
  open: boolean;
  onClose: () => void;
  onCreated: (incident: IncidentResponse) => void;
  initialValues?: CreateIncidentInitialValues | null;
}

export function CreateIncidentModal({
  token,
  open,
  onClose,
  onCreated,
  initialValues = null,
}: CreateIncidentModalProps) {
  const [eventType, setEventType] = useState<IncidentEventType>('UNIDENTIFIED_SIGHTING');
  const [location, setLocation] = useState('');
  const [detectedAt, setDetectedAt] = useState('');
  const [description, setDescription] = useState('');
  const [monitoringAlertId, setMonitoringAlertId] = useState<number | undefined>();
  const [referenceMediaUrls, setReferenceMediaUrls] = useState<string[]>([]);
  const [attachmentFiles, setAttachmentFiles] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const trapRef = useFocusTrap(open);

  useEffect(() => {
    if (!open) {
      return;
    }
    if (initialValues) {
      setEventType(initialValues.eventType ?? 'UNIDENTIFIED_SIGHTING');
      setLocation(initialValues.location ?? '');
      setDetectedAt(
        initialValues.detectedAt ? toDatetimeLocalValue(initialValues.detectedAt) : '',
      );
      setDescription(initialValues.description ?? '');
      setMonitoringAlertId(initialValues.monitoringAlertId);
      setReferenceMediaUrls(initialValues.mediaUrls ?? []);
    } else {
      setEventType('UNIDENTIFIED_SIGHTING');
      setLocation('');
      setDetectedAt('');
      setDescription('');
      setMonitoringAlertId(undefined);
      setReferenceMediaUrls([]);
    }
    setAttachmentFiles([]);
    setError(null);
  }, [open, initialValues]);

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
      if (monitoringAlertId != null) {
        payload.monitoringAlertId = monitoringAlertId;
      }
      const incident = await api.createIncident(token, payload);
      onCreated(incident);
      onClose();
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
          <h2 id="create-incident-title">
            {monitoringAlertId != null ? 'Зарегистрировать инцидент по алерту' : 'Создать инцидент'}
          </h2>
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
          {monitoringAlertId != null && (
            <div className="monitoring-alert-modal-note">
              {referenceMediaUrls.length > 0 && (
                <MonitoringAlertMediaLinks urls={referenceMediaUrls} compact />
              )}
              <p className="monitoring-alert-modal-note__text">
                Медиафайлы от внешней системы мониторинга не прикрепляются к инциденту автоматически.
                При необходимости скачайте их по ссылкам и загрузите вручную во вложения.
              </p>
            </div>
          )}
          {monitoringAlertId == null && referenceMediaUrls.length > 0 && (
            <MonitoringAlertMediaLinks urls={referenceMediaUrls} compact />
          )}
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
