import React, { FormEvent, useEffect, useRef, useState } from 'react';
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
import { AttachmentDownloadList } from './AttachmentDownloadList';
import { MonitoringAlertMediaLinks } from './MonitoringAlertMediaLinks';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

const MAX_VICTIM_COUNT = 2_147_483_647;
const MAX_ATTACHMENT_COUNT = 10;

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

interface IncidentEditorModalProps {
  token: string;
  open: boolean;
  onClose: () => void;
  onSaved: (incident: IncidentResponse) => void;
  initialValues?: CreateIncidentInitialValues | null;
  incident?: IncidentResponse | null;
  onConflict?: (incident: IncidentResponse) => void;
}

export function IncidentEditorModal({
  token,
  open,
  onClose,
  onSaved,
  initialValues = null,
  incident = null,
  onConflict,
}: IncidentEditorModalProps) {
  const [eventType, setEventType] = useState<IncidentEventType>('UNIDENTIFIED_SIGHTING');
  const [location, setLocation] = useState('');
  const [detectedAt, setDetectedAt] = useState('');
  const [description, setDescription] = useState('');
  const [victimCount, setVictimCount] = useState('');
  const [monitoringAlertId, setMonitoringAlertId] = useState<number | undefined>();
  const [referenceMediaUrls, setReferenceMediaUrls] = useState<string[]>([]);
  const [attachmentFiles, setAttachmentFiles] = useState<File[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const trapRef = useFocusTrap(open);
  const generation = useRef(0);
  const submitting = useRef(false);
  const uploadedIds = useRef(new Map<File, number>());
  const initialDetectedAt = useRef<string | null>(null);
  const openingValues = useRef({ incident, initialValues });
  openingValues.current = { incident, initialValues };
  const scope = JSON.stringify([token, open, incident?.id]);
  const currentScope = useRef(scope);
  currentScope.current = scope;
  const existingFiles = Array.from(new Set(incident?.attachmentFileIds ?? []));
  const remainingFiles = Math.max(0, MAX_ATTACHMENT_COUNT - existingFiles.length);

  const close = () => { if (!submitting.current) { onClose(); } };

  useEffect(() => {
    if (!open) { return; }
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !submitting.current) { onClose(); }
    };
    document.addEventListener('keydown', onKey);
    return () => document.removeEventListener('keydown', onKey);
  }, [open, onClose]);

  useEffect(() => {
    generation.current += 1;
    submitting.current = false;
    setLoading(false);
    uploadedIds.current.clear();
    if (!open) {
      return;
    }
    const { incident, initialValues } = openingValues.current;
    initialDetectedAt.current = incident?.detectedAt ?? null;
    if (incident) {
      setEventType(incident.eventType);
      setLocation(incident.location);
      setDetectedAt(toDatetimeLocalValue(incident.detectedAt));
      setDescription(incident.description);
      setMonitoringAlertId(undefined);
      setReferenceMediaUrls([]);
    } else if (initialValues) {
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
    setVictimCount(incident?.victimCount == null ? '' : String(incident.victimCount));
    setAttachmentFiles([]);
    setError(null);
    return () => { generation.current += 1; };
  }, [open, initialValues, incident?.id, token]);

  if (!open) {
    return null;
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (submitting.current) { return; }
    const count = victimCount.trim() === '' ? null : Number(victimCount);
    if (count != null && (!Number.isInteger(count) || count < 0 || count > MAX_VICTIM_COUNT)) {
      setError('Укажите целое неотрицательное количество пострадавших');
      return;
    }
    if (!incident && attachmentFiles.length === 0) {
      setError('Загрузите хотя бы одно вложение перед созданием инцидента');
      return;
    }
    if (existingFiles.length + attachmentFiles.length > MAX_ATTACHMENT_COUNT) {
      setError(`Можно прикрепить не более ${MAX_ATTACHMENT_COUNT} файлов`);
      return;
    }

    const request = ++generation.current;
    const isCurrent = () => request === generation.current && currentScope.current === scope;
    submitting.current = true;
    setLoading(true);
    setError(null);

    try {
      const pendingFiles = attachmentFiles.filter((file) => !uploadedIds.current.has(file));
      const uploaded = pendingFiles.length ? await api.uploadFiles(token, pendingFiles) : [];
      if (!isCurrent()) { return; }
      pendingFiles.forEach((file, index) => uploadedIds.current.set(file, uploaded[index].id));
      const attachmentIds = attachmentFiles.map((file) => uploadedIds.current.get(file)!);
      const details = {
        eventType,
        location: location.trim(),
        detectedAt: initialDetectedAt.current && detectedAt === toDatetimeLocalValue(initialDetectedAt.current)
          ? initialDetectedAt.current : toUtcIsoString(detectedAt),
        description: description.trim(),
        victimCount: count,
      };
      const payload: CreateIncidentRequest = { ...details, attachmentFileIds: attachmentIds };
      if (monitoringAlertId != null) {
        payload.monitoringAlertId = monitoringAlertId;
      }
      const updated = incident
        ? await api.updateIncident(token, incident.id, { ...details, additionalAttachmentFileIds: attachmentIds })
        : await api.createIncident(token, payload);
      if (isCurrent()) { onSaved(updated); onClose(); }
    } catch (err: unknown) {
      if (isCurrent()) {
        setError(err instanceof Error ? err.message : 'Не удалось сохранить инцидент');
        if (incident && err instanceof api.ApiError && err.status === 409) {
          try {
            const current = await api.getIncident(token, incident.id);
            if (isCurrent()) {
              setAttachmentFiles((files) => files.filter((file) => {
                const id = uploadedIds.current.get(file);
                return id == null || !current.attachmentFileIds.includes(id);
              }));
              onConflict?.(current);
              if (current.status !== 'DRAFT' && current.status !== 'CLARIFICATION_REQUIRED') { onClose(); }
            }
          } catch { /* Сохраняем исходную ошибку и введённые данные для повторной попытки. */ }
        }
      }
    } finally {
      if (isCurrent()) { submitting.current = false; setLoading(false); }
    }
  };

  return (
    <div className="modal-overlay" onClick={close} role="presentation">
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
            {incident ? 'Редактировать инцидент' : monitoringAlertId != null ? 'Зарегистрировать инцидент по алерту' : 'Создать инцидент'}
          </h2>
          <button type="button" className="btn btn--ghost" onClick={close} disabled={loading} aria-label="Закрыть">
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
              disabled={loading}
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
              disabled={loading}
              value={location}
              onChange={(e) => setLocation(e.target.value)}
              required
            />
          </FormField>
          <FormField label="Время обнаружения" required>
            <input
              disabled={loading}
              type="datetime-local"
              value={detectedAt}
              onChange={(e) => setDetectedAt(e.target.value)}
              required
            />
          </FormField>
          <FormField label="Количество пострадавших">
            <input type="number" min={0} max={MAX_VICTIM_COUNT} step={1} inputMode="numeric" disabled={loading}
              placeholder="Неизвестно" value={victimCount} onChange={(event) => setVictimCount(event.target.value)} />
          </FormField>
          <FormField label="Описание" required>
            <textarea
              disabled={loading}
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
          {incident && <AttachmentDownloadList token={token} fileIds={existingFiles} downloadingId={null}
            disabled={loading} onDownload={(id, fileName) => {
              const request = generation.current;
              api.downloadFile(token, id, fileName).catch((err) => {
                if (generation.current === request && currentScope.current === scope) { setError(err.message); }
              });
            }} />}
          <FileUploadField
            label={incident ? 'Добавить вложения' : 'Вложения'}
            files={attachmentFiles}
            maxFiles={remainingFiles}
            onChange={setAttachmentFiles}
            disabled={loading || (remainingFiles === 0 && attachmentFiles.length === 0)}
          />
          <div className="modal-actions">
            <Button type="button" variant="secondary" onClick={close} disabled={loading}>
              Отмена
            </Button>
            <Button type="submit" variant="primary" loading={loading}>
              {incident ? 'Сохранить' : 'Создать'}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
