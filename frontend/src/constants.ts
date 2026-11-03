export const SYSTEM_NAME = 'Alien Incident Management System';

export const MIB_LOGO_PATH = `${process.env.PUBLIC_URL || ''}/mib_logo.png`;

export const TOKEN_STORAGE_KEY = 'aims.accessToken';

export const INCIDENTS_TAB_ROLES = ['OPERATOR', 'ANALYST', 'ADMIN', 'AGENT', 'STOREKEEPER', 'TENDER_MANAGER', 'CLEANER'] as const;
export const KNOWLEDGE_BASE_TAB_ROLES = ['ANALYST', 'ADMIN'] as const;

/** Оператор мониторинга — единственная роль с доступом к входящим алертам. */
export const MONITORING_ALERTS_TAB_ROLES = ['OPERATOR'] as const;

export const USER_BATCH_SIZE = 100;

export const ROLE_LABELS: Record<string, string> = {
  OPERATOR: 'Оператор',
  ANALYST: 'Аналитик',
  ADMIN: 'Администратор',
  AGENT: 'Оперативный агент',
  STOREKEEPER: 'Завхоз',
  TENDER_MANAGER: 'Менеджер закупок',
  CLEANER: 'Специалист по прикрытию',
};
