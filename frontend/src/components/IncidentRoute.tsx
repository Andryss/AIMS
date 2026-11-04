import React from 'react';
import { useAuth } from '../auth/AuthContext';
import { IncidentDetailPage } from './IncidentDetailPage';

/** Adapter keeps session concerns out of the incident's independently testable view. */
export function IncidentRoute() {
  const { token, profile, access } = useAuth();
  return <IncidentDetailPage token={token} roles={profile.roles} currentUserId={profile.userId}
    canChangeStatus={access.canChangeIncidentStatus} canEdit={access.canEditIncident}
    canReadAliens={access.canReadAliens} canLinkAlien={access.canLinkAlien}
    canClassifyAlien={access.canClassifyAlien} canCalculateThreat={access.canCalculateThreat}
    canComment={access.canComment} canAssign={access.canAssign}
    canReadCleanupReport={access.canReadCleanupReport} canCreateCleanupReport={access.canCreateCleanupReport}
    canChangeCleanupStatus={access.canChangeCleanupStatus}
    canReadEquipmentMovements={access.canReadMovements} canCreateEquipmentMovements={access.canCreateMovements}
    canReadDetentions={access.canReadDetentions} canCreateDetentions={access.canCreateDetentions} />;
}
