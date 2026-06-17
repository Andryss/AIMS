import React from 'react';
import { Link } from 'react-router-dom';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { MonitoringAlert } from '../types';
import { MonitoringAlertMediaLinks } from './MonitoringAlertMediaLinks';
import { Button } from './ui/Button';

const ALERT_STATUS_LABELS: Record<MonitoringAlert['status'], string> = {
  NEW: 'Новый',
  INCIDENT_CREATED: 'Инцидент создан',
};

interface MonitoringAlertCardProps {
  alert: MonitoringAlert;
  canCreate: boolean;
  onRegisterIncident: (alert: MonitoringAlert) => void;
  formatDate: (iso: string) => string;
}

export function MonitoringAlertCard({
  alert,
  canCreate,
  onRegisterIncident,
  formatDate,
}: MonitoringAlertCardProps) {
  return (
    <article className="monitoring-alert-card" aria-labelledby={`monitoring-alert-${alert.id}-title`}>
      <header className="monitoring-alert-card__header">
        <div>
          <p className="monitoring-alert-card__eyebrow">
            Алерт #{alert.id} · {alert.sourceSystem}
          </p>
          <h3 id={`monitoring-alert-${alert.id}-title`} className="monitoring-alert-card__title">
            {EVENT_TYPE_LABELS[alert.eventType]}
          </h3>
        </div>
        <span
          className={`monitoring-alert-card__status monitoring-alert-card__status--${alert.status.toLowerCase()}`}
        >
          {ALERT_STATUS_LABELS[alert.status]}
        </span>
      </header>

      <dl className="monitoring-alert-card__facts">
        <div>
          <dt>Место</dt>
          <dd>{alert.location}</dd>
        </div>
        <div>
          <dt>Обнаружено</dt>
          <dd>{formatDate(alert.detectedAt)}</dd>
        </div>
        <div>
          <dt>Получено в AIMS</dt>
          <dd>{formatDate(alert.receivedAt)}</dd>
        </div>
        <div>
          <dt>Внешний ID</dt>
          <dd className="monitoring-alert-card__mono">{alert.externalEventId}</dd>
        </div>
      </dl>

      <p className="monitoring-alert-card__description">{alert.description}</p>

      <MonitoringAlertMediaLinks urls={alert.mediaUrls} />

      <footer className="monitoring-alert-card__actions">
        {alert.status === 'NEW' && canCreate && (
          <Button type="button" variant="primary" onClick={() => onRegisterIncident(alert)}>
            Зарегистрировать инцидент
          </Button>
        )}
        {alert.incidentId != null && (
          <Link to={`/incidents/${alert.incidentId}`} className="btn btn--secondary">
            Открыть инцидент #{alert.incidentId}
          </Link>
        )}
      </footer>
    </article>
  );
}
