import { compareHistoryFields } from './historyDiff';

type Value = string | { kind: 'user'; id: number | null };

it('omits unchanged structured references while preserving changed reference values', () => {
  const rows = compareHistoryFields<Value>({ 'Ответственный': { kind: 'user', id: 4 }, 'Автор': { kind: 'user', id: 2 }, 'Статус': 'Принята' },
    { 'Ответственный': { kind: 'user', id: 3 }, 'Автор': { kind: 'user', id: 2 }, 'Статус': 'Новая' });
  expect(rows).toEqual([
    { label: 'Ответственный', oldValue: { kind: 'user', id: 3 }, newValue: { kind: 'user', id: 4 } },
    { label: 'Статус', oldValue: 'Новая', newValue: 'Принята' },
  ]);
});

it('includes every creation value without inventing a previous value', () => {
  expect(compareHistoryFields<Value>({ 'Субъект': 'Альф', 'Рассмотрел': { kind: 'user', id: null } })).toEqual([
    { label: 'Субъект', oldValue: undefined, newValue: 'Альф' },
    { label: 'Рассмотрел', oldValue: undefined, newValue: { kind: 'user', id: null } },
  ]);
});
