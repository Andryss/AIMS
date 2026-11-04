import { DetentionRequestResponse, DetentionRequestStatus } from '../types';

export function detentionTransitions(request: DetentionRequestResponse, permissions: string[], userId?: number): DetentionRequestStatus[] {
  if (permissions.includes('DETENTION_PROCESS')) {
    if (request.status === 'NEW') { return ['ACCEPTED', 'REJECTED', 'CANCELLED']; }
    if (request.status === 'ACCEPTED') { return ['IN_CUSTODY', 'CANCELLED']; }
    if (request.status === 'IN_CUSTODY') { return ['RELEASED']; }
  }
  return request.status === 'NEW' && permissions.includes('DETENTION_EDIT') && request.createdByUserId === userId
    ? ['CANCELLED'] : [];
}
