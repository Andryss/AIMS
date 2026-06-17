function startOfLocalDay(date: Date): number {
  return new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
}

export function formatNotificationTime(iso: string, now: Date = new Date()): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return iso;
  }

  const timePart = date.toLocaleString('ru-RU', {
    hour: '2-digit',
    minute: '2-digit',
  });

  const todayStart = startOfLocalDay(now);
  const yesterday = new Date(now);
  yesterday.setDate(yesterday.getDate() - 1);
  const yesterdayStart = startOfLocalDay(yesterday);
  const createdStart = startOfLocalDay(date);

  if (createdStart === todayStart) {
    return `сегодня, ${timePart}`;
  }
  if (createdStart === yesterdayStart) {
    return `вчера, ${timePart}`;
  }

  return date.toLocaleString('ru-RU', {
    day: '2-digit',
    month: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  });
}
