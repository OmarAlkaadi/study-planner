// Zentrale Stelle für alle HTTP-Anfragen an das Backend.
// - hängt das Access-Token (JWT) automatisch an
// - erneuert es bei Ablauf automatisch über das Refresh-Token (httpOnly-Cookie) und wiederholt die Anfrage
// - wandelt Fehlerantworten (Problem Details) in ApiError um

import type { AuthResponse } from './types';

/**
 * Das Access-Token liegt nur im Arbeitsspeicher (nicht im localStorage): Eingeschleuster Schadcode
 * könnte localStorage auslesen. Nach dem Neuladen der Seite holt sich die App über das Refresh-Cookie
 * ein neues (siehe refreshSession). Das Refresh-Token selbst kann JavaScript gar nicht lesen.
 */
let accessToken: string | null = null;

/** Lesen, Setzen und Löschen des Access-Tokens im Arbeitsspeicher. */
export const tokenStore = {
  get: (): string | null => accessToken,
  set(token: string) {
    accessToken = token;
  },
  clear() {
    accessToken = null;
  },
};

// Aufräumen: Frühere Versionen der App haben das Token im localStorage gespeichert – dort soll es nicht bleiben
try {
  localStorage.removeItem('studyplanner.token');
} catch {
  // localStorage nicht verfügbar (z. B. privater Modus) – dann gibt es auch nichts aufzuräumen
}

/**
 * Fehler einer API-Anfrage: HTTP-Status (0 = Server nicht erreichbar), Meldung vom Backend,
 * Fehlermeldungen pro Formularfeld (aus "errors" der Problem-Details-Antwort) und ein optionaler
 * Fehlercode für Sonderfälle (z. B. "EMAIL_NOT_VERIFIED" beim Login).
 */
export class ApiError extends Error {
  readonly status: number;
  readonly fieldErrors: Record<string, string>;
  readonly code: string | null;

  constructor(status: number, message: string, fieldErrors: Record<string, string> = {}, code: string | null = null) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.fieldErrors = fieldErrors;
    this.code = code;
  }
}

/** Liefert eine lesbare Fehlermeldung für beliebige Fehler. */
export function errorMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  return 'Unbekannter Fehler';
}

/** Funktion, die bei endgültig abgelaufener Sitzung aufgerufen wird (setzt der AuthProvider). */
let unauthorizedHandler: (() => void) | null = null;

/** Wird aufgerufen, wenn die Sitzung endgültig abgelaufen ist (Access-Token ungültig und Erneuern nicht möglich). */
export function setUnauthorizedHandler(handler: (() => void) | null) {
  unauthorizedHandler = handler;
}

/** Fehlercode des Backends: Ein anderer Tab hat das Refresh-Token gerade erneuert. */
const REFRESH_TOKEN_ROTATED = 'REFRESH_TOKEN_ROTATED';

/** Laufende Erneuerung – damit gleichzeitige Aufrufe nur EINE Anfrage auslösen (siehe refreshSession). */
let refreshInFlight: Promise<AuthResponse | null> | null = null;

/**
 * Erneuert die Anmeldung über das Refresh-Token-Cookie (POST /api/auth/refresh).
 * Liefert Access-Token + Benutzer oder null, wenn man nicht (mehr) angemeldet ist.
 *
 * Gleichzeitige Aufrufe teilen sich eine Anfrage: Jedes Erneuern ersetzt das Refresh-Token, ein zweiter
 * paralleler Aufruf mit dem alten Token würde abgelehnt. Das passiert z. B. beim Start der App im
 * Entwicklungsmodus (React StrictMode) oder wenn mehrere Anfragen gleichzeitig ein abgelaufenes Token melden.
 */
export function refreshSession(): Promise<AuthResponse | null> {
  refreshInFlight ??= doRefresh().finally(() => {
    refreshInFlight = null;
  });
  return refreshInFlight;
}

/** Die eigentliche Erneuerung; der Browser schickt das httpOnly-Cookie automatisch mit. */
async function doRefresh(): Promise<AuthResponse | null> {
  // Höchstens zwei Versuche: Hat ein anderer Tab gerade erneuert, hat der Browser danach das neue Cookie
  for (let attempt = 0; attempt < 2; attempt++) {
    let response: Response;
    try {
      response = await fetch('/api/auth/refresh', { method: 'POST' });
    } catch {
      return null;
    }
    if (response.ok) {
      const data = (await response.json()) as AuthResponse;
      accessToken = data.token;
      return data;
    }
    const code = await response
      .json()
      .then((body) => (typeof body?.code === 'string' ? body.code : null))
      .catch(() => null);
    if (code !== REFRESH_TOKEN_ROTATED) break;
    await new Promise((resolve) => setTimeout(resolve, 250));
  }
  accessToken = null;
  return null;
}

/**
 * Optionen für request(): HTTP-Methode (Standard GET) und optionaler Body, der als JSON gesendet wird.
 */
interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
  body?: unknown;
}

/**
 * Führt eine Anfrage an /api + path aus (von api.ts für jeden Endpunkt benutzt).
 *
 * 1. hängt das Access-Token als Bearer-Token an und wandelt den Body in JSON um
 * 2. Netzwerkfehler → ApiError mit Status 0
 * 3. HTTP 401 trotz Token → Token ist abgelaufen: einmal über das Refresh-Cookie erneuern und die Anfrage
 *    wiederholen. Klappt das nicht, ist die Sitzung vorbei → automatisch abmelden.
 * 4. sonstiger Fehlerstatus → Meldung, Feldfehler und Code aus der Antwort lesen
 * 5. Erfolg → JSON-Antwort zurückgeben (bei 204 No Content: undefined)
 */
export async function request<T>(path: string, options: RequestOptions = {}, isRetry = false): Promise<T> {
  const token = accessToken;
  const headers: Record<string, string> = {};
  if (token) headers.Authorization = `Bearer ${token}`;
  if (options.body !== undefined) headers['Content-Type'] = 'application/json';

  let response: Response;
  try {
    response = await fetch(`/api${path}`, {
      method: options.method ?? 'GET',
      headers,
      body: options.body !== undefined ? JSON.stringify(options.body) : undefined,
    });
  } catch {
    throw new ApiError(0, 'Der Server ist nicht erreichbar. Läuft das Backend?');
  }

  // Abgelaufenes Access-Token: still erneuern und die Anfrage einmal wiederholen
  if (response.status === 401 && token && !isRetry && (await refreshSession())) {
    return request<T>(path, options, true);
  }

  if (!response.ok) {
    let message = `Fehler ${response.status}`;
    let fieldErrors: Record<string, string> = {};
    let code: string | null = null;
    try {
      const data = await response.json();
      if (typeof data?.detail === 'string') message = data.detail;
      if (data?.errors && typeof data.errors === 'object') fieldErrors = data.errors;
      if (typeof data?.code === 'string') code = data.code;
    } catch {
      // Antwort ohne JSON-Body (z. B. 401 von Spring Security)
    }
    if (response.status === 401 && token) {
      message = 'Deine Sitzung ist abgelaufen. Bitte melde dich erneut an.';
      unauthorizedHandler?.();
    }
    throw new ApiError(response.status, message, fieldErrors, code);
  }

  if (response.status === 204) {
    return undefined as T;
  }
  return (await response.json()) as T;
}

/** Baut einen Query-String und lässt leere Werte weg: {a: 1, b: ''} -> "?a=1" */
export function queryString(params: Record<string, string | number | boolean | null | undefined>): string {
  const search = new URLSearchParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(key, String(value));
    }
  }
  const result = search.toString();
  return result ? `?${result}` : '';
}
