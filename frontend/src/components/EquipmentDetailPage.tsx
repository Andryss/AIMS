import { formatDateTime } from '../utils/dateTime';
import { positiveId } from '../utils/params';
import { formatUserLabel } from '../utils/users';
import { Card, CardHeader } from './ui/Card';
import { EditIcon } from './ui/EditIcon';
import React, { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_STATUS_LABELS, EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentResponse } from '../types';

import { EquipmentEditorModal } from './EquipmentEditorModal';
import { EquipmentHistory } from './EquipmentHistory';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { LoadingBlock } from './ui/LoadingBlock';
import { StatusChip } from './ui/StatusChip';
import { HistoryTab } from './ui/HistoryTab';

interface EquipmentDetailPageProps { token: string; canWrite: boolean; canReadMovements: boolean; canReadTenders?: boolean; }

export function EquipmentDetailPage({ token, canWrite, canReadMovements, canReadTenders = false }: EquipmentDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const equipmentId = positiveId(id);
  const activeRoute = useRef<{ id?: number; token: string } | null>(null);
  const location = useLocation();
  const movementId = positiveId(String(location.state?.movementId ?? ''));
  const [equipment, setEquipment] = useState<EquipmentResponse | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [revision, setRevision] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const users = useUsersMap(token, [equipment?.holderUserId]);

  useEffect(() => {
    activeRoute.current = { id: equipmentId, token };
    return () => { activeRoute.current = null; };
  }, [equipmentId, token]);

  useEffect(() => {
    setEquipment(null);
    setError(null);
    setEditorOpen(false);
    if (!equipmentId) {
      setError('Некорректный идентификатор оборудования');
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    api.getEquipment(token, equipmentId)
      .then((response) => { if (!cancelled) { setEquipment(response); } })
      .catch((err: unknown) => {
        if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить оборудование'); }
      })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, equipmentId]);

  return (
    <Card  aria-busy={loading}>
      <CardHeader>
        <h2>{equipment?.model ?? 'Карточка оборудования'}</h2>
        <div className="card__actions" role="group" aria-label="Действия оборудования">
          {!loading && equipment && canWrite && <Button variant="ghost" className="icon-button"
            aria-label="Редактировать оборудование" title="Редактировать оборудование" onClick={() => setEditorOpen(true)}>
            <EditIcon />
          </Button>}
          {!loading && equipment && canReadMovements && <Button variant="outline"
            to={`/equipment-movements?equipmentId=${equipment.id}`}>Все движения оборудования</Button>}
          <Button variant="outline" to={movementId ? `/equipment-movements/${movementId}` : `/equipment${location.search}`}>
            {movementId ? '← К заявке' : '← К списку'}
          </Button>
        </div>
      </CardHeader>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка оборудования…" skeleton rows={4} />}
      {!loading && equipment && <>
        <section className="section">
          <dl className="data-list">
            <div className="data-list__row"><dt>Статус</dt><dd><StatusChip status={equipment.status.toLowerCase()}
              label={EQUIPMENT_STATUS_LABELS[equipment.status]} /></dd></div>
            <div className="data-list__row"><dt>Тип</dt><dd>{EQUIPMENT_TYPE_LABELS[equipment.type]}</dd></div>
            <div className="data-list__row"><dt>Серийный номер</dt><dd>{equipment.serialNumber}</dd></div>
            <div className="data-list__row"><dt>Держатель</dt><dd>{equipment.holderUserId
              ? <UserChip login={formatUserLabel(equipment.holderUserId, users)} /> : '—'}</dd></div>
            {equipment.currentIssueMovementId != null && <div className="data-list__row"><dt>Текущая выдача</dt><dd>
              {canReadMovements ? <Link to={`/equipment-movements/${equipment.currentIssueMovementId}`}
                state={{ equipmentId }}>Заявка #{equipment.currentIssueMovementId}</Link>
                : `Заявка #${equipment.currentIssueMovementId}`}
            </dd></div>}
            {equipment.sourceTenderId != null && <div className="data-list__row"><dt>Тендер-основание</dt><dd>
              {canReadTenders ? <Link to={`/tenders/${equipment.sourceTenderId}`}>Тендер #{equipment.sourceTenderId}</Link>
                : `Тендер #${equipment.sourceTenderId}`}
            </dd></div>}
            <div className="data-list__row"><dt>Добавлено</dt><dd>{formatDateTime(equipment.createdAt)}</dd></div>
          </dl>
        </section>
        <HistoryTab>
          <EquipmentHistory key={equipment.id} token={token} equipmentId={equipment.id} revision={revision}
            canReadMovements={canReadMovements} canReadTenders={canReadTenders} />
        </HistoryTab>
      </>}
      {equipment && <EquipmentEditorModal token={token} open={editorOpen && canWrite} equipment={equipment}
        onClose={() => {
          if (activeRoute.current?.id === equipmentId && activeRoute.current?.token === token) { setEditorOpen(false); }
        }} onSaved={(saved) => {
          if (activeRoute.current?.id !== equipmentId || activeRoute.current?.token !== token) { return; }
          setEquipment(saved);
          setRevision((value) => value + 1);
        }} />}
    </Card>
  );
}
