import { formatUserLabel as userLoginFromMap } from '../../utils/users';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import * as api from '../../api/client';
import { IncidentResponse, UserSummary } from '../../types';

import { UserChip } from '../UserChip';
import { UserPickerDrawer } from '../UserPickerDrawer';
import { Spinner } from '../ui/Spinner';

function sameUserIdSets(a: number[], b: number[]): boolean {
  if (a.length !== b.length) {
    return false;
  }
  const sortedA = [...a].sort((x, y) => x - y);
  const sortedB = [...b].sort((x, y) => x - y);
  return sortedA.every((id, index) => id === sortedB[index]);
}
interface Props { token: string; incident: IncidentResponse; canAssign: boolean; usersMap: Map<number, string>;
  usersLoading: boolean; setUsersMap: React.Dispatch<React.SetStateAction<Map<number, string>>>;
  onChanged: (incident: IncidentResponse) => void; onError: (message: string | null) => void; }
export function IncidentAssignments({ token, incident, canAssign, usersMap, usersLoading, setUsersMap,
  onChanged, onError: setError }: Props) {
  const incidentId = incident.id;
  const [assignmentLoading, setAssignmentLoading] = useState(false);
  const [responsiblePickerOpen, setResponsiblePickerOpen] = useState(false);
  const [executorPickerOpen, setExecutorPickerOpen] = useState(false);
  const [responsibleEditing, setResponsibleEditing] = useState(false);
  const [responsibleDraftId, setResponsibleDraftId] = useState<number | null>(null);
  const [responsibleRemoved, setResponsibleRemoved] = useState(false);
  const [executorsEditing, setExecutorsEditing] = useState(false);
  const [executorDraftIds, setExecutorDraftIds] = useState<number[]>([]);
  const [executorRemovedIds, setExecutorRemovedIds] = useState<Set<number>>(() => new Set());
  const responsibleEditRef = useRef<HTMLDivElement>(null);
  const executorEditRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    setResponsibleEditing(false);
    setResponsiblePickerOpen(false);
    setResponsibleDraftId(null);
    setResponsibleRemoved(false);
    setExecutorsEditing(false);
    setExecutorPickerOpen(false);
    setExecutorDraftIds([]);
    setExecutorRemovedIds(new Set());
  }, [incidentId]);
  const canEditAssignment = canAssign && (
    incident?.status === 'READY_FOR_EXECUTION'
    || incident?.status === 'PREPARATION_FOR_EXECUTION'
  );
  const showResponsibleRow = canEditAssignment || incident?.responsibleUserId != null;
  const showExecutorsRow = canEditAssignment || (incident?.executorUserIds ?? []).length > 0;
  const responsibleDraftAssigned = responsibleDraftId != null && !responsibleRemoved;
  const handleSelectResponsibleFromPicker = (user: UserSummary) => {
    setUsersMap((prev) => {
      const next = new Map(prev);
      next.set(user.id, user.login);
      return next;
    });
    setResponsibleDraftId(user.id);
    setResponsibleRemoved(false);
    setResponsiblePickerOpen(false);
  };

  const startResponsibleEdit = () => {
    if (!incident || !canEditAssignment || assignmentLoading) {
      return;
    }
    setResponsibleDraftId(incident.responsibleUserId ?? null);
    setResponsibleRemoved(false);
    setResponsibleEditing(true);
  };

  const commitResponsibleEdit = useCallback(async () => {
    if (!incident || !responsibleEditing) {
      return;
    }

    const finalId = responsibleDraftId != null && !responsibleRemoved ? responsibleDraftId : null;
    const currentId = incident.responsibleUserId ?? null;

    setResponsibleEditing(false);
    setResponsiblePickerOpen(false);

    if (finalId === currentId) {
      return;
    }

    setAssignmentLoading(true);
    setError(null);
    try {
      const updated = await api.setIncidentResponsible(token, incident.id, finalId);
      onChanged(updated);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось обновить ответственного';
      setError(message);
      setResponsibleDraftId(incident.responsibleUserId ?? null);
      setResponsibleRemoved(false);
      setResponsibleEditing(true);
    } finally {
      setAssignmentLoading(false);
    }
  }, [
    incident,
    responsibleEditing,
    responsibleDraftId,
    responsibleRemoved,
    token,
    onChanged,
    setError,
  ]);

  const toggleResponsibleRemoved = () => {
    setResponsibleRemoved((prev) => !prev);
  };

  const handleAddExecutorsFromPicker = (users: UserSummary[]) => {
    if (users.length === 0) {
      return;
    }

    setUsersMap((prev) => {
      const next = new Map(prev);
      users.forEach((user) => next.set(user.id, user.login));
      return next;
    });

    setExecutorDraftIds((prev) => {
      const next = [...prev];
      users.forEach((user) => {
        if (!next.includes(user.id)) {
          next.push(user.id);
        }
      });
      return next;
    });
    setExecutorRemovedIds((prev) => {
      const next = new Set(prev);
      users.forEach((user) => next.delete(user.id));
      return next;
    });
    setExecutorPickerOpen(false);
  };

  const startExecutorEdit = () => {
    if (!incident || !canEditAssignment || assignmentLoading) {
      return;
    }
    setExecutorDraftIds([...(incident.executorUserIds ?? [])]);
    setExecutorRemovedIds(new Set());
    setExecutorsEditing(true);
  };

  const commitExecutorEdit = useCallback(async () => {
    if (!incident || !executorsEditing) {
      return;
    }

    const finalIds = executorDraftIds.filter((id) => !executorRemovedIds.has(id));
    const currentIds = incident.executorUserIds ?? [];

    setExecutorsEditing(false);
    setExecutorPickerOpen(false);

    if (sameUserIdSets(finalIds, currentIds)) {
      return;
    }

    setAssignmentLoading(true);
    setError(null);
    try {
      const updated = await api.setIncidentExecutors(token, incident.id, finalIds);
      onChanged(updated);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось обновить исполнителей';
      setError(message);
      setExecutorDraftIds([...(incident.executorUserIds ?? [])]);
      setExecutorRemovedIds(new Set());
      setExecutorsEditing(true);
    } finally {
      setAssignmentLoading(false);
    }
  }, [
    executorsEditing,
    executorDraftIds,
    executorRemovedIds,
    incident,
    token,
    onChanged,
    setError,
  ]);

  useEffect(() => {
    if (!executorsEditing && !responsibleEditing) {
      return;
    }

    const onMouseDown = (event: MouseEvent) => {
      const target = event.target as Node;
      if (document.querySelector('.drawer-overlay')?.contains(target)) {
        return;
      }
      if (executorsEditing && !executorEditRef.current?.contains(target)) {
        void commitExecutorEdit();
      }
      if (responsibleEditing && !responsibleEditRef.current?.contains(target)) {
        void commitResponsibleEdit();
      }
    };

    document.addEventListener('mousedown', onMouseDown);
    return () => document.removeEventListener('mousedown', onMouseDown);
  }, [
    commitExecutorEdit,
    commitResponsibleEdit,
    executorsEditing,
    responsibleEditing,
  ]);

  const toggleExecutorRemoved = (userId: number) => {
    setExecutorRemovedIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) {
        next.delete(userId);
      } else {
        next.add(userId);
      }
      return next;
    });
  };
  return <>
  {showResponsibleRow && (
    <div className="data-list__row">
      <dt>Ответственный</dt>
      <dd
        className={
          canEditAssignment && !responsibleEditing
            ? 'executors-field executors-field--clickable'
            : 'executors-field'
        }
        onClick={canEditAssignment && !responsibleEditing ? startResponsibleEdit : undefined}
        onKeyDown={
          canEditAssignment && !responsibleEditing
            ? (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault();
                startResponsibleEdit();
              }
            }
            : undefined
        }
        role={canEditAssignment && !responsibleEditing ? 'button' : undefined}
        tabIndex={canEditAssignment && !responsibleEditing ? 0 : undefined}
      >
        {usersLoading && incident.responsibleUserId != null && !responsibleEditing && (
          <Spinner size="sm" label="Загрузка пользователя" />
        )}
        {responsibleEditing ? (
          <div ref={responsibleEditRef} className="executors-edit-box">
            <div className="user-chip-group">
              {responsibleDraftAssigned && responsibleDraftId != null ? (
                <UserChip
                  login={userLoginFromMap(responsibleDraftId, usersMap)}
                  removable
                  removed={responsibleRemoved}
                  onRemove={toggleResponsibleRemoved}
                />
              ) : (
                <span className="text-muted">Назначьте ответственного</span>
              )}
            </div>
            {!responsibleDraftAssigned && (
              <button
                type="button"
                className="executors-add-btn"
                aria-label="Выбрать ответственного"
                disabled={assignmentLoading}
                onClick={(event) => {
                  event.stopPropagation();
                  setResponsiblePickerOpen(true);
                }}
              >
                +
              </button>
            )}
          </div>
        ) : (
          <div className="user-chip-group">
            {incident.responsibleUserId != null ? (
              <UserChip login={userLoginFromMap(incident.responsibleUserId, usersMap)} />
            ) : (
              canEditAssignment && (
                <span className="text-muted">Нажмите, чтобы назначить ответственного</span>
              )
            )}
          </div>
        )}
      </dd>
    </div>
  )}

  {showExecutorsRow && (
    <div className="data-list__row">
      <dt>Исполнители</dt>
    <dd
      className={
        canEditAssignment && !executorsEditing
          ? 'executors-field executors-field--clickable'
          : 'executors-field'
      }
      onClick={canEditAssignment && !executorsEditing ? startExecutorEdit : undefined}
      onKeyDown={
        canEditAssignment && !executorsEditing
          ? (event) => {
            if (event.key === 'Enter' || event.key === ' ') {
              event.preventDefault();
              startExecutorEdit();
            }
          }
          : undefined
      }
      role={canEditAssignment && !executorsEditing ? 'button' : undefined}
      tabIndex={canEditAssignment && !executorsEditing ? 0 : undefined}
    >
      {usersLoading && (incident.executorUserIds ?? []).length > 0 && !executorsEditing && (
        <Spinner size="sm" label="Загрузка исполнителей" />
      )}
      {executorsEditing ? (
        <div ref={executorEditRef} className="executors-edit-box">
          <div className="user-chip-group">
            {executorDraftIds.length === 0 && (
              <span className="text-muted">Добавьте исполнителей</span>
            )}
            {executorDraftIds.map((userId) => (
              <UserChip
                key={userId}
                login={userLoginFromMap(userId, usersMap)}
                removable
                removed={executorRemovedIds.has(userId)}
                onRemove={() => toggleExecutorRemoved(userId)}
              />
            ))}
          </div>
          <button
            type="button"
            className="executors-add-btn"
            aria-label="Добавить исполнителя"
            disabled={assignmentLoading}
            onClick={(event) => {
              event.stopPropagation();
              setExecutorPickerOpen(true);
            }}
          >
            +
          </button>
        </div>
      ) : (
        <div className="user-chip-group">
          {(incident.executorUserIds ?? []).length === 0 ? (
            canEditAssignment ? (
              <span className="text-muted">Нажмите, чтобы назначить исполнителей</span>
            ) : (
              '—'
            )
          ) : (
            (incident.executorUserIds ?? []).map((userId) => (
              <UserChip
                key={userId}
                login={userLoginFromMap(userId, usersMap)}
              />
            ))
          )}
        </div>
      )}
    </dd>
  </div>
  )}
      <UserPickerDrawer
        token={token}
        open={responsiblePickerOpen && canEditAssignment && responsibleEditing}
        title="Назначить ответственного"
        selecting={assignmentLoading}
        onClose={() => setResponsiblePickerOpen(false)}
        onSelect={handleSelectResponsibleFromPicker}
      />

      <UserPickerDrawer
        token={token}
        open={executorPickerOpen && canEditAssignment && executorsEditing}
        title="Добавить исполнителей"
        selecting={assignmentLoading}
        multiple
        excludeUserIds={executorDraftIds.filter((id) => !executorRemovedIds.has(id))}
        onClose={() => setExecutorPickerOpen(false)}
        onSelectMany={handleAddExecutorsFromPicker}
      />
  </>;
}
