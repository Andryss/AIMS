import { useCallback, useEffect, useRef, useState } from 'react';
import * as api from '../api/client';
import { IncidentHistoryEntry } from '../types';

/** Incident history is loaded lazily and in full, unlike paged entity histories. */
export function useIncidentHistory(token: string, incidentId: number, onError: (error: string) => void) {
  const [historyEntries, setHistoryEntries] = useState<IncidentHistoryEntry[]>([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [historyLoaded, setHistoryLoaded] = useState(false);
  const historyRequest = useRef(0);
  const historyScope = useRef('');
  historyScope.current = JSON.stringify([incidentId, token]);
  const invalidateHistory = useCallback((clear = false) => {
    historyRequest.current += 1;
    setHistoryLoading(false);
    setHistoryLoaded(false);
    if (clear) { setHistoryEntries([]); }
  }, []);
  useEffect(() => {
    invalidateHistory(true);
    return () => { historyRequest.current += 1; };
  }, [incidentId, token, invalidateHistory]);
  const loadHistory = useCallback(async () => {
    if (!Number.isFinite(incidentId)) {
      return;
    }
    const request = ++historyRequest.current;
    const scope = JSON.stringify([incidentId, token]);
    const isCurrent = () => historyRequest.current === request && historyScope.current === scope;
    setHistoryLoading(true);
    try {
      const response = await api.listIncidentHistory(token, incidentId);
      if (!isCurrent()) { return; }
      const entries = [...response.items];
      for (let page = 1; page < response.totalPages; page += 1) {
        const next = await api.listIncidentHistory(token, incidentId, page, response.size);
        if (!isCurrent()) { return; }
        entries.push(...next.items);
      }
      setHistoryEntries(entries);
      setHistoryLoaded(true);
    } catch (err: unknown) {
      if (isCurrent()) {
        const message = err instanceof Error ? err.message : 'Не удалось загрузить историю';
        onError(message);
      }
    } finally {
      if (isCurrent()) { setHistoryLoading(false); }
    }
  }, [incidentId, token, onError]);
  return { historyEntries, historyLoading, historyLoaded, loadHistory, invalidateHistory };
}
