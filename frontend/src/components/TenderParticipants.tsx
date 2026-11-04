import { formatDateTime } from '../utils/dateTime';
import { formatUserLabel } from '../utils/users';
import { CardHeader } from './ui/Card';
import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { EQUIPMENT_PAGE_SIZE } from '../equipmentLabels';
import { useMutation } from '../hooks/useMutation';
import { Modal } from './ui/Modal';
import { useUsersMap } from '../hooks/useUsersMap';
import { TENDER_PARTICIPANT_STATUS_LABELS } from '../tenderLabels';
import { TenderParticipantListResponse, TenderParticipantResponse, TenderResponse } from '../types';

import { TenderParticipantEditorModal } from './TenderParticipantEditorModal';
import { UserChip } from './UserChip';
import { Button } from './ui/Button';
import { EmptyState } from './ui/EmptyState';
import { LoadingBlock } from './ui/LoadingBlock';
import { Pagination } from './ui/Pagination';
import { StatusChip } from './ui/StatusChip';

interface TenderParticipantsProps {
  token: string; tender: TenderResponse; canManage: boolean;
  onChanged: (tender?: TenderResponse) => void; onCountChange?: (count: number) => void;
}

export function TenderParticipants({ token, tender, canManage, onChanged, onCountChange }: TenderParticipantsProps) {
  const [page, setPage] = useState(0);
  const [result, setResult] = useState<TenderParticipantListResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [revision, setRevision] = useState(0);
  const [editorOpen, setEditorOpen] = useState(false);
  const [editing, setEditing] = useState<TenderParticipantResponse | null>(null);
  const [selected, setSelected] = useState<TenderParticipantResponse | null>(null);
  const scopeKey = JSON.stringify([token, tender.id, tender.status]);
  const mutation = useMutation(scopeKey);
  const { busy: saving } = mutation;
  const visibleError = error ?? mutation.error;
  const writable = canManage && tender.status === 'COLLECTING_OFFERS';
  const users = useUsersMap(token, result?.items.map((item) => item.createdByUserId) ?? []);

  useEffect(() => { setSelected(null); setEditorOpen(false); }, [scopeKey]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setResult(null);
    setError(null);
    api.listTenderParticipants(token, tender.id, page, EQUIPMENT_PAGE_SIZE)
      .then((response) => {
        if (cancelled) { return; }
        if (page > 0 && page >= Math.max(response.totalPages, 1)) { setPage(Math.max(response.totalPages - 1, 0)); return; }
        setResult(response);
        onCountChange?.(response.totalElements);
      })
      .catch((err: unknown) => { if (!cancelled) { setError(err instanceof Error ? err.message : 'Не удалось загрузить участников'); } })
      .finally(() => { if (!cancelled) { setLoading(false); } });
    return () => { cancelled = true; };
  }, [token, tender.id, tender.status, page, revision, onCountChange]);

  const choose = async () => {
    if (!selected || !writable) { return; }
    const session = mutation.start();
    if (!session) { return; }
    setError(null);
    try {
      const saved = await api.selectTenderWinner(token, tender.id, { participantId: selected.id });
      if (!session.isCurrent()) { return; }
      setSelected(null);
      setRevision((value) => value + 1);
      onChanged(saved);
    } catch (err: unknown) {
      session.fail(err, 'Не удалось выбрать победителя');
    } finally {
      session.finish();
    }
  };

  return <section className="section" id="participants" aria-label="Участники тендера" aria-busy={loading}>
    <CardHeader><h3>Участники и предложения</h3>
      {writable && <Button variant="outline" onClick={() => { setEditing(null); setEditorOpen(true); }}>Добавить участника</Button>}
    </CardHeader>
    {visibleError && !selected && <div className="alert alert--error" role="alert">{visibleError}</div>}
    {loading && <LoadingBlock label="Загрузка участников…" inline />}
    {!loading && !error && result?.items.length === 0 && <EmptyState title="Предложений пока нет" />}
    {!loading && result && result.items.length > 0 && <>
      <div className="data-table-wrapper"><table className="data-table">
        <thead><tr><th>Организация</th><th>Предложение</th><th>Статус</th><th>Добавил</th><th>Дата</th>{writable && <th>Действия</th>}</tr></thead>
        <tbody>{result.items.map((item) => <tr key={item.id}>
          <td>{item.name}</td><td className="text-pre-wrap">{item.proposal}</td>
          <td><StatusChip status={item.status === 'WINNER' ? 'available' : 'pending'} label={TENDER_PARTICIPANT_STATUS_LABELS[item.status]} /></td>
          <td><UserChip login={formatUserLabel(item.createdByUserId, users)} size={24} /></td>
          <td>{formatDateTime(item.createdAt)}</td>
          {writable && <td><div className="page-actions">
            <Button variant="ghost" onClick={() => { setEditing(item); setEditorOpen(true); }}>Редактировать предложение</Button>
            <Button variant="outline" disabled={saving} onClick={() => { setSelected(item); setError(null); mutation.setError(null); }}>Выбрать победителя</Button>
          </div></td>}
        </tr>)}</tbody>
      </table></div>
      <Pagination page={page} totalPages={result.totalPages} totalElements={result.totalElements} loading={loading} onChange={setPage} />
    </>}
    <TenderParticipantEditorModal token={token} open={editorOpen && writable} tenderId={tender.id} participant={editing}
      onClose={() => setEditorOpen(false)} onSaved={() => {
        onCountChange?.(Math.max((result?.totalElements ?? 0) + (editing ? 0 : 1), 1));
        setRevision((value) => value + 1);
        onChanged();
      }} />
    <Modal open={selected != null} title="Выбрать победителя" busy={saving} className="modal--narrow"
      onClose={() => { if (!mutation.isBusy()) { setSelected(null); } }}>
        <p>Победитель: {selected?.name}. Тендер перейдёт в ожидание поставки, предложения будут зафиксированы.</p>
        {visibleError && <div className="alert alert--error" role="alert">{visibleError}</div>}
        <div className="modal-actions"><Button variant="secondary" disabled={saving} onClick={() => { if (!mutation.isBusy()) { setSelected(null); } }}>Отмена</Button>
          <Button loading={saving} onClick={() => { void choose(); }}>Подтвердить</Button></div>
    </Modal>
  </section>;
}
