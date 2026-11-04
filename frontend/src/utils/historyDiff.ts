export interface HistoryDiffRow<Value> {
  label: string;
  oldValue?: Value;
  newValue: Value;
}

/** Field definitions and any normalization belong to the domain's snapshot formatter. */
export function compareHistoryFields<Value>(current: Record<string, Value>, previous?: Record<string, Value>): HistoryDiffRow<Value>[] {
  return Object.entries(current)
    .filter(([label, value]) => !previous || JSON.stringify(previous[label]) !== JSON.stringify(value))
    .map(([label, value]) => ({ label, oldValue: previous?.[label], newValue: value }));
}
