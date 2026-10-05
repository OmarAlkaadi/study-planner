import { createContext } from 'react';
import type { User } from '../api/types';

/**
 * Was der Auth-Context allen Komponenten bereitstellt: Benutzer, Ladezustand und die Funktionen
 * rund um Anmeldung und Konto.
 */
export interface AuthContextValue {
  user: User | null;
  /** true, solange beim Start geprüft wird, ob man noch angemeldet ist (Refresh-Cookie) */
  loading: boolean;
  login: (email: string, password: string) => Promise<void>;
  /** Registriert ein Konto und liefert die Meldung "Wir haben dir eine E-Mail geschickt …" (noch kein Login) */
  register: (email: string, password: string, displayName: string) => Promise<string>;
  /** Übernimmt geänderte Benutzerdaten (z. B. neuer Name auf der Seite "Konto") */
  updateUser: (user: User) => void;
  /** Abmelden auf diesem Gerät (Refresh-Token wird im Backend widerrufen, Cookie gelöscht) */
  logout: () => void;
}

/**
 * React-Context für den Login-Zustand. Befüllt wird er im AuthProvider, gelesen über useAuth().
 * Steht in einer eigenen Datei, weil Komponenten-Dateien für Fast Refresh nur Komponenten exportieren sollen.
 */
export const AuthContext = createContext<AuthContextValue | null>(null);
