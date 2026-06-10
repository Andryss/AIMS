import { buildIdenticon } from './identicon';

describe('buildIdenticon', () => {
  it('returns stable identicon for same login', () => {
    const first = buildIdenticon('Operator');
    const second = buildIdenticon('operator');
    expect(first).toEqual(second);
  });

  it('returns different colors for different logins', () => {
    const a = buildIdenticon('alice');
    const b = buildIdenticon('bob');
    expect(a.background).not.toBe(b.background);
  });

  it('uses 5x5 grid with mirrored cells', () => {
    const data = buildIdenticon('test-user');
    expect(data.gridSize).toBe(5);
    data.cells.forEach((cell) => {
      expect(cell.x).toBeGreaterThanOrEqual(0);
      expect(cell.x).toBeLessThan(5);
      expect(cell.y).toBeGreaterThanOrEqual(0);
      expect(cell.y).toBeLessThan(5);
    });
  });
});
