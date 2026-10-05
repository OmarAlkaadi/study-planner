// Eigene React-Hooks, die von fast allen Seiten benutzt werden.

import { useCallback, useEffect, useRef, useState } from 'react';
import { errorMessage } from '../api/client';

/**
 * Lädt Daten über eine API-Funktion und merkt sich Ladezustand und Fehler.
 * Wenn sich `loader` ändert (z. B. neue Filter), wird automatisch neu geladen.
 * Veraltete Antworten (z. B. beim schnellen Tippen im Suchfeld) werden ignoriert.
 */
export function useApiData<T>(loader: () => Promise<T>) {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const latestRequest = useRef(0);

  const reload = useCallback(async () => {
    const requestId = ++latestRequest.current;
    setLoading(true);
    try {
      const result = await loader();
      if (requestId === latestRequest.current) {
        setData(result);
        setError(null);
      }
    } catch (e) {
      if (requestId === latestRequest.current) setError(errorMessage(e));
    } finally {
      if (requestId === latestRequest.current) setLoading(false);
    }
  }, [loader]);

  useEffect(() => {
    // Laden von Serverdaten ist genau der Fall, für den Effekte gedacht sind (externes System)
    // oxlint-disable-next-line react/set-state-in-effect
    void reload();
  }, [reload]);

  // setData erlaubt "optimistische" Änderungen: die Oberfläche reagiert sofort,
  // danach wird mit reload() der echte Stand vom Server geholt
  return { data, error, loading, reload, setData };
}

/** Gibt den Wert erst zurück, wenn er sich `delay` ms nicht mehr geändert hat (für Suchfelder). */
export function useDebouncedValue<T>(value: T, delay = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}
