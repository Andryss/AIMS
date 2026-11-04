import { useEffect, useState } from 'react';
import * as api from '../api/client';
import { Alien, CleanupReportResponse, IncidentResponse } from '../types';

export function useIncidentDetails(token: string, incidentId: number, canReadAliens: boolean, canReadCleanupReport: boolean) {
  const [incident, setIncident] = useState<IncidentResponse | null>(null);
  const [linkedAlien, setLinkedAlien] = useState<Alien | null>(null);
  const [cleanupReport, setCleanupReport] = useState<CleanupReportResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  useEffect(() => {
    if (!Number.isFinite(incidentId)) {
      setError('Некорректный идентификатор инцидента');
      setIncident(null);
      setLinkedAlien(null);
      setCleanupReport(null);
      setLoading(false);
      return;
    }

    let cancelled = false;
    setLoading(true);
    setError(null);

    (async () => {
      try {
        const data = await api.getIncident(token, incidentId);
        if (cancelled) {
          return;
        }
        setIncident(data);
        if (canReadAliens && data.alienId != null) {
          const alien = await api.getAlien(token, data.alienId);
          if (!cancelled) {
            setLinkedAlien(alien);
          }
        } else {
          setLinkedAlien(null);
        }
        if (canReadCleanupReport && data.cleanupReportId != null) {
          const report = await api.getCleanupReport(token, incidentId);
          if (!cancelled) {
            setCleanupReport(report);
          }
        } else {
          setCleanupReport(null);
        }
      } catch (err: unknown) {
        if (cancelled) {
          return;
        }
        const message = err instanceof Error ? err.message : 'Не удалось загрузить инцидент';
        setError(message);
        setIncident(null);
        setLinkedAlien(null);
        setCleanupReport(null);
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [canReadAliens, canReadCleanupReport, incidentId, token]);
  return { incident, setIncident, linkedAlien, setLinkedAlien, cleanupReport, setCleanupReport, loading, error, setError };
}
