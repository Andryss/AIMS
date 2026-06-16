import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { IncidentResponse } from '../types';
import { CreateIncidentModal } from './CreateIncidentModal';
import { IncidentStatusSelect } from './IncidentStatusSelect';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';

const PAGE_SIZE = 10;

interface IncidentsTabProps {
  token: string;
  roles: string[];
  canCreate: boolean;
  canChangeStatus: boolean;
}

export function IncidentsTab({
  token,
  roles,
  canCreate,
  canChangeStatus,
}: IncidentsTabProps) {
  const navigate = useNavigate();
  const isMobile = useMediaQuery('(max-width: 640px)');
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<IncidentResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);

  const loadPage = useCallback(
    async (pageNumber: number) => {
      setLoading(true);
      setError(null);
      try {
        const response = await api.listIncidents(token, pageNumber, PAGE_SIZE);
        setItems(response.items);
        setPage(response.page);
        setTotalPages(response.totalPages);
        setTotalElements(response.totalElements);
      } catch (err: unknown) {
        const message = err instanceof Error ? err.message : 'Не удалось загрузить инциденты';
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

  const handleCreated = () => {
    loadPage(0);
  };

  const handleStatusChanged = (updated: IncidentResponse) => {
    setItems((prev) => prev.map((item) => (item.id === updated.id ? updated : item)));
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
        <h2>Инциденты</h2>
        {canCreate && (
          <Button type="button" variant="primary" onClick={() => setCreateModalOpen(true)}>
            Создать инцидент
          </Button>
        )}
      </div>

      {error && (
        <div className="alert alert--error" role="alert" aria-live="polite">
          {error}
        </div>
      )}
      {loading && <LoadingBlock label="Загрузка инцидентов…" skeleton rows={4} />}

      {!loading && items.length === 0 && !error && (
        <EmptyState
          title="Инцидентов пока нет"
          hint={canCreate ? 'Создайте первый инцидент, чтобы начать работу.' : undefined}
        />
      )}

      {!loading && items.length > 0 && (
        <>
          {!isMobile && (
            <div className="incidents-table-wrapper">
              <table className="incidents-table">
                <thead>
                  <tr>
                    <th>ID</th>
                    <th>Статус</th>
                    <th>Тип</th>
                    <th>Место</th>
                    <th>Обнаружен</th>
                  </tr>
                </thead>
                <tbody>
                  {items.map((incident) => (
                    <tr
                      key={incident.id}
                      className="incidents-table__row"
                      onClick={() => navigate(`/incidents/${incident.id}`)}
                    >
                      <td>#{incident.id}</td>
                      <td>
                        <IncidentStatusSelect
                          token={token}
                          incident={incident}
                          roles={roles}
                          canChange={canChangeStatus}
                          onStatusChanged={handleStatusChanged}
                        />
                      </td>
                      <td>{EVENT_TYPE_LABELS[incident.eventType] ?? incident.eventType}</td>
                      <td>{incident.location}</td>
                      <td>{formatDate(incident.detectedAt)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {isMobile && (
            <div className="incidents-cards">
              {items.map((incident) => (
                <button
                  key={incident.id}
                  type="button"
                  className="incidents-card"
                  onClick={() => navigate(`/incidents/${incident.id}`)}
                >
                  <div className="incidents-card__top">
                    <span className="incidents-card__id">#{incident.id}</span>
                    <IncidentStatusSelect
                      token={token}
                      incident={incident}
                      roles={roles}
                      canChange={canChangeStatus}
                      onStatusChanged={handleStatusChanged}
                    />
                  </div>
                  <p className="incidents-card__meta">
                    {EVENT_TYPE_LABELS[incident.eventType] ?? incident.eventType} ·{' '}
                    {formatDate(incident.detectedAt)}
                  </p>
                  <p className="incidents-card__location">{incident.location}</p>
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {totalPages > 0 && (
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
        onClose={() => setCreateModalOpen(false)}
        onCreated={handleCreated}
      />
    </section>
  );
}
