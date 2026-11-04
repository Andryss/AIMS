/** URL inputs are untrusted: accept safe integers only. */
export function positiveId(value: string | null | undefined): number | undefined {
  const id = Number(value);
  return Number.isSafeInteger(id) && id > 0 ? id : undefined;
}

export function parsePage(value: string | null | undefined): number {
  const page = Number(value ?? 0);
  return Number.isSafeInteger(page) && page >= 0 ? page : 0;
}

export function parseEnumValue<T extends string>(labels: Record<T, string>, value: string | null | undefined): T | undefined {
  return value != null && Object.prototype.hasOwnProperty.call(labels, value) ? value as T : undefined;
}
