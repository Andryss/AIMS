import { IncidentEventType, IncidentStatus } from './types';

export const EVENT_TYPE_LABELS: Record<IncidentEventType, string> = {
  UNIDENTIFIED_SIGHTING: 'Неопознанное наблюдение',
  CONTACT_SUSPECT: 'Подозрение на контакт',
  ILLEGAL_UFO_LANDING: 'Незаконная посадка НЛО',
  MEMORY_ANOMALY: 'Аномалия памяти',
  ALIEN_ARTIFACT: 'Инопланетный артефакт',
  ALIEN_CAPTURE: 'Захват инопланетянина',
};

export const STATUS_LABELS: Record<IncidentStatus, string> = {
  DRAFT: 'Черновик',
  READY_FOR_ANALYSIS: 'Готов к анализу',
};

/** Допустимые целевые статусы из текущего (совпадает с графом на бэкенде). */
export const STATUS_TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  DRAFT: ['READY_FOR_ANALYSIS'],
  READY_FOR_ANALYSIS: [],
};
