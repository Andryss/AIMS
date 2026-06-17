import { formatNotificationTime } from './formatNotificationTime';

describe('formatNotificationTime', () => {
  const now = new Date(2025, 5, 17, 14, 30);

  it('formats today with "сегодня" label', () => {
    expect(formatNotificationTime('2025-06-17T09:15:00', now)).toMatch(/^сегодня, /);
  });

  it('formats yesterday with "вчера" label', () => {
    expect(formatNotificationTime('2025-06-16T20:00:00', now)).toMatch(/^вчера, /);
  });

  it('formats older dates with day and month', () => {
    const formatted = formatNotificationTime('2025-06-01T12:00:00', now);
    expect(formatted).toMatch(/01\.06/);
    expect(formatted).not.toMatch(/сегодня|вчера/);
  });

  it('returns original string for invalid date', () => {
    expect(formatNotificationTime('not-a-date', now)).toBe('not-a-date');
  });
});
