import { Card, CardHeader } from './ui/Card';
import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import * as api from '../api/client';
import { usePaginatedResource } from '../hooks/usePaginatedResource';
import { formatDateTime } from '../utils/dateTime';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { useMediaQuery } from '../hooks/useMediaQuery';
import { IncidentResponse } from '../types';
import { IncidentEditorModal } from './IncidentEditorModal';
import { IncidentStatusSelect } from './IncidentStatusSelect';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

const PAGE_SIZE = 10;

interface IncidentsTabProps {
  token: string;
  roles: string[];
  canCreate: boolean;
  canChangeStatus: boolean;
}

export function IncidentsTab({ token, roles, canCreate, canChangeStatus }: IncidentsTabProps) {
  const navigate = useNavigate();
  const isMobile = useMediaQuery('(max-width: 640px)');
  const [paging, setPaging] = useState({ token, page: 0 });
  const page = paging.token === token ? paging.page : 0;
  const setPage = (nextPage: number) => setPaging({ token, page: nextPage });
  useEffect(() => setPaging({ token, page: 0 }), [token]);
  const { result, loading, error, reload, setResult } = usePaginatedResource({
    scopeKey: token, page,
    load: (requestedPage) => api.listIncidents(token, requestedPage, PAGE_SIZE),
    errorMessage: 'Не удалось загрузить инциденты', onPageCorrection: setPage,
  });
  const items = result?.items ?? [];
  const totalPages = result?.totalPages ?? 0;
  const [createModalOpen, setCreateModalOpen] = useState(false);

  const handleCreated = () => {
    setPage(0);
    reload();
  };

  const handleStatusChanged = (updated: IncidentResponse) => {
    setResult((previous) => previous && { ...previous,
      items: previous.items.map((item) => item.id === updated.id ? updated : item),
    });
  };

  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <h2>Инциденты</h2>
        {canCreate && (
          <Button type="button" variant="primary" onClick={() => setCreateModalOpen(true)}>
            Создать инцидент
          </Button>
        )}
      </CardHeader>

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
            <div className="data-table-wrapper">
              <table className="data-table">
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
                      className="data-table__row"
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
                      <td>{formatDateTime(incident.detectedAt)}</td>
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
                    {formatDateTime(incident.detectedAt)}
                  </p>
                  <p className="incidents-card__location">{incident.location}</p>
                </button>
              ))}
            </div>
          )}
        </>
      )}

      {totalPages > 0 && result && <Pagination page={result.page} totalPages={totalPages}
        totalElements={result.totalElements} loading={loading} onChange={setPage} />}

      <IncidentEditorModal
        token={token}
        open={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        onSaved={handleCreated}
      />
    </Card>
  );
}
