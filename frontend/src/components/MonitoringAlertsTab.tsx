import { Card, CardHeader } from './ui/Card';
import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { formatDateTime } from '../utils/dateTime';
import { CreateIncidentInitialValues, IncidentResponse, MonitoringAlert } from '../types';
import { IncidentEditorModal } from './IncidentEditorModal';
import { MonitoringAlertCard } from './MonitoringAlertCard';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

const PAGE_SIZE = 12;

interface MonitoringAlertsTabProps {
  token: string;
  canCreate: boolean;
}

export function MonitoringAlertsTab({ token, canCreate }: MonitoringAlertsTabProps) {
  const [paging, setPaging] = useState({ token, page: 0 });
  const page = paging.token === token ? paging.page : 0;
  const setPage = (nextPage: number) => setPaging({ token, page: nextPage });
  useEffect(() => setPaging({ token, page: 0 }), [token]);
  const { result, loading, error, reload } = usePaginatedResource({
    scopeKey: token, page,
    load: (requestedPage) => api.listMonitoringAlerts(token, requestedPage, PAGE_SIZE),
    errorMessage: 'Не удалось загрузить алерты', onPageCorrection: setPage,
  });
  const items = result?.items ?? [];
  const totalPages = result?.totalPages ?? 0;
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [createInitialValues, setCreateInitialValues] =
    useState<CreateIncidentInitialValues | null>(null);

  const handleCreated = (_incident: IncidentResponse) => {
    setPage(0);
    reload();
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

  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <div>
          <h2>Входящие алерты</h2>
          <p className="card__subtitle">
            События от внешней системы мониторинга. Откройте медиа-ссылки или зарегистрируйте инцидент.
          </p>
        </div>
      </CardHeader>

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
              formatDate={formatDateTime}
            />
          ))}
        </div>
      )}

      {totalPages > 1 && result && <Pagination page={result.page} totalPages={totalPages}
        totalElements={result.totalElements} loading={loading} onChange={setPage} />}

      <IncidentEditorModal
        token={token}
        open={createModalOpen}
        onClose={() => {
          setCreateModalOpen(false);
          setCreateInitialValues(null);
        }}
        onSaved={handleCreated}
        initialValues={createInitialValues}
      />
    </Card>
  );
}
