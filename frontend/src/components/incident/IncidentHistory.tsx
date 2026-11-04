import { formatUserLabel as userLoginFromMap } from '../../utils/users';
import React, { useEffect, useMemo, useState } from 'react';
import * as api from '../../api/client';
import { IncidentHistoryEntry, IncidentHistorySnapshot } from '../../types';
import { buildIncidentHistoryDiffs } from '../../utils/incidentHistoryDiff';
import { formatDateTime as formatDate } from '../../utils/dateTime';

import { UserChip } from '../UserChip';
import { LoadingBlock } from '../ui/LoadingBlock';
import { EmptyState } from '../ui/EmptyState';
import { HistoryTimeline } from '../ui/HistoryTimeline';
import { HistoryDiffList } from '../ui/HistoryDiffList';

interface Props { token: string; canReadAliens: boolean; historyEntries: IncidentHistoryEntry[];
  usersMap: Map<number, string>; historyLoading: boolean; }
export function IncidentHistory({ token, canReadAliens, historyEntries, usersMap, historyLoading }: Props) {
  const [alienNames, setAlienNames] = useState<Record<number, string>>({});
  const historyDiffs = useMemo(() => buildIncidentHistoryDiffs(historyEntries, usersMap), [historyEntries, usersMap]);
  useEffect(() => {
    if (!canReadAliens || historyEntries.length === 0) {
      return;
    }
    const alienIds = new Set<number>();
    historyEntries.forEach((entry) => {
      if (entry.snapshot.alienId != null) {
        alienIds.add(entry.snapshot.alienId);
      }
    });
    const missing = Array.from(alienIds).filter((id) => alienNames[id] == null);
    if (missing.length === 0) {
      return;
    }
    let cancelled = false;
    (async () => {
      const resolved: Record<number, string> = {};
      await Promise.all(
        missing.map(async (alienId) => {
          try {
            const alien = await api.getAlien(token, alienId);
            resolved[alienId] = alien.name;
          } catch {
            resolved[alienId] = `#${alienId}`;
          }
        }),
      );
      if (!cancelled) {
        setAlienNames((prev) => ({ ...prev, ...resolved }));
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [alienNames, canReadAliens, historyEntries, token]);

  const formatAlienLabel = (alienId: number | null | undefined): string => {
    if (alienId == null) {
      return '—';
    }
    const name = alienNames[alienId];
    return name ? `${name} (#${alienId})` : `#${alienId}`;
  };

  const enrichAlienDiffValue = (label: string, value: string): string => {
    if (label !== 'Тип инопланетянина' || value === '—') {
      return value;
    }
    const match = value.match(/^#(\d+)$/);
    if (!match) {
      return value;
    }
    return formatAlienLabel(Number(match[1]));
  };

  const renderHistoryValue = (label: string, value: string, snapshot?: IncidentHistorySnapshot): React.ReactNode => {
    if (label === 'Ответственный') {
      return snapshot?.responsibleUserId != null
        ? <UserChip login={userLoginFromMap(snapshot.responsibleUserId, usersMap)} size={24} /> : '—';
    }
    if (label === 'Исполнители') {
      return snapshot?.executorUserIds?.length ? snapshot.executorUserIds.map((userId) => (
        <UserChip key={userId} login={userLoginFromMap(userId, usersMap)} size={24} />
      )) : '—';
    }
    return enrichAlienDiffValue(label, value);
  };
  return <>
    {historyLoading && <LoadingBlock label="Загрузка истории…" inline />}
    {!historyLoading && historyDiffs.length === 0 && (
      <EmptyState title="История изменений пуста" />
    )}
    <HistoryTimeline entries={historyDiffs.map((block) => ({ ...block, id: block.entry.id }))}
      renderHeader={(block) => <>
        {block.isCreation ? 'Создание инцидента · ' : ''}
        <span className="history-timeline__when">{formatDate(block.entry.changedAt)}</span>
        <span aria-hidden>·</span>
        <UserChip login={userLoginFromMap(block.entry.changedByUserId, usersMap)} size={24} />
      </>}>
      {(block, previous) => <HistoryDiffList rows={block.rows.map((row) => ({
        label: row.label,
        oldValue: block.isCreation ? undefined : renderHistoryValue(row.label, row.oldValue, previous?.entry.snapshot),
        newValue: renderHistoryValue(row.label, row.newValue, block.entry.snapshot),
      }))} renderValue={(value) => value} emptyMessage={block.isCreation ? undefined : 'Без изменений полей'} />}
    </HistoryTimeline>
  </>;
}
