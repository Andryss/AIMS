import React, { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_STATUS_LABELS, MOVEMENT_OPERATION_LABELS, formatEquipmentDate, outstandingEquipment,
  positiveId } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { EquipmentMovementResponse, IncidentResponse } from '../types';
import { formatUserLabel } from '../utils/incidentUsers';
import { EquipmentMovementEditorModal } from './EquipmentMovementEditorModal';
import { EquipmentMovementHistory } from './EquipmentMovementHistory';
import { EquipmentMovementStatusSelect } from './EquipmentMovementStatusSelect';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { LoadingBlock } from './ui/LoadingBlock';
import { HistoryTab } from './ui/HistoryTab';

interface EquipmentMovementDetailPageProps {
  token: string;
  canCreate: boolean;
  canProcess: boolean;
  currentUserId?: number;
  canReadEquipment: boolean;
}

export function EquipmentMovementDetailPage({ token, canCreate, canProcess, currentUserId,
  canReadEquipment }: EquipmentMovementDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const movementId = positiveId(id);
  const activeRoute = useRef<{ id?: number; token: string } | null>(null);
  const location = useLocation();
  const navigate = useNavigate();
  const [movement, setMovement] = useState<EquipmentMovementResponse | null>(null);
  const [incident, setIncident] = useState<IncidentResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editor, setEditor] = useState<'edit' | 'return' | null>(null);
  const [revision, setRevision] = useState(0);
  const users = useUsersMap(token, [movement?.responsibleUserId, movement?.createdByUserId]);
  const fromIncident = positiveId(String(location.state?.incidentId ?? ''));
  const fromEquipment = positiveId(String(location.state?.equipmentId ?? ''));
  const fromIssue = positiveId(String(location.state?.sourceIssueId ?? ''));
  const backTo = fromIncident ? `/incidents/${fromIncident}` : fromEquipment ? `/equipment/${fromEquipment}`
    : fromIssue ? `/equipment-movements/${fromIssue}` : `/equipment-movements${location.search}`;
  const backLabel = fromIncident ? '← К инциденту' : fromEquipment ? '← К оборудованию'
    : fromIssue ? '← К выдаче' : '← К списку';

  useEffect(() => {
    activeRoute.current = { id: movementId, token };
    return () => { activeRoute.current = null; };
  }, [movementId, token]);

  const isCurrentRoute = () => activeRoute.current?.id === movementId && activeRoute.current?.token === token;

  useEffect(() => {
    setMovement(null); setIncident(null); setEditor(null); setError(null);
    if (!movementId) {
      setError('Некорректный идентификатор заявки'); setLoading(false); return;
    }

    let cancelled = false;
    setLoading(true);
    api.getEquipmentMovement(token, movementId).then(async (response) => {
      const relatedIncident = await api.getIncident(token, response.incidentId);
      if (!cancelled) { setMovement(response); setIncident(relatedIncident); }
    }).catch((err: unknown) => {
      if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить заявку'); }
    }).finally(() => { if (!cancelled) { setLoading(false); } });

    return () => { cancelled = true; };
  }, [token, movementId]);

  const incidentActive = incident != null && incident.status !== 'EXECUTION_COMPLETED';
  const canEdit = canCreate && incidentActive && movement?.status === 'PENDING'
    && (movement.createdByUserId === currentUserId || canProcess);
  const remaining = movement ? outstandingEquipment(movement.equipment, movement.id) : [];
  const canReturn = canCreate && incidentActive && movement?.operation === 'ISSUE'
    && movement.status === 'COMPLETED' && remaining.length > 0;

  return (
    <section className="card" aria-busy={loading}>
      <div className="card__header">
        <div className="knowledge-base__heading">
          <h2>{movement ? `Заявка #${movement.id} — ${MOVEMENT_OPERATION_LABELS[movement.operation]}` : 'Заявка на оборудование'}</h2>
          {canEdit && <Button variant="ghost" className="icon-button" aria-label="Редактировать заявку"
            title="Редактировать заявку" onClick={() => setEditor('edit')}>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"
              strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="m16 3 5 5M3 21l5-1L21 7a2.1 2.1 0 0 0-5-5L3 15z" />
            </svg>
          </Button>}
        </div>
        <Button variant="outline" to={backTo}>{backLabel}</Button>
      </div>
      {error && <div className="alert alert--error" role="alert">{error}</div>}
      {loading && <LoadingBlock label="Загрузка заявки…" skeleton rows={4} />}
      {!loading && movement && <>
        <section className="section">
          <dl className="data-list">
            <div className="data-list__row"><dt>Статус</dt><dd>
              <EquipmentMovementStatusSelect key={movement.id} token={token} movement={movement} canCreate={canCreate}
                canProcess={canProcess} currentUserId={currentUserId} disabled={!incidentActive}
                onChanged={(updated) => {
                  if (!isCurrentRoute()) { return; }
                  setMovement(updated);
                  setRevision((value) => value + 1);
                }} />
            </dd></div>
            <div className="data-list__row"><dt>Инцидент</dt><dd><Link to={`/incidents/${movement.incidentId}`}>
              Инцидент #{movement.incidentId}</Link>{incident ? ` — ${incident.location}` : ''}</dd></div>
            <div className="data-list__row"><dt>Ответственный</dt><dd>
              <UserChip login={formatUserLabel(movement.responsibleUserId, users)} />
            </dd></div>
            <div className="data-list__row"><dt>Автор</dt><dd>
              <UserChip login={formatUserLabel(movement.createdByUserId, users)} />
            </dd></div>
            <div className="data-list__row"><dt>Создана</dt><dd>{formatEquipmentDate(movement.createdAt)}</dd></div>
            {movement.completedAt && <div className="data-list__row"><dt>Выполнена</dt><dd>
              {formatEquipmentDate(movement.completedAt)}</dd></div>}
            {movement.sourceIssueMovementId && <div className="data-list__row"><dt>Исходная выдача</dt><dd>
              <Link to={`/equipment-movements/${movement.sourceIssueMovementId}`}>
                Заявка #{movement.sourceIssueMovementId}</Link></dd></div>}
            {movement.comment && <div className="data-list__row"><dt>Комментарий</dt><dd className="text-pre-wrap">
              {movement.comment}</dd></div>}
          </dl>
        </section>
        <section className="section"><h3>Оборудование · {movement.equipment.length}</h3>
          <div className="incidents-table-wrapper"><table className="incidents-table">
            <thead><tr><th>Модель</th><th>Серийный номер</th><th>Текущее состояние</th></tr></thead>
            <tbody>{movement.equipment.map((item) => <tr key={item.id}>
              <td>{canReadEquipment ? <Link to={`/equipment/${item.id}`} state={{ movementId: movement.id }}
                className="knowledge-base__name">{item.model}</Link> : item.model}</td>
              <td>{item.serialNumber}</td><td>{EQUIPMENT_STATUS_LABELS[item.status]}</td>
            </tr>)}</tbody>
          </table></div>
          {movement.operation === 'ISSUE' && movement.status === 'COMPLETED' && <p>
            Не возвращено по этой выдаче: {remaining.length}
          </p>}
          {canReturn && <Button onClick={() => setEditor('return')}>Оформить возврат</Button>}
        </section>
        <HistoryTab>
          <EquipmentMovementHistory key={movement.id} token={token} movementId={movement.id} revision={revision} />
        </HistoryTab>
        {editor && <EquipmentMovementEditorModal token={token} open incidentId={movement.incidentId}
          movement={editor === 'edit' ? movement : null} sourceIssue={editor === 'return' ? movement : null}
          onClose={() => { if (isCurrentRoute()) { setEditor(null); } }} onSaved={(saved) => {
            if (!isCurrentRoute()) { return; }
            if (editor === 'edit') { setMovement(saved); setRevision((value) => value + 1); }
            else { navigate(`/equipment-movements/${saved.id}`, { state: { sourceIssueId: movement.id } }); }
          }} />}
      </>}
    </section>
  );
}
