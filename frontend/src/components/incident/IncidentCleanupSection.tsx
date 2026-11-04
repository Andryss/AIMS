import React, { useState } from 'react';
import { CleanupReportResponse, IncidentResponse } from '../../types';
import { CleanupStatusSelect } from '../CleanupStatusSelect';
import { CleanupReportDrawer } from '../CleanupReportDrawer';

interface Props { token: string; incident: IncidentResponse; canReadCleanupReport: boolean;
  canCreateCleanupReport: boolean; canChangeCleanupStatus: boolean; cleanupReport: CleanupReportResponse | null;
  onChanged: (incident: IncidentResponse) => void; onCreated: (report: CleanupReportResponse) => void;
  handleDownload: (id: number, name?: string) => void; downloadingId: number | null; }
export function IncidentCleanupSection({ token, incident, canCreateCleanupReport, canChangeCleanupStatus,
  cleanupReport, onChanged, onCreated, handleDownload, downloadingId }: Props) {
  const [cleanupDrawerOpen, setCleanupDrawerOpen] = useState(false);
  const [cleanupDrawerMode, setCleanupDrawerMode] = useState<'create' | 'view'>('view');
  const showCleanupBlock = incident?.status === 'EXECUTING'
    || incident?.status === 'EXECUTION_COMPLETED';

  const showCleanupStatusRow = showCleanupBlock && (
    incident?.cleanupStatus != null || canChangeCleanupStatus
  );

  const showCleanupReportRow = showCleanupBlock && (
    incident?.cleanupReportId != null || canCreateCleanupReport
  );
  return <>
  {showCleanupStatusRow && (
    <div className="data-list__row">
      <dt>Статус очистки</dt>
      <dd>
        <CleanupStatusSelect
          token={token}
          incident={incident}
          canChange={canChangeCleanupStatus}
          className="status-select-wrap status-select-wrap--modal"
          onStatusChanged={onChanged}
        />
      </dd>
    </div>
  )}

  {showCleanupReportRow && (
    <div className="data-list__row">
      <dt>Отчёт об очистке</dt>
      <dd>
        {incident.cleanupReportId != null ? (
          <button
            type="button"
            className="btn btn--outline"
            onClick={() => {
              setCleanupDrawerMode('view');
              setCleanupDrawerOpen(true);
            }}
          >
            Открыть
          </button>
        ) : canCreateCleanupReport ? (
          <button
            type="button"
            className="btn btn--outline"
            onClick={() => {
              setCleanupDrawerMode('create');
              setCleanupDrawerOpen(true);
            }}
          >
            Прикрепить отчёт
          </button>
        ) : (
          '—'
        )}

        <CleanupReportDrawer
          token={token}
          incidentId={incident.id}
          open={cleanupDrawerOpen}
          mode={cleanupDrawerMode}
          report={cleanupReport}
          onClose={() => setCleanupDrawerOpen(false)}
          onCreated={onCreated}
          onDownloadFile={handleDownload}
          downloadingId={downloadingId}
        />
      </dd>
    </div>
  )}
  </>;
}
