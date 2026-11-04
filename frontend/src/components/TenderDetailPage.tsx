import { formatDateTime } from '../utils/dateTime';
import { positiveId } from '../utils/params';
import { formatUserLabel } from '../utils/users';
import { Card, CardHeader } from './ui/Card';
import { EditIcon } from './ui/EditIcon';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { useUsersMap } from '../hooks/useUsersMap';
import { canEditTender } from '../tenderLabels';
import { TenderResponse } from '../types';

import { TenderEditorModal } from './TenderEditorModal';
import { TenderHistory } from './TenderHistory';
import { TenderParticipants } from './TenderParticipants';
import { TenderReceiptModal } from './TenderReceiptModal';
import { TenderStatusSelect } from './TenderStatusSelect';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { HistoryTab } from './ui/HistoryTab';
import { LoadingBlock } from './ui/LoadingBlock';

interface TenderDetailPageProps {
  token: string; permissions: string[]; currentUserId?: number; canReadIncidents?: boolean; canReadEquipment?: boolean;
}

export function TenderDetailPage(props: TenderDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const tenderId = positiveId(id);
  return tenderId ? <TenderCard key={`${props.token}:${tenderId}`} {...props} id={tenderId} />
    : <Card><div className="alert alert--error" role="alert">Некорректный идентификатор тендера</div></Card>;
}

function TenderCard({ token, permissions, currentUserId, id, canReadIncidents = true, canReadEquipment = true }:
  TenderDetailPageProps & { id: number }) {
  const location = useLocation();
  const [tender, setTender] = useState<TenderResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [receiptOpen, setReceiptOpen] = useState(false);
  const [offersCount, setOffersCount] = useState<number | null>(null);
  const [receivedIds, setReceivedIds] = useState<number[]>([]);
  const [revision, setRevision] = useState(0);
  const alive = useRef(false);
  const request = useRef(0);
  const users = useUsersMap(token, [tender?.createdByUserId]);

  useEffect(() => {
    alive.current = true;
    const version = ++request.current;
    api.getTender(token, id)
      .then((response) => { if (alive.current && version === request.current) { setTender(response); } })
      .catch((err: unknown) => {
        if (alive.current && version === request.current) { setError(err instanceof Error ? err.message : 'Не удалось загрузить тендер'); }
      })
      .finally(() => { if (alive.current && version === request.current) { setLoading(false); } });
    return () => { alive.current = false; request.current += 1; };
  }, [token, id]);

  const changed = useCallback((saved?: TenderResponse) => {
    if (!alive.current) { return; }
    const version = ++request.current;
    setError(null);
    setRevision((value) => value + 1);
    if (saved) { setTender(saved); return; }
    api.getTender(token, id)
      .then((response) => { if (alive.current && request.current === version) { setTender(response); } })
      .catch((err: unknown) => {
        if (alive.current && request.current === version) { setError(err instanceof Error ? err.message : 'Не удалось обновить тендер'); }
      });
  }, [token, id]);

  const editable = tender != null && canEditTender(tender, permissions, currentUserId);
  const receivable = tender?.status === 'AWAITING_DELIVERY' && tender.winnerParticipantId != null
    && permissions.includes('TENDER_RECEIVE');

  return <Card  aria-busy={loading}>
    <CardHeader>
      <h2>{tender ? `Тендер #${tender.id}` : 'Карточка тендера'}</h2>
      <div className="card__actions" role="group" aria-label="Действия тендера">
      {editable && <Button variant="ghost" className="icon-button" aria-label="Редактировать тендер" title="Редактировать тендер"
        onClick={() => setEditorOpen(true)}><EditIcon /></Button>}
        {receivable && <Button onClick={() => setReceiptOpen(true)}>Принять поставку</Button>}
        <Button variant="outline" to={`/tenders${location.search}`}>← К списку</Button>
      </div>
    </CardHeader>
    {loading && <LoadingBlock label="Загрузка тендера…" />}
    {error && <div className="alert alert--error" role="alert">{error}</div>}
    {!loading && tender && <>
      <section className="section">
        <dl className="data-list">
          <div className="data-list__row"><dt>Статус</dt><dd>
            <TenderStatusSelect key={tender.status} token={token} tender={tender} permissions={permissions}
              currentUserId={currentUserId} onChanged={changed} />
          </dd></div>
          <div className="data-list__row"><dt>Тип оборудования</dt><dd>{EQUIPMENT_TYPE_LABELS[tender.equipmentType]}</dd></div>
          <div className="data-list__row"><dt>Количество оборудования</dt><dd>{tender.quantity}</dd></div>
          <div className="data-list__row"><dt>Условия закупки</dt><dd className="text-pre-wrap">{tender.conditions || '—'}</dd></div>
          <div className="data-list__row"><dt>Инцидент-основание</dt><dd>{tender.incidentId != null
            ? canReadIncidents ? <Link to={`/incidents/${tender.incidentId}`}>Инцидент #{tender.incidentId}</Link> : `Инцидент #${tender.incidentId}` : '—'}</dd></div>
          <div className="data-list__row"><dt>Автор</dt><dd><UserChip login={formatUserLabel(tender.createdByUserId, users)} size={24} /></dd></div>
          <div className="data-list__row"><dt>Победитель</dt><dd>{tender.winnerParticipantId != null
            ? <a href="#participants" className="entity-link">Участник #{tender.winnerParticipantId}</a> : '—'}</dd></div>
          <div className="data-list__row"><dt>Создан</dt><dd>{formatDateTime(tender.createdAt)}</dd></div>
          {tender.completedAt && <div className="data-list__row"><dt>Завершён</dt><dd>{formatDateTime(tender.completedAt)}</dd></div>}
        </dl>
        {receivedIds.length > 0 && <div className="alert alert--success" role="status">Поставка принята.{' '}
          {receivedIds.map((value, index) => <React.Fragment key={value}>{index > 0 && ', '}
            {canReadEquipment ? <Link to={`/equipment/${value}`}>Оборудование #{value}</Link> : `Оборудование #${value}`}
          </React.Fragment>)}
        </div>}
      </section>
      <TenderParticipants token={token} tender={tender} canManage={permissions.includes('TENDER_MANAGE')}
        onChanged={changed} onCountChange={setOffersCount} />
      <HistoryTab><TenderHistory token={token} tenderId={id} revision={revision}
        canReadIncidents={canReadIncidents} canReadEquipment={canReadEquipment} /></HistoryTab>
      <TenderEditorModal token={token} open={editorOpen && editable} tender={tender} hasOffers={offersCount !== 0}
        onClose={() => setEditorOpen(false)} onSaved={changed} />
      <TenderReceiptModal token={token} open={receiptOpen && receivable} tender={tender}
        onClose={() => setReceiptOpen(false)} onSaved={(response) => {
          if (!alive.current) { return; }
          setReceivedIds(response.equipment.map((item) => item.id)); changed(response.tender);
        }} />
    </>}
  </Card>;
}
