import { TenderResponse, TenderStatus } from '../types';

export function tenderTransitions(tender: TenderResponse, permissions: string[], currentUserId?: number): TenderStatus[] {
  const manager = permissions.includes('TENDER_MANAGE');
  const ownNeed = permissions.includes('TENDER_EDIT') && tender.status === 'NEED' && tender.createdByUserId === currentUserId;
  const options: TenderStatus[] = [];
  if (manager && tender.status === 'NEED') { options.push('COLLECTING_OFFERS'); }
  if ((manager || ownNeed) && tender.status !== 'COMPLETED' && tender.status !== 'CANCELLED') { options.push('CANCELLED'); }
  return options;
}
