import React, { useCallback, useEffect, useState } from 'react';
import * as api from '../api/client';
import { CreateIncidentInitialValues, IncidentResponse, MonitoringAlert } from '../types';
import { CreateIncidentModal } from './CreateIncidentModal';
import { MonitoringAlertCard } from './MonitoringAlertCard';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';

const PAGE_SIZE = 12;

interface MonitoringAlertsTabProps {
  token: string;
  canCreate: boolean;
}

export function MonitoringAlertsTab({ token, canCreate }: MonitoringAlertsTabProps) {
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<MonitoringAlert[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createInitialValues, setCreateInitialValues] =
    useState<CreateIncidentInitialValues | null>(null);

  const loadPage = useCallback(
    async (pageNumber: number) => {
      setLoading(true);
      setError(null);
      try {
        const response = await api.listMonitoringAlerts(token, pageNumber, PAGE_SIZE);
        setItems(response.items);
        setPage(response.page);
        setTotalPages(response.totalPages);
        setTotalElements(response.totalElements);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Не удалось загрузить алерты';
        setError(message);
      } finally {
        setLoading(false);
      }
    },
    [token],
  );

  useEffect(() => {
    loadPage(0);
  }, [loadPage]);

  const handleCreated = (_incident: IncidentResponse) => {
    loadPage(0);
  };

  const openCreateFromAlert = (alert: MonitoringAlert) => {
    setCreateInitialValues({
      eventType: alert.eventType,
      location: alert.location,
      detectedAt: alert.detectedAt,
      description: alert.description,
      monitoringAlertId: alert.id,
      mediaUrls: alert.mediaUrls,
    });
    setCreateModalOpen(true);
  };

  const formatDate = (iso: string) => {
    try {
      return new Date(iso).toLocaleString('ru-RU');
    } catch {
      return iso;
    }
  };

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header">
        <div>
          <h2>Входящие алерты</h2>
          <p className="card__subtitle">
            События от внешней системы мониторинга. Откройте медиа-ссылки или зарегистрируйте инцидент.
          </p>
        </div>
      </div>

      {error && (
        <div className="alert alert--error" role="alert" aria-live="polite">
          {error}
        </div>
      )}

      {loading && <LoadingBlock label="Загрузка алертов…" skeleton rows={3} />}

      {!loading && items.length === 0 && !error && (
        <EmptyState
          title="Алерты не найдены"
          hint="Когда внешняя система отправит событие, оно появится здесь."
        />
      )}

      {!loading && items.length > 0 && (
        <div className="monitoring-alerts-grid">
          {items.map((alert) => (
            <MonitoringAlertCard
              key={alert.id}
              alert={alert}
              canCreate={canCreate}
              onRegisterIncident={openCreateFromAlert}
              formatDate={formatDate}
            />
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="pagination">
          <button
            type="button"
            className="btn btn--secondary"
            disabled={page <= 0 || loading}
            onClick={() => loadPage(page - 1)}
          >
            ← Предыдущая
          </button>
          <span className="pagination__info">
            Страница {page + 1} из {totalPages} ({totalElements} всего)
          </span>
          <button
            type="button"
            className="btn btn--secondary"
            disabled={page >= totalPages - 1 || loading}
            onClick={() => loadPage(page + 1)}
          >
            Следующая →
          </button>
        </div>
      )}

      <CreateIncidentModal
        token={token}
        open={createModalOpen}
        onClose={() => {
          setCreateModalOpen(false);
          setCreateInitialValues(null);
        }}
        onCreated={handleCreated}
        initialValues={createInitialValues}
      />
    </section>
  );
}
