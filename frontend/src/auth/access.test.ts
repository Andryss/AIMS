import { deriveAccess } from './access';
import { getNavigationItems } from './navigation';
import { AuthMeResponse } from '../types';

const profile = (roles: string[], permissions: string[]): AuthMeResponse => ({ userId: 1, login: 'user', roles, permissions });

it('requires both monitoring permission and the operator role', () => {
  expect(deriveAccess(profile(['ADMIN'], ['MONITORING_ALERT_READ'])).canReadMonitoringAlerts).toBe(false);
  expect(deriveAccess(profile(['OPERATOR'], [])).canReadMonitoringAlerts).toBe(false);
  expect(deriveAccess(profile(['OPERATOR'], ['MONITORING_ALERT_READ'])).canReadMonitoringAlerts).toBe(true);
});

it('keeps direct alien access separate from the knowledge base list', () => {
  const access = deriveAccess(profile(['AGENT'], ['ALIEN_READ']));
  expect(access.canReadAliens).toBe(true);
  expect(access.showKnowledgeBaseTab).toBe(false);
  expect(getNavigationItems(access).map((item) => item.to)).not.toContain('/aliens');
});

it('requires warehouse role for processing movements even with permission', () => {
  expect(deriveAccess(profile(['AGENT'], ['EQUIPMENT_MOVEMENT_PROCESS'])).canProcessMovements).toBe(false);
  expect(deriveAccess(profile(['STOREKEEPER'], ['EQUIPMENT_MOVEMENT_PROCESS'])).canProcessMovements).toBe(true);
});

it.each([
  ['MANAGEMENT', 'ANALYTICS_READ', '/analytics'], ['PRISON_GUARD', 'DETENTION_READ', '/detention-requests'],
  ['TENDER_MANAGER', 'TENDER_READ', '/tenders'], ['STOREKEEPER', 'EQUIPMENT_READ', '/equipment'],
] as const)('uses the permitted default screen for %s', (role, permission, path) => {
  expect(deriveAccess(profile([role], [permission])).defaultPath).toBe(path);
  expect(deriveAccess(profile([role], [])).defaultPath).toBe('/incidents');
});

it('does not expose any navigation for an absent session', () => {
  const access = deriveAccess(null);
  expect(access.defaultPath).toBe('/profile');
  expect(getNavigationItems(access)).toEqual([]);
});
