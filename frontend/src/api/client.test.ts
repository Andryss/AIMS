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
  listAliens,
  createAlien,
  updateAlien,
  signIn,
  uploadFile,
  uploadFiles,
} from './client';
import * as api from './client';
import { clearFileCache } from '../utils/fileCache';

beforeEach(clearFileCache);

const mockFetch = jest.fn();
global.fetch = mockFetch;

describe('detention API', () => {
  beforeEach(() => mockFetch.mockReset());

  it('encodes search, incident and unified status filters', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [] }));
    await api.listDetentionRequests('tok', { q: '  Субъект  ', incidentId: 8, status: 'IN_CUSTODY', page: 1, size: 10 });
    const url = new URL(mockFetch.mock.calls[0][0], 'http://localhost');
    expect(url.pathname).toBe('/api/v1/detention-requests');
    expect(Object.fromEntries(url.searchParams)).toEqual({ q: 'Субъект', incidentId: '8', status: 'IN_CUSTODY', page: '1', size: '10' });
  });

  it.each([
    [() => api.getDetentionRequest('tok', 7), '/detention-requests/7', 'GET'],
    [() => api.listDetentionRequestHistory('tok', 7, 1, 10), '/detention-requests/7/history?page=1&size=10', 'GET'],
    [() => api.changeDetentionRequestStatus('tok', 7, { status: 'RELEASED', comment: 'Причина' }), '/detention-requests/7/status', 'PATCH'],
  ] as const)('uses detention endpoint %#', async (run, path, method) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, {})); await run();
    expect(mockFetch).toHaveBeenCalledWith(`/api/v1${path}`, expect.objectContaining({ method }));
  });

  it.each([false, true])('sends only editable fields, updating=%s', async (updating) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, {}));
    const body = { incidentId: 8, subjectName: 'Субъект', subjectDescription: 'Описание', alienId: 1, durationDays: 30 };
    if (updating) { await api.updateDetentionRequest('tok', 7, body); }
    else { await api.createDetentionRequest('tok', body); }
    expect(JSON.parse(mockFetch.mock.calls[0][1].body)).toEqual(body);
    expect(new Headers(mockFetch.mock.calls[0][1].headers).get('Authorization')).toBe('Bearer tok');
  });
});

describe('equipment API', () => {
  beforeEach(() => mockFetch.mockReset());

  it.each([
    ['equipment', () => api.listEquipment('tok', { q: '  SN  ', type: 'NEURALYZER', status: 'AVAILABLE',
      holderUserId: 3, page: 1, size: 10 }), '/api/v1/equipment', { q: 'SN', type: 'NEURALYZER',
      status: 'AVAILABLE', holderUserId: '3', page: '1', size: '10' }],
    ['movements', () => api.listEquipmentMovements('tok', { incidentId: 4, responsibleUserId: 3,
      equipmentId: 1, status: 'PENDING', operation: 'ISSUE', page: 0, size: 10 }),
    '/api/v1/equipment-movements', { incidentId: '4', responsibleUserId: '3', equipmentId: '1',
      status: 'PENDING', operation: 'ISSUE', page: '0', size: '10' }],
  ] as const)('encodes %s list filters', async (_, run, path, query) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [] }));
    await run();
    const url = new URL(mockFetch.mock.calls[0][0], 'http://localhost');
    expect(url.pathname).toBe(path);
    expect(Object.fromEntries(url.searchParams)).toEqual(query);
    expect(new Headers(mockFetch.mock.calls[0][1].headers).get('Authorization')).toBe('Bearer tok');
  });

  it.each([
    [() => api.getEquipment('tok', 1), '/equipment/1', 'GET'],
    [() => api.getEquipmentMovement('tok', 7), '/equipment-movements/7', 'GET'],
    [() => api.listEquipmentHistory('tok', 1, 1, 10), '/equipment/1/history?page=1&size=10', 'GET'],
    [() => api.listEquipmentMovementHistory('tok', 7, 1, 10),
      '/equipment-movements/7/history?page=1&size=10', 'GET'],
    [() => api.changeEquipmentMovementStatus('tok', 7, { status: 'CANCELLED', comment: 'Причина' }),
      '/equipment-movements/7/status', 'PATCH'],
  ] as const)('uses the equipment endpoint %#', async (run, path, method) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, {}));
    await run();
    expect(mockFetch).toHaveBeenCalledWith(`/api/v1${path}`, expect.objectContaining({ method }));
  });

  it('supports employee search without a role while preserving agent search', async () => {
    mockFetch.mockResolvedValue(jsonResponse(true, { items: [] }));
    await api.searchUsers('tok', 'ag');
    expect(mockFetch.mock.calls[0][0]).toBe('/api/v1/users/search?q=ag');
    await api.searchUsers('tok', 'ag', 'AGENT');
    expect(mockFetch.mock.calls[1][0]).toBe('/api/v1/users/search?q=ag&role=AGENT');
  });
});

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


  it('listAliens sends pagination and encodes the filter', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [], page: 1, size: 10 }));
    await listAliens('tok', { q: 'слиз %', page: 1, size: 10 });
    expect(mockFetch).toHaveBeenCalledWith(
      '/api/v1/aliens?page=1&size=10&q=%D1%81%D0%BB%D0%B8%D0%B7+%25', expect.any(Object));
    expect((mockFetch.mock.calls[0][1].headers as Headers).get('Authorization')).toBe('Bearer tok');
  });

  it.each(['create', 'update'])('sends the %s species request', async (mode) => {
    const body = { name: 'Слизень', description: 'Признаки', threatLevel: 3 };
    mockFetch.mockResolvedValueOnce(jsonResponse(true, { id: 1, ...body }));
    const saved = mode === 'create' ? await createAlien('tok', body) : await updateAlien('tok', 1, body);
    expect(saved.name).toBe('Слизень');
    expect(mockFetch).toHaveBeenCalledWith(
      mode === 'create' ? '/api/v1/aliens' : '/api/v1/aliens/1',
      expect.objectContaining({ method: mode === 'create' ? 'POST' : 'PUT', body: JSON.stringify(body) }));
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


describe('tenders API', () => {
  beforeEach(() => mockFetch.mockReset());

  it('encodes tender filters and authorization', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [] }));
    await api.listTenders('tok', { q: '  неделя  ', status: 'NEED', equipmentType: 'NEURALYZER',
      incidentId: 8, page: 1, size: 10 });
    const [url, options] = mockFetch.mock.calls[0];
    const parsed = new URL(url, 'http://localhost');
    expect(parsed.pathname).toBe('/api/v1/tenders');
    expect(Object.fromEntries(parsed.searchParams)).toEqual({ q: 'неделя', status: 'NEED',
      equipmentType: 'NEURALYZER', incidentId: '8', page: '1', size: '10' });
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer tok');
  });

  it.each([
    [() => api.getTender('tok', 4), '/tenders/4', 'GET', undefined],
    [() => api.createTender('tok', { equipmentType: 'NEURALYZER', quantity: 2 }), '/tenders', 'POST',
      { equipmentType: 'NEURALYZER', quantity: 2 }],
    [() => api.updateTender('tok', 4, { equipmentType: 'NEURALYZER', quantity: 3 }), '/tenders/4', 'PUT',
      { equipmentType: 'NEURALYZER', quantity: 3 }],
    [() => api.changeTenderStatus('tok', 4, { status: 'CANCELLED', comment: 'Причина' }),
      '/tenders/4/status', 'PATCH', { status: 'CANCELLED', comment: 'Причина' }],
    [() => api.listTenderParticipants('tok', 4, 1, 10), '/tenders/4/participants?page=1&size=10', 'GET', undefined],
    [() => api.createTenderParticipant('tok', 4, { name: 'Orbit', proposal: 'Delivery' }),
      '/tenders/4/participants', 'POST', { name: 'Orbit', proposal: 'Delivery' }],
    [() => api.updateTenderParticipant('tok', 4, 7, { name: 'Orbit', proposal: 'Updated' }),
      '/tenders/4/participants/7', 'PUT', { name: 'Orbit', proposal: 'Updated' }],
    [() => api.selectTenderWinner('tok', 4, { participantId: 7 }), '/tenders/4/winner', 'POST', { participantId: 7 }],
    [() => api.receiveTender('tok', 4, { equipment: [{ model: 'N', serialNumber: 'SN' }] }),
      '/tenders/4/receipt', 'POST', { equipment: [{ model: 'N', serialNumber: 'SN' }] }],
    [() => api.listTenderHistory('tok', 4, 1, 10), '/tenders/4/history?page=1&size=10', 'GET', undefined],
  ] as const)('sends the tender operation %s', async (run, path, method, body) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, {}));
    await run();
    const [url, options] = mockFetch.mock.calls[0];
    expect(url).toBe(`/api/v1${path}`);
    expect(options.method).toBe(method);
    expect(options.body).toBe(body ? JSON.stringify(body) : undefined);
    expect(new Headers(options.headers).get('Authorization')).toBe('Bearer tok');
  });
});


describe('statistics API', () => {
  beforeEach(() => mockFetch.mockReset());

  it.each([
    ['getIncidentCreatedCount', api.getIncidentCreatedCount, '/incidents/statistics/created-count'],
    ['getIncidentCompletedCount', api.getIncidentCompletedCount, '/incidents/statistics/completed-count'],
    ['getIncidentTotalAtTo', api.getIncidentTotalAtTo, '/incidents/statistics/total-at-to'],
    ['getIncidentCompletedAtTo', api.getIncidentCompletedAtTo, '/incidents/statistics/completed-at-to'],
    ['getIncidentOpenAtTo', api.getIncidentOpenAtTo, '/incidents/statistics/open-at-to'],
    ['getIncidentCompletionDuration', api.getIncidentCompletionDuration, '/incidents/statistics/completion-duration'],
    ['getIncidentTrend', api.getIncidentTrend, '/incidents/statistics/trend'],
    ['getIncidentByStatus', api.getIncidentByStatus, '/incidents/statistics/by-status'],
    ['getIncidentByEventType', api.getIncidentByEventType, '/incidents/statistics/by-event-type'],
    ['getIncidentByAlien', api.getIncidentByAlien, '/incidents/statistics/by-alien'],
    ['getIncidentByLocation', api.getIncidentByLocation, '/incidents/statistics/by-location'],
    ['getIncidentStale', api.getIncidentStale, '/incidents/statistics/stale'],
    ['getEquipmentReceivedCount', api.getEquipmentReceivedCount, '/equipment/statistics/received-count'],
    ['getEquipmentIssuedCount', api.getEquipmentIssuedCount, '/equipment/statistics/issued-count'],
    ['getEquipmentReturnedCount', api.getEquipmentReturnedCount, '/equipment/statistics/returned-count'],
    ['getEquipmentTotalAtTo', api.getEquipmentTotalAtTo, '/equipment/statistics/total-at-to'],
    ['getEquipmentAvailableAtTo', api.getEquipmentAvailableAtTo, '/equipment/statistics/available-at-to'],
    ['getEquipmentIssuedAtTo', api.getEquipmentIssuedAtTo, '/equipment/statistics/issued-at-to'],
    ['getEquipmentTrend', api.getEquipmentTrend, '/equipment/statistics/trend'],
    ['getEquipmentInventoryByType', api.getEquipmentInventoryByType, '/equipment/statistics/inventory-by-type'],
    ['getEquipmentOpenRequests', api.getEquipmentOpenRequests, '/equipment/statistics/open-requests'],
    ['getEquipmentExpectedDeliveriesByType', api.getEquipmentExpectedDeliveriesByType, '/equipment/statistics/expected-deliveries-by-type'],
    ['getEquipmentOutstandingByIncident', api.getEquipmentOutstandingByIncident, '/equipment/statistics/outstanding-by-incident'],
    ['getDetentionCreatedCount', api.getDetentionCreatedCount, '/detention-requests/statistics/created-count'],
    ['getDetentionAdmittedCount', api.getDetentionAdmittedCount, '/detention-requests/statistics/admitted-count'],
    ['getDetentionReleasedCount', api.getDetentionReleasedCount, '/detention-requests/statistics/released-count'],
    ['getDetentionAwaitingAtTo', api.getDetentionAwaitingAtTo, '/detention-requests/statistics/awaiting-at-to'],
    ['getDetentionInCustodyAtTo', api.getDetentionInCustodyAtTo, '/detention-requests/statistics/in-custody-at-to'],
    ['getDetentionOverdueAtTo', api.getDetentionOverdueAtTo, '/detention-requests/statistics/overdue-at-to'],
    ['getDetentionCustodyDuration', api.getDetentionCustodyDuration, '/detention-requests/statistics/custody-duration'],
    ['getDetentionTrend', api.getDetentionTrend, '/detention-requests/statistics/trend'],
    ['getDetentionByStatus', api.getDetentionByStatus, '/detention-requests/statistics/by-status'],
    ['getDetentionByAlien', api.getDetentionByAlien, '/detention-requests/statistics/by-alien'],
    ['getDetentionByAssignedTerm', api.getDetentionByAssignedTerm, '/detention-requests/statistics/by-assigned-term'],
    ['getDetentionByRemainingTime', api.getDetentionByRemainingTime, '/detention-requests/statistics/by-remaining-time'],
    ['getDetentionUpcomingReleases', api.getDetentionUpcomingReleases, '/detention-requests/statistics/upcoming-releases'],
  ] as const)('requests %s independently with its period and filters', async (_, load, path) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, {}));
    await load('tok', { from: '2025-05-01T03:00:00+03:00', to: '2025-06-01T00:00:00Z' });
    const url = new URL(mockFetch.mock.calls[0][0], 'http://localhost');
    expect(url.pathname).toBe('/api/v1' + path);
    expect(Object.fromEntries(url.searchParams)).toEqual({
      from: '2025-05-01T03:00:00+03:00', to: '2025-06-01T00:00:00Z',
    });
    expect(new Headers(mockFetch.mock.calls[0][1].headers).get('Authorization')).toBe('Bearer tok');
  });

  it.each([
    [() => api.getIncidentByStatus('tok', { from: 'from', to: 'to', eventType: 'ALIEN_CAPTURE', alienId: 2, responsibleUserId: 3 }),
      { eventType: 'ALIEN_CAPTURE', alienId: '2', responsibleUserId: '3' }],
    [() => api.getEquipmentInventoryByType('tok', { from: 'from', to: 'to', equipmentType: 'NEURALYZER' }),
      { equipmentType: 'NEURALYZER' }],
    [() => api.getDetentionByStatus('tok', { from: 'from', to: 'to', status: 'IN_CUSTODY', incidentId: 4, alienId: 2 }),
      { status: 'IN_CUSTODY', incidentId: '4', alienId: '2' }],
  ] as const)('encodes domain-specific filters %#', async (load, filters) => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, {}));
    await load();
    const url = new URL(mockFetch.mock.calls[0][0], 'http://localhost');
    expect(Object.fromEntries(url.searchParams)).toEqual({ from: 'from', to: 'to', ...filters });
  });
});

describe('file metadata and original cache', () => {
  beforeEach(() => mockFetch.mockReset());

  it('loads metadata with a single batch request', async () => {
    mockFetch.mockResolvedValueOnce(jsonResponse(true, { items: [] }));
    await api.batchFiles('tok', [8, 3]);
    expect(mockFetch).toHaveBeenCalledWith('/api/v1/files/batch', expect.objectContaining({
      method: 'POST', body: JSON.stringify({ ids: [8, 3] }),
    }));
  });

  it('uses the already loaded original for explicit download', async () => {
    mockFetch.mockResolvedValueOnce({ ok: true, blob: async () => new Blob(['image']), headers: new Headers() });
    await api.getFileBlob('file-test', 93);
    await api.downloadFile('file-test', 93, 'photo.jpg');
    expect(mockFetch).toHaveBeenCalledTimes(1);
  });
});

it('does not let a late upload from a previous session revoke current previews', async () => {
  mockFetch.mockReset();
  URL.createObjectURL = jest.fn(() => 'blob:current');
  URL.revokeObjectURL = jest.fn();
  let finishUpload!: (value: unknown) => void;
  mockFetch.mockImplementationOnce(() => new Promise((resolve) => { finishUpload = resolve; }))
    .mockResolvedValueOnce({ ok: true, blob: async () => new Blob(['current']), headers: new Headers() });
  const uploading = api.uploadFile('old-session', new File(['old'], 'old.png', { type: 'image/png' }));
  expect(await api.getFilePreviewUrl('new-session', 1)).toBe('blob:current');
  finishUpload(jsonResponse(true, { id: 9, fileName: 'old.png', contentType: 'image/png', fileSize: 3, createdAt: '' }));
  await uploading;
  expect(URL.revokeObjectURL).not.toHaveBeenCalled();
  expect(await api.getFilePreviewUrl('new-session', 1)).toBe('blob:current');
  expect(mockFetch).toHaveBeenCalledTimes(2);
});
