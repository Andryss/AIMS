import { AnalyticsPeriodQuery } from '../types';

export const formatAnalyticsNumber = (value: number): string =>
  new Intl.NumberFormat('ru-RU', { maximumFractionDigits: 3 }).format(value);

export const formatAnalyticsDate = (value: string): string => new Date(value).toLocaleString('ru-RU');

export function formatAnalyticsDuration(seconds: number | null | undefined): string {
  if (seconds == null) { return '—'; }
  if (seconds === 0) { return '0 с'; }
  if (seconds < 0.001) { return `${formatAnalyticsNumber(seconds * 1_000_000)} мкс`; }
  if (seconds < 1) { return `${formatAnalyticsNumber(seconds * 1000)} мс`; }
  const parts: string[] = [];
  let remainder = seconds;
  [[86400, 'сут'], [3600, 'ч'], [60, 'мин']].forEach(([unit, label]) => {
    const count = Math.floor(remainder / Number(unit));
    if (count) { parts.push(`${formatAnalyticsNumber(count)} ${label}`); remainder %= Number(unit); }
  });
  if (remainder || !parts.length) { parts.push(`${formatAnalyticsNumber(remainder)} с`); }
  return parts.join(' ');
}

export function defaultAnalyticsPeriod(now = Date.now()): AnalyticsPeriodQuery {
  const to = Math.floor(now / 60000) * 60000;
  return { from: new Date(to - 7 * 86400000).toISOString(), to: new Date(to).toISOString() };
}

export function normalizeLocalDateTimeInput(value: string): string {
  return value.replace(/(\.\d*?)0+$/, '$1').replace(/\.$/, '').replace(/(T\d{2}:\d{2}):00$/, '$1');
}

export function toLocalDateTimeInput(value: string): string {
  const date = new Date(value);
  if (!Number.isFinite(date.getTime())) { return ''; }
  return normalizeLocalDateTimeInput(new Date(date.getTime() - date.getTimezoneOffset() * 60000).toISOString().slice(0, -1));
}

/** URL и API требуют явное смещение и точность не выше микросекунд. */
export function isAnalyticsTimestamp(value: string): boolean {
  const match = /^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.\d{1,6})?(Z|[+-]\d{2}:\d{2})$/.exec(value);
  if (!match || !Number.isFinite(Date.parse(value))) { return false; }
  const date = new Date(match[1] + 'Z');
  if (!Number.isFinite(date.getTime()) || date.toISOString().slice(0, 19) !== match[1]) { return false; }
  if (match[2] !== 'Z') {
    const hours = Number(match[2].slice(1, 3));
    const minutes = Number(match[2].slice(4));
    if (hours > 18 || minutes > 59 || (hours === 18 && minutes !== 0)) { return false; }
  }
  return true;
}

function microsecondRemainder(value: string): number {
  return Number((/\.(\d+)/.exec(value)?.[1] ?? '').padEnd(6, '0').slice(3));
}

export function validateAnalyticsQuery(from: string, to: string): string | null {
  if (!isAnalyticsTimestamp(from) || !isAnalyticsTimestamp(to)) {
    return 'Укажите даты с часовым поясом и точностью не выше микросекунд';
  }
  const start = Date.parse(from);
  const end = Date.parse(to);
  if (start > end || (start === end && microsecondRemainder(from) >= microsecondRemainder(to))) {
    return 'Начало периода должно быть раньше окончания';
  }
  const now = Date.now();
  if (end > now || (end === now && microsecondRemainder(to) > 0)) {
    return 'Окончание периода не может быть в будущем';
  }
  return null;
}
