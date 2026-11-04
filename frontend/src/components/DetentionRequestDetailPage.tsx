import { formatDateTime } from '../utils/dateTime';
import { positiveId } from '../utils/params';
import { formatUserLabel } from '../utils/users';
import { Card, CardHeader } from './ui/Card';
import { EditIcon } from './ui/EditIcon';
import React, { useEffect, useRef, useState } from 'react';
import { Link, useLocation, useParams } from 'react-router-dom';
import * as api from '../api/client';
import { canEditDetentionRequest, isDetentionOverdue } from '../detentionLabels';

import { useUsersMap } from '../hooks/useUsersMap';
import { DetentionRequestResponse } from '../types';

import { DetentionRequestEditorModal } from './DetentionRequestEditorModal';
import { DetentionRequestHistory } from './DetentionRequestHistory';
import { DetentionRequestStatusSelect } from './DetentionRequestStatusSelect';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { HistoryTab } from './ui/HistoryTab';
import { LoadingBlock } from './ui/LoadingBlock';

interface DetentionRequestDetailPageProps {
  token: string; permissions: string[]; currentUserId?: number; canReadIncidents?: boolean; canReadAliens?: boolean;
}

export function DetentionRequestDetailPage(props: DetentionRequestDetailPageProps) {
  const { id } = useParams<{ id: string }>();
  const requestId = positiveId(id);
  return requestId ? <DetentionCard key={`${props.token}:${requestId}`} {...props} id={requestId} />
    : <Card><div className="alert alert--error" role="alert">Некорректный идентификатор заявки</div></Card>;
}

function DetentionCard({ token, permissions, currentUserId, canReadIncidents = false, canReadAliens = false, id }:
  DetentionRequestDetailPageProps & { id: number }) {
  const location = useLocation();
  const fromIncidentId = positiveId(String((location.state as { incidentId?: number } | null)?.incidentId ?? ''));
  const [request, setRequest] = useState<DetentionRequestResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [editorOpen, setEditorOpen] = useState(false);
  const [changingStatus, setChangingStatus] = useState(false);
  const [revision, setRevision] = useState(0);
  const [alienName, setAlienName] = useState('');
  const alive = useRef(false);
  const generation = useRef(0);
  const users = useUsersMap(token, [request?.createdByUserId, request?.reviewedByUserId]);
  const alienId = request?.alienId;

  useEffect(() => {
    alive.current = true;
    const version = ++generation.current;
    api.getDetentionRequest(token, id)
      .then((value) => { if (alive.current && version === generation.current) { setRequest(value); } })
      .catch((err: unknown) => { if (alive.current && version === generation.current) { setError(err instanceof Error ? err.message : 'Не удалось загрузить заявку'); } })
      .finally(() => { if (alive.current && version === generation.current) { setLoading(false); } });
    return () => { alive.current = false; generation.current += 1; };
  }, [token, id]);

  useEffect(() => {
    let cancelled = false; setAlienName('');
    if (alienId == null || !canReadAliens) { return; }
    api.getAlien(token, alienId).then((value) => { if (!cancelled) { setAlienName(value.name); } }).catch(() => {});
    return () => { cancelled = true; };
  }, [token, alienId, canReadAliens]);

  const changed = (saved: DetentionRequestResponse) => {
    if (!alive.current) { return; }
    generation.current += 1;
    setRequest(saved); setRevision((value) => value + 1);
  };
  const editable = request != null && canEditDetentionRequest(request, permissions, currentUserId);

  return <Card  aria-busy={loading}>
    <CardHeader><h2>{request ? `Заявка на заключение #${request.id}` : 'Заявка на заключение'}</h2>
      <div className="card__actions" role="group" aria-label="Действия заявки">
        {editable && <Button variant="ghost" className="icon-button" aria-label="Редактировать заявку" title="Редактировать заявку"
          disabled={changingStatus}
          onClick={() => setEditorOpen(true)}><EditIcon /></Button>}
        {fromIncidentId && canReadIncidents ? <Button variant="outline" to={`/incidents/${fromIncidentId}`}>← К инциденту</Button>
          : <Button variant="outline" to={`/detention-requests${location.search}`}>← К списку</Button>}
      </div>
    </CardHeader>
    {loading && <LoadingBlock label="Загрузка заявки…" />}
    {error && <div className="alert alert--error" role="alert">{error}</div>}
    {!loading && request && <>
      <section className="section"><dl className="data-list">
        <div className="data-list__row"><dt>Статус</dt><dd><DetentionRequestStatusSelect key={request.status} token={token}
          request={request} permissions={permissions} currentUserId={currentUserId} disabled={editorOpen}
          onSavingChange={setChangingStatus} onChanged={changed} /></dd></div>
        <div className="data-list__row"><dt>Субъект</dt><dd>{request.subjectName}</dd></div>
        <div className="data-list__row"><dt>Описание субъекта</dt><dd className="text-pre-wrap">{request.subjectDescription}</dd></div>
        <div className="data-list__row"><dt>Вид</dt><dd>{alienId != null ? canReadAliens
          ? <Link to={`/aliens/${alienId}`}>{alienName || `Вид #${alienId}`}</Link> : `Вид #${alienId}` : '—'}</dd></div>
        <div className="data-list__row"><dt>Срок, суток</dt><dd>{request.durationDays}</dd></div>
        <div className="data-list__row"><dt>Инцидент-основание</dt><dd>{canReadIncidents
          ? <Link to={`/incidents/${request.incidentId}`}>Инцидент #{request.incidentId}</Link> : `Инцидент #${request.incidentId}`}</dd></div>
        <div className="data-list__row"><dt>Автор</dt><dd><UserChip login={formatUserLabel(request.createdByUserId, users)} size={24} /></dd></div>
        <div className="data-list__row"><dt>Рассмотрел</dt><dd>{request.reviewedByUserId != null
          ? <UserChip login={formatUserLabel(request.reviewedByUserId, users)} size={24} /> : '—'}</dd></div>
        <div className="data-list__row"><dt>Создана</dt><dd>{formatDateTime(request.createdAt)}</dd></div>
        <div className="data-list__row"><dt>Дата прибытия</dt><dd>{request.admittedAt ? formatDateTime(request.admittedAt) : '—'}</dd></div>
        <div className="data-list__row"><dt>Плановое освобождение</dt><dd>{request.plannedReleaseAt ? formatDateTime(request.plannedReleaseAt) : '—'}</dd></div>
        <div className="data-list__row"><dt>Фактическое освобождение</dt><dd>{request.releasedAt ? formatDateTime(request.releasedAt) : '—'}</dd></div>
      </dl>
        {isDetentionOverdue(request) && <div className="alert alert--error" role="status">Срок заключения истёк</div>}
      </section>
      <HistoryTab><DetentionRequestHistory token={token} requestId={id} revision={revision}
        canReadIncidents={canReadIncidents} canReadAliens={canReadAliens} /></HistoryTab>
      <DetentionRequestEditorModal token={token} open={editorOpen && editable} incidentId={request.incidentId} request={request}
        canReadAliens={canReadAliens} onClose={() => setEditorOpen(false)} onSaved={changed} />
    </>}
  </Card>;
}
