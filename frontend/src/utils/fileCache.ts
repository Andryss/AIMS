interface FileCache {
  token: string;
  blobs: Map<number, Promise<Blob>>;
  urls: Map<number, Promise<string>>;
  resolvedUrls: Set<string>;
}

let active: FileCache | null = null;

export function clearFileCache(): void {
  active?.resolvedUrls.forEach((url) => URL.revokeObjectURL(url));
  active = null;
}

function session(token: string): FileCache {
  if (active?.token !== token) {
    clearFileCache();
    active = { token, blobs: new Map(), urls: new Map(), resolvedUrls: new Set() };
  }
  return active;
}

export function rememberFile(token: string, file: Blob): (id: number) => void {
  const cache = active ?? session(token);
  return (id) => {
    if (active === cache && cache.token === token) {
      cache.blobs.set(id, Promise.resolve(file));
    }
  };
}

export function readFileBlob(token: string, id: number, load: () => Promise<Blob>): Promise<Blob> {
  const cache = session(token);
  let pending = cache.blobs.get(id);
  if (!pending) {
    pending = load().then((blob) => {
      if (active !== cache) { throw new Error('Сессия изменилась'); }
      return blob;
    }).catch((error) => { cache.blobs.delete(id); throw error; });
    cache.blobs.set(id, pending);
  }
  return pending;
}

export function readFileUrl(token: string, id: number, load: () => Promise<Blob>): Promise<string> {
  const cache = session(token);
  let pending = cache.urls.get(id);
  if (!pending) {
    pending = readFileBlob(token, id, load).then((blob) => {
      if (active !== cache) { throw new Error('Сессия изменилась'); }
      const url = URL.createObjectURL(blob);
      cache.resolvedUrls.add(url);
      return url;
    }).catch((error) => { cache.urls.delete(id); throw error; });
    cache.urls.set(id, pending);
  }
  return pending;
}
