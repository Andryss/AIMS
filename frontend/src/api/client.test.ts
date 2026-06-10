import {
  ApiError,
  changeIncidentStatus,
  createIncident,
  downloadFile,
  getAlien,
  getAuthMe,
  getIncident,
  getUnreadNotificationsCount,
  listIncidents,
  listNotifications,
  markNotificationRead,
  putIncidentAlien,
  searchAliens,
  signIn,
  uploadFile,
  uploadFiles,
} from './client';

const mockFetch = jest.fn();
global.fetch = mockFetch;

function jsonResponse(
  ok: boolean,
  payload: unknown,
  init?: { status?: number; statusText?: string; headers?: Record<string, string> },
): Response {
  const status = init?.status ?? (ok ? 200 : 500);
  const statusText = init?.statusText ?? (ok ? 'OK' : 'Error');
  const text =
    payload === undefined || payload === null
      ? ''
      : typeof payload === 'string'
        ? payload
        : JSON.stringify(payload);
  const headers = new Headers(init?.headers);
  return {
    ok,
    status,
    statusText,
    headers,
    text: jest.fn().mockResolvedValue(text),
    json: jest.fn().mockResolvedValue(
      typeof payload === 'string' ? JSON.parse(payload) : payload,
    ),
    blob: jest.fn().mockResolvedValue(new Blob(['data'])),
  } as unknown as Response;
}

describe('api client', () => {
  beforeEach(() => {
    mockFetch.mockReset();
  });

  describe('ApiError', () => {
    it('uses humanMessage when present', () => {
      const error = new ApiError(400, { humanMessage: 'Ошибка', message: 'err' });
      expect(error.message).toBe('Ошибка');
      expect(error.status).toBe(400);
    });

    it('falls back to message or HTTP status', () => {
      expect(new ApiError(500, { message: 'fail' }).message).toBe('fail');
      expect(new ApiError(502, {}).message).toBe('HTTP 502');
    });
  });

  describe('signIn', () => {
    it('POSTs credentials to /api/v1/auth/signin', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { accessToken: 'token-1' }));
      const result = await signIn({ login: 'operator', password: 'operator' });
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/auth/signin',
        expect.objectContaining({
          method: 'POST',
          body: JSON.stringify({ login: 'operator', password: 'operator' }),
        }),
      );
      expect(result.accessToken).toBe('token-1');
    });

    it('throws ApiError on failure with JSON body', async () => {
      mockFetch.mockResolvedValueOnce(
        jsonResponse(false, { humanMessage: 'Неверный пароль', code: 401 }, { status: 401 }),
      );
      await expect(signIn({ login: 'x', password: 'y' })).rejects.toMatchObject({
        status: 401,
        message: 'Неверный пароль',
      });
    });

    it('throws ApiError when error body is not JSON', async () => {
      mockFetch.mockResolvedValueOnce({
        ok: false,
        status: 500,
        statusText: 'Server Error',
        json: jest.fn().mockRejectedValue(new Error('parse')),
      } as unknown as Response);
      await expect(signIn({ login: 'x', password: 'y' })).rejects.toBeInstanceOf(ApiError);
    });
  });

  describe('getAuthMe', () => {
    it('sends Authorization header', async () => {
      mockFetch.mockResolvedValueOnce(
        jsonResponse(true, { login: 'analyst', roles: ['ANALYST'], permissions: [] }),
      );
      const me = await getAuthMe('tok');
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/auth/me',
        expect.objectContaining({
          method: 'GET',
          headers: expect.any(Headers),
        }),
      );
      const headers = mockFetch.mock.calls[0][1].headers as Headers;
      expect(headers.get('Authorization')).toBe('Bearer tok');
      expect(me.login).toBe('analyst');
    });
  });

  describe('incidents', () => {
    it('listIncidents requests pagination', async () => {
      mockFetch.mockResolvedValueOnce(
        jsonResponse(true, { items: [], page: 1, size: 5, totalElements: 0, totalPages: 0 }),
      );
      await listIncidents('tok', 1, 5);
      expect(mockFetch).toHaveBeenCalledWith('/api/v1/incidents?page=1&size=5', expect.any(Object));
    });

    it('createIncident POSTs JSON body', async () => {
      const payload = {
        eventType: 'UNIDENTIFIED_SIGHTING' as const,
        location: 'Moscow',
        detectedAt: '2025-01-01T00:00:00Z',
        description: 'desc',
        attachmentFileIds: [1],
      };
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { id: 7, ...payload, status: 'DRAFT' }));
      const created = await createIncident('tok', payload);
      expect(created.id).toBe(7);
    });

    it('getIncident and changeIncidentStatus', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { id: 3, status: 'DRAFT' }));
      await getIncident('tok', 3);
      expect(mockFetch).toHaveBeenLastCalledWith('/api/v1/incidents/3', expect.any(Object));

      mockFetch.mockResolvedValueOnce(jsonResponse(true, { id: 3, status: 'READY_FOR_ANALYSIS' }));
      await changeIncidentStatus('tok', 3, 'READY_FOR_ANALYSIS');
      expect(mockFetch).toHaveBeenLastCalledWith(
        '/api/v1/incidents/3/status',
        expect.objectContaining({ method: 'POST' }),
      );
    });

    it('putIncidentAlien links alien', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { id: 2, alienId: 5 }));
      await putIncidentAlien('tok', 2, 5);
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/incidents/2/alien',
        expect.objectContaining({ method: 'PUT', body: JSON.stringify({ alienId: 5 }) }),
      );
    });
  });

  describe('aliens', () => {
    it('searchAliens encodes query', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [] }));
      await searchAliens('tok', 'слиз');
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/aliens/search?q=%D1%81%D0%BB%D0%B8%D0%B7',
        expect.any(Object),
      );
    });

    it('getAlien fetches by id', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { id: 1, name: 'Слизень' }));
      const alien = await getAlien('tok', 1);
      expect(alien.name).toBe('Слизень');
    });
  });

  describe('files', () => {
    it('uploadFile sends multipart form', async () => {
      mockFetch.mockResolvedValueOnce(
        jsonResponse(true, { id: 9, fileName: 'a.txt', contentType: 'text/plain', fileSize: 1 }),
      );
      const file = new File(['x'], 'a.txt', { type: 'text/plain' });
      const uploaded = await uploadFile('tok', file);
      expect(uploaded.id).toBe(9);
      const body = mockFetch.mock.calls[0][1].body as FormData;
      expect(body).toBeInstanceOf(FormData);
    });

    it('uploadFiles uploads each file', async () => {
      mockFetch
        .mockResolvedValueOnce(jsonResponse(true, { id: 1 }))
        .mockResolvedValueOnce(jsonResponse(true, { id: 2 }));
      const files = [
        new File(['a'], 'a.txt'),
        new File(['b'], 'b.txt'),
      ];
      const result = await uploadFiles('tok', files);
      expect(result).toHaveLength(2);
      expect(mockFetch).toHaveBeenCalledTimes(2);
    });

    it('downloadFile triggers blob download', async () => {
      const createObjectURL = jest.fn().mockReturnValue('blob:url');
      const revokeObjectURL = jest.fn();
      global.URL.createObjectURL = createObjectURL;
      global.URL.revokeObjectURL = revokeObjectURL;

      const click = jest.fn();
      const originalCreateElement = document.createElement.bind(document);
      jest.spyOn(document, 'createElement').mockImplementation((tag: string) => {
        if (tag === 'a') {
          return { click, download: '', href: '' } as unknown as HTMLAnchorElement;
        }
        return originalCreateElement(tag);
      });

      mockFetch.mockResolvedValueOnce(
        jsonResponse(true, null, {
          headers: { 'Content-Disposition': 'attachment; filename="report.pdf"' },
        }),
      );

      await downloadFile('tok', 42);
      expect(mockFetch).toHaveBeenCalledWith(
        '/api/v1/files/42',
        expect.objectContaining({ headers: { Authorization: 'Bearer tok' } }),
      );
      expect(click).toHaveBeenCalled();
      expect(revokeObjectURL).toHaveBeenCalledWith('blob:url');

      jest.restoreAllMocks();
    });
  });

  describe('notifications', () => {
    it('lists, counts unread and marks read', async () => {
      mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [], page: 0, size: 20 }));
      await listNotifications('tok', 0, 20);

      mockFetch.mockResolvedValueOnce(jsonResponse(true, { count: 3 }));
      const count = await getUnreadNotificationsCount('tok');
      expect(count.count).toBe(3);

      mockFetch.mockResolvedValueOnce(jsonResponse(true, null, { status: 204 }));
      await markNotificationRead('tok', 11);
      expect(mockFetch).toHaveBeenLastCalledWith(
        '/api/v1/notifications/11/read',
        expect.objectContaining({ method: 'PATCH' }),
      );
    });
  });

  it('returns undefined for empty 200 body', async () => {
    mockFetch.mockResolvedValueOnce({
      ok: true,
      status: 200,
      text: jest.fn().mockResolvedValue(''),
    } as unknown as Response);
    const result = await markNotificationRead('tok', 1);
    expect(result).toBeUndefined();
  });
});
