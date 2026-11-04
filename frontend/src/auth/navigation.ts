import { Access } from './access';

export interface NavigationItem { to: string; label: string; }

const NAVIGATION: Array<NavigationItem & { access: keyof Access }> = [
  { to: '/incidents', label: 'Инциденты', access: 'showIncidentsTab' },
  { to: '/monitoring/alerts', label: 'Входящие алерты', access: 'canReadMonitoringAlerts' },
  { to: '/equipment', label: 'Склад', access: 'canReadEquipment' },
  { to: '/equipment-movements', label: 'Движение оборудования', access: 'canReadMovements' },
  { to: '/tenders', label: 'Тендеры', access: 'canReadTenders' },
  { to: '/detention-requests', label: 'Заключение', access: 'canReadDetentions' },
  { to: '/analytics', label: 'Аналитика', access: 'canReadAnalytics' },
  { to: '/aliens', label: 'База знаний', access: 'showKnowledgeBaseTab' },
];

export function getNavigationItems(access: Access): NavigationItem[] {
  return NAVIGATION.filter((item) => access[item.access] === true).map(({ to, label }) => ({ to, label }));
}
