import { CleanupStatus, IncidentEventType, IncidentStatus } from './types';

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
  READY_FOR_EXECUTION: 'Готов к выполнению',
  PREPARATION_FOR_EXECUTION: 'Подготовка к выполнению',
  PREPARED_FOR_EXECUTION: 'Подготовлен к выполнению',
  EXECUTING: 'Выполняется',
  EXECUTION_COMPLETED: 'Выполнение завершено',
  CLARIFICATION_REQUIRED: 'Требуется уточнение',
  REANALYSIS_REQUIRED: 'Требуется повторный анализ',
};

export const CLEANUP_STATUS_LABELS: Record<CleanupStatus, string> = {
  PREPARATION: 'Подготовка',
  EXECUTION: 'Выполнение',
  COMPLETED: 'Завершена',
};

/** Базовый граф переходов (роль уточняется в IncidentStatusSelect). */
export const STATUS_TRANSITIONS: Record<IncidentStatus, IncidentStatus[]> = {
  DRAFT: ['READY_FOR_ANALYSIS'],
  READY_FOR_ANALYSIS: ['READY_FOR_EXECUTION', 'CLARIFICATION_REQUIRED'],
  READY_FOR_EXECUTION: [
    'PREPARATION_FOR_EXECUTION',
    'CLARIFICATION_REQUIRED',
    'REANALYSIS_REQUIRED',
  ],
  PREPARATION_FOR_EXECUTION: [
    'PREPARED_FOR_EXECUTION',
    'CLARIFICATION_REQUIRED',
    'REANALYSIS_REQUIRED',
  ],
  PREPARED_FOR_EXECUTION: ['EXECUTING'],
  EXECUTING: ['EXECUTION_COMPLETED'],
  EXECUTION_COMPLETED: [],
  CLARIFICATION_REQUIRED: ['READY_FOR_ANALYSIS'],
  REANALYSIS_REQUIRED: ['READY_FOR_ANALYSIS'],
};

/** Линейный граф статуса очистки (null трактуется как первый переход в CleanupStatusSelect). */
export const CLEANUP_STATUS_TRANSITIONS: Record<CleanupStatus | 'null', CleanupStatus[]> = {
  null: ['PREPARATION'],
  PREPARATION: ['EXECUTION'],
  EXECUTION: ['COMPLETED'],
  COMPLETED: [],
};
