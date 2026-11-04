import { parseEnumValue, parsePage, positiveId } from './params';

it.each([[null, 0], ['-1', 0], ['1.5', 0], ['NaN', 0], ['9007199254740992', 0], ['2', 2]] as const)(
  'normalizes invalid page %s without losing valid pages', (input, expected) => {
    expect(parsePage(input)).toBe(expected);
  },
);

it.each([[null, undefined], ['', undefined], ['0', undefined], ['-1', undefined], ['1.5', undefined],
  ['9007199254740992', undefined], ['12', 12]] as const)('parses a positive identifier %s', (input, expected) => {
  expect(positiveId(input)).toBe(expected);
});

it('rejects inherited keys and unknown enum values from URLs', () => {
  const labels = { NEW: 'Новая', CLOSED: 'Закрытая' };
  expect(parseEnumValue(labels, 'NEW')).toBe('NEW');
  expect(parseEnumValue(labels, 'toString')).toBeUndefined();
  expect(parseEnumValue(labels, 'OTHER')).toBeUndefined();
});
