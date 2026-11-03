import { clearFileCache, readFileBlob, readFileUrl, rememberFile } from './fileCache';

beforeEach(() => {
  URL.createObjectURL = jest.fn(() => 'blob:preview');
  URL.revokeObjectURL = jest.fn();
  clearFileCache();
});

afterEach(clearFileCache);

it('deduplicates concurrent reads and reuses an object URL by file ID', async () => {
  const blob = new Blob(['image']);
  const load = jest.fn().mockResolvedValue(blob);
  const values = await Promise.all([readFileBlob('token', 1, load), readFileBlob('token', 1, load)]);
  expect(values).toEqual([blob, blob]);
  expect(load).toHaveBeenCalledTimes(1);
  expect(await readFileUrl('token', 1, load)).toBe('blob:preview');
  expect(await readFileUrl('token', 1, load)).toBe('blob:preview');
  expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
  expect(load).toHaveBeenCalledTimes(1);
});

it('keeps IDs separate and retries a failed download', async () => {
  const load = jest.fn().mockRejectedValueOnce(new Error('offline')).mockResolvedValue(new Blob(['ok']));
  await expect(readFileBlob('token', 1, load)).rejects.toThrow('offline');
  await readFileBlob('token', 1, load);
  await readFileBlob('token', 2, load);
  expect(load).toHaveBeenCalledTimes(3);
});

it('uses the uploaded original immediately without downloading it again', async () => {
  const original = new File(['original'], 'photo.png', { type: 'image/png' });
  rememberFile('token', original)(7);
  const load = jest.fn();
  expect(await readFileBlob('token', 7, load)).toBe(original);
  expect(await readFileUrl('token', 7, load)).toBe('blob:preview');
  expect(load).not.toHaveBeenCalled();
  clearFileCache();
  expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:preview');
});

it('isolates sessions and discards an old in-flight download after logout', async () => {
  let finish!: (value: Blob) => void;
  const old = readFileUrl('old', 1, () => new Promise((resolve) => { finish = resolve; }));
  clearFileCache();
  const load = jest.fn().mockResolvedValue(new Blob(['new']));
  expect(await readFileUrl('new', 1, load)).toBe('blob:preview');
  finish(new Blob(['old']));
  await expect(old).rejects.toThrow();
  expect(URL.createObjectURL).toHaveBeenCalledTimes(1);
  expect(load).toHaveBeenCalledTimes(1);
});
