import { defaultAnalyticsPeriod, formatAnalyticsDuration, isAnalyticsTimestamp, validateAnalyticsQuery } from './analyticsFormatting';

it.each([
  [null, '—'], [0, '0 с'], [0.0001, '100 мкс'], [0.1, '100 мс'], [49.6, '49,6 с'],
  [60, '1 мин'], [3600, '1 ч'], [86400, '1 сут'], [90061, '1 сут 1 ч 1 мин 1 с'],
])('formats duration %s without confusing null with zero', (value, expected) => {
  expect(formatAnalyticsDuration(value)).toBe(expected);
});

it('defaults to seven days with a minute-aligned end', () => {
  expect(defaultAnalyticsPeriod(Date.parse('2025-06-08T12:34:56.789Z'))).toEqual({
    from: '2025-06-01T12:34:00.000Z', to: '2025-06-08T12:34:00.000Z',
  });
});

it.each(['2025-02-30T00:00:00Z', '2025-01-01T24:00:00Z', '2025-01-01T00:00:00+19:00',
  '2025-01-01T00:00:00+18:01', '2025-01-01T00:00:00.000000001Z', '2025-01-01T00:00:00'])('rejects unsupported timestamp %s', (value) => {
  expect(isAnalyticsTimestamp(value)).toBe(false);
  expect(validateAnalyticsQuery('2024-01-01T00:00:00Z', value)).not.toBeNull();
});

it.each(['2025-01-01T00:00:00Z', '2025-01-01T03:00:00+03:00', '2025-01-01T00:00:00.123456Z'])('accepts ISO time %s', (value) => {
  expect(isAnalyticsTimestamp(value)).toBe(true);
});

it('orders valid bounds at microsecond precision', () => {
  expect(validateAnalyticsQuery('2025-05-01T00:00:00.000001Z', '2025-05-01T00:00:00.000002Z')).toBeNull();
  expect(validateAnalyticsQuery('2025-05-01T00:00:00.000002Z', '2025-05-01T00:00:00.000001Z')).not.toBeNull();
});
