export interface PageQuery { page: number; size: number; }

export interface Page<T> extends PageQuery { items: T[]; totalElements: number; totalPages: number; }

export interface HistoryEntry<TSnapshot> { id: number; changedAt: string; changedByUserId: number; snapshot: TSnapshot; }

export interface ErrorObject {
  code?: number;
  message?: string;
  humanMessage?: string;
}
