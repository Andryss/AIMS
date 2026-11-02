import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE } from '../equipmentLabels';
import { EquipmentMovementListResponse, IncidentStatus } from '../types';
import { EquipmentMovementEditorModal } from './EquipmentMovementEditorModal';
import { EquipmentMovementTable } from './EquipmentMovementTable';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';

interface IncidentEquipmentSectionProps {
  token: string;
  incidentId: number;
  incidentStatus: IncidentStatus;
  currentUserId?: number;
  canRead: boolean;
  canCreate: boolean;
}

export function IncidentEquipmentSection({ token, incidentId, incidentStatus, canRead,
  canCreate }: IncidentEquipmentSectionProps) {
  const [result, setResult] = useState<EquipmentMovementListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [open, setOpen] = useState(false);
  const [revision, setRevision] = useState(0);
  const [page, setPage] = useState(0);

  useEffect(() => { setPage(0); setOpen(false); }, [incidentId]);

  useEffect(() => {
    if (!canRead) { return; }
    let cancelled = false;
    setLoading(true); setError(null);

    api.listEquipmentMovements(token, { incidentId, page, size: EQUIPMENT_PAGE_SIZE })
      .then((response) => { if (!cancelled) { setResult(response); } })
      .catch((err: unknown) => {
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить оборудование инцидента'); }
      }).finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, incidentId, incidentStatus, canRead, revision, page]);

  if (!canRead) { return null; }

  return (
    <section className="section" aria-label="Оборудование инцидента" aria-busy={loading}>
      {canCreate && incidentStatus !== 'EXECUTION_COMPLETED' && <div className="card__header">
        <Button onClick={() => setOpen(true)}>
          Заявка на выдачу
        </Button>
      </div>}
      {loading && <LoadingBlock label="Загрузка оборудования инцидента…" inline />}
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {!loading && !error && result && <>
        {result.items.length === 0 ? <EmptyState title="Заявок на оборудование пока нет" />
          : <EquipmentMovementTable token={token} items={result.items}
            fromIncidentId={incidentId} />}
        <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} onChange={setPage} />
        <Button variant="outline" to={`/equipment-movements?incidentId=${incidentId}`}>Все заявки по инциденту</Button>
      </>}
      {open && <EquipmentMovementEditorModal token={token} open incidentId={incidentId}
        onClose={() => setOpen(false)} onSaved={() => { setPage(0); setRevision((value) => value + 1); }} />}
    </section>
  );
}
