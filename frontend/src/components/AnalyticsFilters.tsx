import { positiveId } from '../utils/params';
import { formatUserLabel } from '../utils/users';
import React, { useEffect, useState } from 'react';
import * as api from '../api/client';
import { AnalyticsDashboard } from '../analyticsLabels';
import { DETENTION_STATUS_LABELS } from '../detentionLabels';
import { EQUIPMENT_TYPE_LABELS } from '../equipmentLabels';
import { EVENT_TYPE_LABELS } from '../incidentLabels';
import { useUsersMap } from '../hooks/useUsersMap';

import { AlienPickerDrawer } from './AlienPickerDrawer';
import { EmployeeSearch } from './EmployeeSearch';
import { Button } from './ui/Button';
import { FormField } from './ui/FormField';

interface AnalyticsFiltersProps {
  token: string; dashboard: AnalyticsDashboard; params: URLSearchParams; permissions: string[];
  onChange: (field: string, value: string) => void;
}

export function AnalyticsFilters({ token, dashboard, params, permissions, onChange }: AnalyticsFiltersProps) {
  const [pickerOpen, setPickerOpen] = useState(false);
  const [alien, setAlien] = useState<{ id: number; name: string } | null>(null);
  const value = (key: string) => params.get(`${dashboard}.${key}`) ?? '';
  const alienId = positiveId(value('alienId'));
  const ownerId = positiveId(value('responsibleUserId'));
  const readUsers = permissions.includes('USER_READ');
  const readAliens = permissions.includes('ALIEN_READ');
  const users = useUsersMap(token, readUsers && dashboard !== 'detentions' ? [ownerId] : []);

  useEffect(() => {
    let cancelled = false;
    setAlien(null);
    if (readAliens && alienId && dashboard !== 'equipment') {
      api.getAlien(token, alienId).then((result) => { if (!cancelled) { setAlien(result); } }).catch(() => {});
    }
    return () => { cancelled = true; };
  }, [token, alienId, readAliens, dashboard]);

  const select = (label: string, key: string, labels: Record<string, string>, all: string) =>
    <FormField label={label}><select value={Object.prototype.hasOwnProperty.call(labels, value(key)) ? value(key) : ''} onChange={(event) => onChange(key, event.target.value)}>
      <option value="">{all}</option>{Object.entries(labels).map(([key, label]) => <option key={key} value={key}>{label}</option>)}
    </select></FormField>;

  return <>
    <div className="analytics-filters">
      {dashboard === 'incidents' && select('Тип события', 'eventType', EVENT_TYPE_LABELS, 'Все типы событий')}
      {dashboard === 'equipment' && select('Тип оборудования', 'equipmentType', EQUIPMENT_TYPE_LABELS, 'Все типы')}
      {dashboard === 'detentions' && select('Статус', 'status', DETENTION_STATUS_LABELS, 'Все статусы')}
      {dashboard !== 'equipment' && (readAliens ? <div className="field">
        <span className="field__label">Вид</span><div className="analytics-filter-actions">
          <Button variant="outline" onClick={() => setPickerOpen(true)}>{alienId
            ? alien?.id === alienId ? alien.name : `Вид #${alienId}` : 'Все виды'}</Button>
          {alienId && <Button variant="ghost" size="sm" aria-label="Очистить вид" onClick={() => onChange('alienId', '')}>Очистить</Button>}
        </div>
      </div> : <FormField label="ID вида"><input type="number" min="1" step="1" value={value('alienId')}
        onChange={(event) => onChange('alienId', event.target.value)} /></FormField>)}
      {dashboard !== 'detentions' && (readUsers ? <EmployeeSearch token={token} label="Ответственный"
        value={ownerId ? { id: ownerId, login: formatUserLabel(ownerId, users) } : null}
        onChange={(user) => onChange('responsibleUserId', user ? String(user.id) : '')} />
        : <FormField label="ID ответственного"><input type="number" min="1" step="1" value={value('responsibleUserId')}
          onChange={(event) => onChange('responsibleUserId', event.target.value)} /></FormField>)}
      {dashboard !== 'incidents' && <FormField label="Инцидент" help="Номер инцидента">
        <input type="number" min="1" step="1" value={value('incidentId')} placeholder="Все инциденты"
          onChange={(event) => onChange('incidentId', event.target.value)} /></FormField>}
    </div>
    {dashboard === 'equipment' && <p className="analytics-note">Ответственный и инцидент ограничивают движения, открытые заявки и активные выдачи. Остатки и ожидаемые поставки зависят только от типа оборудования.</p>}
    {readAliens && <AlienPickerDrawer token={token} open={pickerOpen} selecting={false}
      onClose={() => setPickerOpen(false)} onSelect={(id) => { onChange('alienId', String(id)); setPickerOpen(false); }} />}
  </>;
}
