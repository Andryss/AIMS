import { STATUS_TRANSITIONS, CLEANUP_STATUS_TRANSITIONS } from '../incidentLabels';
import { IncidentResponse, IncidentStatus, CleanupStatus } from '../types';

function hasRole(roles: string[], name: string): boolean {
  return roles.includes(name) || roles.includes('ADMIN');
}

function isResponsibleAgent(incident: IncidentResponse, currentUserId?: number): boolean {
  return incident.responsibleUserId != null
    && currentUserId != null
    && incident.responsibleUserId === currentUserId;
}

export function incidentTransitions(
  status: IncidentStatus,
  roles: string[],
  incident: IncidentResponse,
  currentUserId?: number,
): IncidentStatus[] {
  const fromGraph = STATUS_TRANSITIONS[status];
  if (status === 'DRAFT') {
    return hasRole(roles, 'OPERATOR') ? fromGraph : [];
  }
  if (status === 'READY_FOR_ANALYSIS') {
    return hasRole(roles, 'ANALYST') ? fromGraph : [];
  }
  if (status === 'CLARIFICATION_REQUIRED') {
    return hasRole(roles, 'OPERATOR') ? fromGraph : [];
  }
  if (status === 'REANALYSIS_REQUIRED') {
    return hasRole(roles, 'ANALYST') ? fromGraph : [];
  }
  if (status === 'PREPARED_FOR_EXECUTION' || status === 'EXECUTING') {
    if (!hasRole(roles, 'AGENT')) {
      return [];
    }
    return isResponsibleAgent(incident, currentUserId) ? fromGraph : [];
  }
  if (status === 'READY_FOR_EXECUTION' || status === 'PREPARATION_FOR_EXECUTION') {
    return hasRole(roles, 'AGENT') ? fromGraph : [];
  }
  return [];
}

export function cleanupTransitions(current: CleanupStatus | null | undefined): CleanupStatus[] {
  return CLEANUP_STATUS_TRANSITIONS[current ?? 'null'];
}
