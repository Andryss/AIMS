export interface IdenticonData {
  background: string;
  foreground: string;
  cells: Array<{ x: number; y: number }>;
  gridSize: number;
}

function hashLogin(login: string): number {
  let hash = 0;
  for (let i = 0; i < login.length; i += 1) {
    hash = login.charCodeAt(i) + ((hash << 5) - hash);
    hash |= 0;
  }
  return Math.abs(hash);
}

/**
 * Генерирует симметричный identicon по логину (аналог GitHub/GitLab).
 */
export function buildIdenticon(login: string): IdenticonData {
  const hash = hashLogin(login.trim().toLowerCase());
  const gridSize = 5;
  const hue = hash % 360;
  const background = `hsl(${hue}, 42%, 88%)`;
  const foreground = `hsl(${hue}, 52%, 36%)`;

  const cells: Array<{ x: number; y: number }> = [];
  let bit = 0;

  for (let y = 0; y < gridSize; y += 1) {
    for (let x = 0; x < Math.ceil(gridSize / 2); x += 1) {
      const filled = ((hash >> bit) & 1) === 1;
      bit += 1;

      if (filled) {
        cells.push({ x, y });
        const mirrorX = gridSize - 1 - x;
        if (mirrorX !== x) {
          cells.push({ x: mirrorX, y });
        }
      }
    }
  }

  return { background, foreground, cells, gridSize };
}
