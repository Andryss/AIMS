import React, { useCallback, useEffect, useState } from 'react';
import * as api from '../api/client';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { IncidentResponse } from '../types';
import { CreateIncidentModal } from './CreateIncidentModal';
import { IncidentStatusSelect } from './IncidentStatusSelect';
import { IncidentViewModal } from './IncidentViewModal';

const PAGE_SIZE = 10;

interface IncidentsTabProps {
  token: string;
  canCreate: boolean;
  canChangeStatus: boolean;
  openIncidentId?: number | null;
  onOpenIncidentHandled?: () => void;
}

export function IncidentsTab({
  token,
  canCreate,
  canChangeStatus,
  openIncidentId,
  onOpenIncidentHandled,
}: IncidentsTabProps) {
  const [page, setPage] = useState(0);
  const [items, setItems] = useState<IncidentResponse[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [viewIncidentId, setViewIncidentId] = useState<number | null>(null);

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

  useEffect(() => {
    if (openIncidentId == null) {
      return;
    }
    setViewIncidentId(openIncidentId);
    onOpenIncidentHandled?.();
  }, [openIncidentId, onOpenIncidentHandled]);

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
    <section className="tab-panel">
      <div className="tab-panel-header">
        <h2>Инциденты</h2>
        {canCreate && (
          <button type="button" onClick={() => setCreateModalOpen(true)}>
            Создать инцидент
          </button>
        )}
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p className="panel-muted">Загрузка…</p>}

      {!loading && items.length === 0 && (
        <p className="panel-muted">Инцидентов пока нет</p>
      )}

      {!loading && items.length > 0 && (
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
                  className="incidents-table-row"
                  onClick={() => setViewIncidentId(incident.id)}
                >
                  <td>#{incident.id}</td>
                  <td>
                    <IncidentStatusSelect
                      token={token}
                      incident={incident}
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

      {totalPages > 0 && (
        <div className="pagination">
          <button
            type="button"
            className="secondary"
            disabled={page <= 0 || loading}
            onClick={() => loadPage(page - 1)}
          >
            ← Предыдущая
          </button>
          <span className="pagination-info">
            Страница {page + 1} из {totalPages} ({totalElements} всего)
          </span>
          <button
            type="button"
            className="secondary"
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

      <IncidentViewModal
        token={token}
        incidentId={viewIncidentId}
        open={viewIncidentId != null}
        canChangeStatus={canChangeStatus}
        onStatusChanged={handleStatusChanged}
        onClose={() => setViewIncidentId(null)}
      />
    </section>
  );
}
