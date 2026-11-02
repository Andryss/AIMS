import React, { FormEvent, useEffect, useId, useState } from 'react';
import * as api from '../api/client';
import { useFocusTrap } from '../hooks/useFocusTrap';
import { Alien, SaveAlienRequest } from '../types';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

interface AlienEditorModalProps {
  token: string;
  open: boolean;
  alien: Alien | null;
  onClose: () => void;
  onSaved: (alien: Alien) => void;
}

export function AlienEditorModal({ token, open, alien, onClose, onSaved }: AlienEditorModalProps) {
  const titleId = useId();
  const trapRef = useFocusTrap(open);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [threatLevel, setThreatLevel] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (open) {
      setName(alien?.name ?? '');
      setDescription(alien?.description ?? '');
      setThreatLevel(alien ? String(alien.threatLevel) : '');
      setError(null);
    }
  }, [open, alien]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !submitting) {
        onClose();
      }
    };
    document.addEventListener('keydown', handleEscape);
    return () => document.removeEventListener('keydown', handleEscape);
  }, [open, submitting, onClose]);

  if (!open) {
    return null;
  }

  const close = () => {
    if (!submitting) {
      onClose();
    }
  };

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault();
    if (submitting) {
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
    setSubmitting(true);
    setError(null);
    try {
      const saved = alien
        ? await api.updateAlien(token, alien.id, payload)
        : await api.createAlien(token, payload);
      onSaved(saved);
      onClose();
    } catch (err: unknown) {
      setError(err instanceof Error ? err.message : 'Не удалось сохранить вид');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={close} role="presentation">
      <div ref={trapRef} className="modal" role="dialog" aria-modal="true"
        aria-labelledby={titleId} onClick={(event) => event.stopPropagation()}>
        <div className="modal__header">
          <h2 id={titleId}>{alien ? 'Редактировать вид' : 'Добавить вид'}</h2>
          <Button variant="ghost" onClick={close} disabled={submitting} aria-label="Закрыть">×</Button>
        </div>
        {error && <div className="alert alert--error" role="alert" aria-live="polite">{error}</div>}
        <form className="form" onSubmit={handleSubmit}>
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
          <div className="modal-actions">
            <Button variant="secondary" onClick={close} disabled={submitting}>Отмена</Button>
            <Button type="submit" loading={submitting}>Сохранить</Button>
          </div>
        </form>
      </div>
    </div>
  );
}
