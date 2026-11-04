import { EquipmentMovementResponse, EquipmentMovementStatus } from '../types';

export function equipmentMovementTransitions(movement: EquipmentMovementResponse, canCreate: boolean,
  canProcess: boolean, currentUserId?: number, disabled = false): EquipmentMovementStatus[] {
  const options: EquipmentMovementStatus[] = [];
  const active = movement.status === 'PENDING' || movement.status === 'PROCESSING';
  if (active && !disabled) {
    if (canProcess) { options.push(movement.status === 'PENDING' ? 'PROCESSING' : 'COMPLETED'); }
    if (canProcess || (canCreate && movement.createdByUserId === currentUserId)) { options.push('CANCELLED'); }
    if (canProcess) { options.push('REJECTED'); }
  }
  return options;
}
