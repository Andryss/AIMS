export const SYSTEM_NAME = 'Alien Incident Management System';

export const MIB_LOGO_PATH = `${process.env.PUBLIC_URL || ''}/mib_logo.png`;

export const TOKEN_STORAGE_KEY = 'aims.accessToken';

export const INCIDENTS_TAB_ROLES = ['OPERATOR', 'ANALYST', 'ADMIN', 'AGENT', 'CLEANER'] as const;

export const ROLE_LABELS: Record<string, string> = {
  OPERATOR: 'Оператор',
  ANALYST: 'Аналитик',
  ADMIN: 'Администратор',
  AGENT: 'Оперативный агент',
  CLEANER: 'Специалист по прикрытию',
};
