import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { authApi } from '../api/api';
import { refreshSession, setUnauthorizedHandler, tokenStore } from '../api/client';
import type { User } from '../api/types';
import { AuthContext, type AuthContextValue } from './auth-context';

/**
 * Verwaltet den Login-Zustand der ganzen App und stellt ihn über den AuthContext bereit.
 *
 * - Beim Start: POST /api/auth/refresh mit dem httpOnly-Cookie. Ist man noch angemeldet, kommen ein neues
 *   Access-Token und der Benutzer zurück – sonst bleibt man abgemeldet. (Das Access-Token liegt nur im
 *   Arbeitsspeicher und ist nach dem Neuladen der Seite weg.)
 * - login: API aufrufen, Access-Token merken, Benutzer setzen (das Refresh-Cookie setzt das Backend).
 * - register: legt nur das Konto an – angemeldet wird erst nach Bestätigung der E-Mail und normalem Login.
 * - updateUser: übernimmt geänderte Benutzerdaten (Seite "Konto").
 * - logout: Backend widerruft das Refresh-Token und löscht das Cookie; lokal werden Token und Benutzer gelöscht.
 *   Ist die Sitzung endgültig abgelaufen, wird lokal automatisch abgemeldet (setUnauthorizedHandler).
 *
 * useCallback/useMemo sorgen dafür, dass nicht bei jedem Rendern neue Funktionen entstehen
 * und alle Komponenten, die useAuth() benutzen, unnötig neu rendern.
 */
export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  // Beim Start ist unklar, ob noch ein gültiges Refresh-Cookie existiert – das klärt der Effekt unten
  const [loading, setLoading] = useState(true);

  // Nur lokal abmelden (z. B. wenn das Backend die Sitzung schon beendet hat)
  const clearSession = useCallback(() => {
    tokenStore.clear();
    setUser(null);
  }, []);

  // Abmelden: Backend informieren (Refresh-Token widerrufen, Cookie löschen), dann lokal abmelden.
  // Fehler beim Backend-Aufruf werden ignoriert – lokal ist man in jedem Fall abgemeldet.
  const logout = useCallback(() => {
    void authApi.logout().catch(() => undefined);
    clearSession();
  }, [clearSession]);

  // Sitzung endgültig abgelaufen (Erneuern nicht möglich) → automatisch lokal abmelden
  useEffect(() => {
    setUnauthorizedHandler(clearSession);
    return () => setUnauthorizedHandler(null);
  }, [clearSession]);

  // Beim Start: Anmeldung über das Refresh-Cookie wiederherstellen
  useEffect(() => {
    let cancelled = false;
    refreshSession()
      .then((session) => {
        if (!cancelled && session) setUser(session.user);
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, []);

  // Fehler (ApiError) werden nicht abgefangen, sondern an die Seiten weitergereicht und dort angezeigt
  const login = useCallback(async (email: string, password: string) => {
    const response = await authApi.login(email, password);
    tokenStore.set(response.token);
    setUser(response.user);
  }, []);

  // Registrieren meldet nicht an – zurück kommt nur die Meldung "Wir haben dir eine E-Mail geschickt …"
  const register = useCallback(
    async (email: string, password: string, displayName: string) =>
      (await authApi.register(email, password, displayName)).message,
    [],
  );

  // Wert des Contexts – ändert sich nur, wenn sich einer der Bestandteile ändert
  const value = useMemo<AuthContextValue>(
    () => ({ user, loading, login, register, updateUser: setUser, logout }),
    [user, loading, login, register, logout],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
