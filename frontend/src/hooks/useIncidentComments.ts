import { FormEvent, useCallback, useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { IncidentComment, IncidentResponse } from '../types';

export function useIncidentComments(token: string, incidentId: number, incident: IncidentResponse | null,
  onError: (message: string | null) => void) {
  const [comments, setComments] = useState<IncidentComment[]>([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [commentText, setCommentText] = useState('');
  const [commentSubmitting, setCommentSubmitting] = useState(false);
  const scope = JSON.stringify([token, incidentId]);
  const currentScope = useRef(scope);
  currentScope.current = scope;
  useEffect(() => {
    currentScope.current = scope;
    setComments([]); setCommentText(''); setCommentsLoading(false); setCommentSubmitting(false);
    return () => { currentScope.current = ''; };
  }, [scope]);
  const loadComments = useCallback(async () => {
    if (!Number.isFinite(incidentId)) {
      return;
    }
    const requestScope = scope;
    setCommentsLoading(true);
    try {
      const response = await api.listIncidentComments(token, incidentId);
      if (currentScope.current === requestScope) { setComments(response.items); }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось загрузить комментарии';
      if (currentScope.current === requestScope) { onError(message); }
    } finally {
      if (currentScope.current === requestScope) { setCommentsLoading(false); }
    }
  }, [incidentId, token, scope, onError]);
  const handleAddComment = async (event: FormEvent) => {
    event.preventDefault();
    if (!incident || !commentText.trim() || commentSubmitting) {
      return;
    }
    const requestScope = scope;
    setCommentSubmitting(true);
    onError(null);
    try {
      const created = await api.createIncidentComment(token, incident.id, commentText.trim());
      if (currentScope.current !== requestScope) { return; }
      setComments((prev) => [...prev, created]);
      setCommentText('');
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Не удалось добавить комментарий';
      if (currentScope.current === requestScope) { onError(message); }
    } finally {
      if (currentScope.current === requestScope) { setCommentSubmitting(false); }
    }
  };
  return { comments, commentsLoading, commentText, setCommentText, commentSubmitting, loadComments, handleAddComment };
}
