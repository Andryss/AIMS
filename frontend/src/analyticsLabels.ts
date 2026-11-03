export const ANALYTICS_DASHBOARDS = {
  incidents: 'Инциденты',
  equipment: 'Склад',
  detentions: 'Заключение',
} as const;

export type AnalyticsDashboard = keyof typeof ANALYTICS_DASHBOARDS;

export const ASSIGNED_TERM_LABELS: Record<string, string> = {
  UP_TO_30: 'До 30 суток', FROM_31_TO_90: '31–90 суток', OVER_90: 'Более 90 суток',
};

export const REMAINING_TIME_LABELS: Record<string, string> = {
  OVERDUE: 'Срок истёк', UP_TO_7: 'До 7 суток', FROM_7_TO_30: 'Свыше 7 до 30 суток', OVER_30: 'Более 30 суток',
};
