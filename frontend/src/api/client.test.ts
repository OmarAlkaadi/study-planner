// Unit-Tests für den API-Client (Vitest, Aufruf: npm test).
// fetch wird durch eine Testfunktion ersetzt – es läuft also kein echtes Backend.

import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, queryString, refreshSession, request, setUnauthorizedHandler, tokenStore } from './client';

/** Ersetzt fetch für einen Test durch eine Funktion, die immer diese Antwort liefert. */
function mockFetch(status: number, body?: unknown) {
  const response = new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  });
  const fetchMock = vi.fn().mockResolvedValue(response);
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

/**
 * Ersetzt fetch durch eine Funktion, die nacheinander diese Antworten liefert ([Status, JSON-Body]).
 * Jede Antwort wird frisch erzeugt, weil ein Response-Body nur einmal gelesen werden kann.
 */
function fetchSequence(...responses: [number, unknown?][]) {
  const queue = [...responses];
  const fetchMock = vi.fn().mockImplementation(async () => {
    const [status, body] = queue.shift() ?? [500, { detail: 'Keine weitere Antwort vorgesehen' }];
    return new Response(body === undefined ? null : JSON.stringify(body), { status });
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

afterEach(() => {
  vi.unstubAllGlobals();
  tokenStore.clear();
  setUnauthorizedHandler(null);
});

describe('queryString', () => {
  it('lässt leere Werte weg', () => {
    expect(queryString({ a: 1, b: '', c: null, d: undefined, e: false })).toBe('?a=1&e=false');
  });

  it('liefert einen leeren Text, wenn nichts gesetzt ist', () => {
    expect(queryString({ q: '' })).toBe('');
  });

  it('kodiert Sonderzeichen', () => {
    expect(queryString({ q: 'Mathe & Physik' })).toBe('?q=Mathe+%26+Physik');
  });
});

describe('request', () => {
  it('schickt JSON und liefert die Antwort', async () => {
    const fetchMock = mockFetch(200, { id: 1, name: 'Mathe' });
    const result = await request<{ id: number; name: string }>('/courses', { method: 'POST', body: { name: 'Mathe' } });

    expect(result).toEqual({ id: 1, name: 'Mathe' });
    const [url, init] = fetchMock.mock.calls[0];
    expect(url).toBe('/api/courses');
    expect(init.method).toBe('POST');
    expect(init.headers['Content-Type']).toBe('application/json');
    expect(init.body).toBe('{"name":"Mathe"}');
  });

  it('liefert bei 204 No Content undefined', async () => {
    mockFetch(204);
    await expect(request('/courses/1', { method: 'DELETE' })).resolves.toBeUndefined();
  });

  it('wandelt Fehlerantworten in ApiError mit Meldung, Feldfehlern und Code um', async () => {
    mockFetch(403, {
      status: 403,
      detail: 'Bitte bestätige zuerst deine E-Mail-Adresse.',
      errors: { email: 'ungültig' },
      code: 'EMAIL_NOT_VERIFIED',
    });

    const error = await request('/auth/login', { method: 'POST', body: {} }).catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    const apiError = error as ApiError;
    expect(apiError.status).toBe(403);
    expect(apiError.message).toBe('Bitte bestätige zuerst deine E-Mail-Adresse.');
    expect(apiError.fieldErrors).toEqual({ email: 'ungültig' });
    expect(apiError.code).toBe('EMAIL_NOT_VERIFIED');
  });

  it('meldet einen nicht erreichbaren Server mit Status 0', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')));
    const error = (await request('/courses').catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(0);
    expect(error.message).toContain('nicht erreichbar');
  });
});

describe('automatisches Erneuern der Anmeldung (Refresh-Token)', () => {
  const session = { token: 'neues-access-token', user: { id: 1, email: 'a@b.de', displayName: 'A' } };

  it('erneuert ein abgelaufenes Access-Token und wiederholt die Anfrage', async () => {
    tokenStore.set('abgelaufen');
    const fetchMock = fetchSequence([401], [200, session], [200, [{ id: 7 }]]);

    await expect(request('/courses')).resolves.toEqual([{ id: 7 }]);

    expect(fetchMock).toHaveBeenCalledTimes(3);
    expect(fetchMock.mock.calls[1][0]).toBe('/api/auth/refresh');
    expect(fetchMock.mock.calls[2][1].headers.Authorization).toBe('Bearer neues-access-token');
    expect(tokenStore.get()).toBe('neues-access-token');
  });

  it('meldet ab, wenn auch das Erneuern scheitert', async () => {
    tokenStore.set('abgelaufen');
    const onUnauthorized = vi.fn();
    setUnauthorizedHandler(onUnauthorized);
    fetchSequence([401], [401, { detail: 'Sitzung abgelaufen' }]);

    const error = (await request('/courses').catch((e: unknown) => e)) as ApiError;
    expect(error.status).toBe(401);
    expect(error.message).toContain('Sitzung ist abgelaufen');
    expect(onUnauthorized).toHaveBeenCalledOnce();
    expect(tokenStore.get()).toBeNull();
  });

  it('versucht es ohne Token (nicht angemeldet) gar nicht erst mit Erneuern', async () => {
    const fetchMock = fetchSequence([401, { detail: 'E-Mail oder Passwort ist falsch.' }]);
    const error = (await request('/auth/login', { method: 'POST', body: {} }).catch((e: unknown) => e)) as ApiError;
    expect(error.message).toBe('E-Mail oder Passwort ist falsch.');
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('schickt bei gleichzeitigen Aufrufen nur EINE Erneuerung ab', async () => {
    const fetchMock = fetchSequence([200, session]);
    const [a, b] = await Promise.all([refreshSession(), refreshSession()]);
    expect(a).toEqual(session);
    expect(b).toEqual(session);
    expect(fetchMock).toHaveBeenCalledOnce();
  });

  it('versucht es erneut, wenn ein anderer Tab das Token gerade erneuert hat', async () => {
    const fetchMock = fetchSequence([401, { code: 'REFRESH_TOKEN_ROTATED' }], [200, session]);
    await expect(refreshSession()).resolves.toEqual(session);
    expect(fetchMock).toHaveBeenCalledTimes(2);
  });

  it('liefert null, wenn kein Refresh-Cookie da ist (nicht angemeldet)', async () => {
    const fetchMock = fetchSequence([401, { detail: 'Nicht angemeldet.' }]);
    await expect(refreshSession()).resolves.toBeNull();
    expect(fetchMock).toHaveBeenCalledOnce();
  });
});
