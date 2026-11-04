/** Shared display formatting; editing/transport helpers preserve their own precision. */
export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('ru-RU');
}
