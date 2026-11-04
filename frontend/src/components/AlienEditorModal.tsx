import React, { FormEvent, useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { useMutation } from '../hooks/useMutation';
import { Alien, SaveAlienRequest } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';
import { Modal } from './ui/Modal';
import { FileUploadField } from './FileUploadField';
import { AttachmentDownloadList } from './AttachmentDownloadList';

interface AlienEditorModalProps {
  token: string;
  open: boolean;
  alien: Alien | null;
  onClose: () => void;
  onSaved: (alien: Alien) => void;
}

export function AlienEditorModal({ token, open, alien, onClose, onSaved }: AlienEditorModalProps) {
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [threatLevel, setThreatLevel] = useState('');
  const [photoIds, setPhotoIds] = useState<number[]>([]);
  const [files, setFiles] = useState<File[]>([]);
  const [photosChanged, setPhotosChanged] = useState(false);
  const uploaded = useRef(new Map<File, number>());
  const mutation = useMutation(JSON.stringify([token, open, alien?.id]));
  const { busy: submitting, error, setError } = mutation;

  useEffect(() => {
    if (open) {
      setName(alien?.name ?? '');
      setDescription(alien?.description ?? '');
      setThreatLevel(alien ? String(alien.threatLevel) : '');
      setPhotoIds(alien?.photoFileIds ?? []);
      setFiles([]);
      setPhotosChanged(false);
      uploaded.current.clear();
      setError(null);
    }
  }, [open, token, alien, setError]);

  if (!open) {
    return null;
  }

  const close = () => {
    if (!mutation.isBusy()) {
      onClose();
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (mutation.isBusy()) {
      return;
    }
    const payload: SaveAlienRequest = { name: name.trim(), description: description.trim(),
      threatLevel: Number(threatLevel) };
    if (!payload.name || !payload.description) {
      setError('Укажите название и описание вида');
      return;
    }
    if (!Number.isInteger(payload.threatLevel) || payload.threatLevel < 1 || payload.threatLevel > 10) {
      setError('Уровень угрозы должен быть целым числом от 1 до 10');
      return;
    }
    if (photoIds.length + files.length > 20) {
      setError('Можно добавить до 20 фотографий');
      return;
    }
    if (files.some((file) => !['image/png', 'image/jpeg'].includes(file.type) || file.size > 10 * 1024 * 1024)) {
      setError('Выберите PNG или JPEG размером до 10 МиБ');
      return;
    }
    const session = mutation.start();
    if (!session) { return; }
    try {
      for (const file of files) {
        if (!uploaded.current.has(file)) {
          const response = await api.uploadFile(token, file);
          if (!session.isCurrent()) { return; }
          uploaded.current.set(file, response.id);
        }
      }
      if (photosChanged) {
        payload.photoFileIds = [...photoIds, ...files.map((file) => uploaded.current.get(file)!)];
      }
      const saved = alien
        ? await api.updateAlien(token, alien.id, payload)
        : await api.createAlien(token, payload);
      if (!session.isCurrent()) { return; }
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      session.fail(err, 'Не удалось сохранить вид');
    } finally {
      session.finish();
    }
  };

  return (
    <Modal open={open} title={alien ? 'Редактировать вид' : 'Добавить вид'} onClose={close} busy={submitting}>
      {error && <div className="alert alert--error" role="alert" aria-live="polite">{error}</div>}
      <form className="form" aria-label={alien ? 'Редактировать вид' : 'Добавить вид'} onSubmit={handleSubmit}>
        <FormField label="Название" required>
          <input value={name} onChange={(event) => setName(event.target.value)}
            maxLength={200} disabled={submitting} required />
        </FormField>
        <FormField label="Описание" required>
          <textarea value={description} onChange={(event) => setDescription(event.target.value)}
            maxLength={4000} rows={5} disabled={submitting} required />
        </FormField>
        <FormField label="Уровень угрозы" required help="От 1 до 10">
          <input type="number" min={1} max={10} step={1} value={threatLevel}
            onChange={(event) => setThreatLevel(event.target.value)} disabled={submitting} required />
        </FormField>
        <AttachmentDownloadList token={token} fileIds={photoIds} downloadingId={null} disabled={submitting}
          onDownload={(id, fileName) => { api.downloadFile(token, id, fileName).catch((err) => setError(err.message)); }}
          onRemove={(id) => { setPhotoIds((ids) => ids.filter((value) => value !== id)); setPhotosChanged(true); }} />
        <FileUploadField label="Фотографии" files={files} disabled={submitting} accept="image/png,image/jpeg"
          onChange={(next) => { setFiles(next); setPhotosChanged(true); }} />
        <div className="modal-actions">
          <Button variant="secondary" onClick={close} disabled={submitting}>Отмена</Button>
          <Button type="submit" loading={submitting}>Сохранить</Button>
        </div>
      </form>
    </Modal>
  );
}
