import { DetentionRequestResponse, DetentionRequestStatus } from './types';

export const DETENTION_PAGE_SIZE = 10;
export const DETENTION_DEFAULT_DURATION = 30;

export const DETENTION_STATUS_LABELS: Record<DetentionRequestStatus, string> = {
  NEW: 'Новая',
  ACCEPTED: 'Принята',
  IN_CUSTODY: 'Под стражей',
  RELEASED: 'Освобождён',
  REJECTED: 'Отклонена',
  CANCELLED: 'Отменена',
};

export function canEditDetentionRequest(request: DetentionRequestResponse, permissions: string[], userId?: number): boolean {
  return permissions.includes('DETENTION_EDIT') && ['NEW', 'ACCEPTED', 'IN_CUSTODY'].includes(request.status)
    && (permissions.includes('DETENTION_PROCESS') || (request.status === 'NEW' && request.createdByUserId === userId));
}

export function isDetentionOverdue(request: DetentionRequestResponse): boolean {
  return request.status === 'IN_CUSTODY' && request.plannedReleaseAt != null
    && Date.parse(request.plannedReleaseAt) <= Date.now();
}

export { detentionTransitions } from './domain/detentionTransitions';
