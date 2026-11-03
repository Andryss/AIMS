import { TenderParticipantStatus, TenderResponse, TenderStatus } from './types';

export const TENDER_STATUS_LABELS: Record<TenderStatus, string> = {
  NEED: 'Потребность',
  COLLECTING_OFFERS: 'Сбор предложений',
  AWAITING_DELIVERY: 'Ожидание поставки',
  COMPLETED: 'Завершён',
  CANCELLED: 'Отменён',
};

export const TENDER_PARTICIPANT_STATUS_LABELS: Record<TenderParticipantStatus, string> = {
  PARTICIPATING: 'Участвует',
  WINNER: 'Победитель',
};

export function canEditTender(tender: TenderResponse, permissions: string[], currentUserId?: number): boolean {
  return permissions.includes('TENDER_EDIT') && (tender.status === 'NEED' || tender.status === 'COLLECTING_OFFERS')
    && (permissions.includes('TENDER_MANAGE') || (tender.status === 'NEED' && tender.createdByUserId === currentUserId));
}
