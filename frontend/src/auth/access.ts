import { INCIDENTS_TAB_ROLES, KNOWLEDGE_BASE_TAB_ROLES, MONITORING_ALERTS_TAB_ROLES } from '../constants';
import { AuthMeResponse } from '../types';

/** UI access rules. Entity-specific ownership and status rules live in each domain. */
export function deriveAccess(profile: AuthMeResponse | null) {
  const roles = profile?.roles ?? [];
  const hasPermission = (permission: string) => profile?.permissions.includes(permission) ?? false;
  const hasAnyRole = (allowed: readonly string[]) => roles.some((role) => allowed.includes(role));
  const showIncidentsTab = hasAnyRole(INCIDENTS_TAB_ROLES);
  const canReadAnalytics = hasPermission('ANALYTICS_READ');
  const canReadDetentions = hasPermission('DETENTION_READ');
  const canReadTenders = hasPermission('TENDER_READ');
  const canReadEquipment = hasPermission('EQUIPMENT_READ');
  const canReadAliens = hasPermission('ALIEN_READ');
  const defaultPath = roles.includes('MANAGEMENT') && canReadAnalytics ? '/analytics'
    : roles.includes('PRISON_GUARD') && canReadDetentions ? '/detention-requests'
    : roles.includes('TENDER_MANAGER') && canReadTenders ? '/tenders'
    : roles.includes('STOREKEEPER') && canReadEquipment ? '/equipment'
    : showIncidentsTab ? '/incidents' : '/profile';

  return {
    showIncidentsTab, canReadAnalytics, canReadDetentions, canReadTenders, canReadEquipment, canReadAliens, defaultPath,
    canReadIncidents: showIncidentsTab && hasPermission('INCIDENT_READ'),
    showKnowledgeBaseTab: canReadAliens && hasAnyRole(KNOWLEDGE_BASE_TAB_ROLES),
    canReadMonitoringAlerts: hasPermission('MONITORING_ALERT_READ') && hasAnyRole(MONITORING_ALERTS_TAB_ROLES),
    canCreateDetentions: hasPermission('DETENTION_CREATE'),
    canCreateTenders: hasPermission('TENDER_CREATE'),
    canWriteEquipment: hasPermission('EQUIPMENT_WRITE'),
    canReadMovements: hasPermission('EQUIPMENT_MOVEMENT_READ'),
    canCreateMovements: hasPermission('EQUIPMENT_MOVEMENT_CREATE'),
    canProcessMovements: hasPermission('EQUIPMENT_MOVEMENT_PROCESS') && hasAnyRole(['STOREKEEPER', 'ADMIN']),
    canCreateIncident: hasPermission('INCIDENT_CREATE'),
    canEditIncident: hasPermission('INCIDENT_EDIT'),
    canChangeIncidentStatus: hasPermission('INCIDENT_STATUS_CHANGE'),
    canWriteAliens: hasPermission('ALIEN_WRITE'),
    canClassifyAlien: hasPermission('INCIDENT_ALIEN_CLASSIFY'),
    canCalculateThreat: hasPermission('INCIDENT_THREAT_CALCULATE'),
    canLinkAlien: hasPermission('INCIDENT_ALIEN_LINK'),
    canComment: hasPermission('INCIDENT_COMMENT'),
    canAssign: hasPermission('INCIDENT_ASSIGN'),
    canReadCleanupReport: hasPermission('CLEANUP_REPORT_READ'),
    canCreateCleanupReport: hasPermission('CLEANUP_REPORT_CREATE'),
    canChangeCleanupStatus: hasPermission('CLEANUP_STATUS_CHANGE'),
  };
}

export type Access = ReturnType<typeof deriveAccess>;
