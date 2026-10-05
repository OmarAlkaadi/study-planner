import { createContext } from 'react';

/** Inhalt einer Sicherheitsabfrage, z. B. vor dem Löschen. */
export interface ConfirmOptions {
  title: string;
  message: string;
  /** Beschriftung des Bestätigungs-Buttons (Standard: "OK") */
  confirmLabel?: string;
  /** true = roter Button für endgültige Aktionen wie Löschen */
  danger?: boolean;
}

/**
 * Was der Dialog-Context bereitstellt: eigene Dialoge im Stil der App statt der Browser-Fenster
 * window.confirm() und window.alert().
 */
export interface DialogContextValue {
  /** Zeigt eine Sicherheitsabfrage und liefert true (bestätigt) oder false (abgebrochen). */
  confirm: (options: ConfirmOptions) => Promise<boolean>;
  /** Zeigt eine Fehlermeldung mit OK-Button. */
  showError: (message: string) => void;
}

/**
 * React-Context für die Dialoge. Befüllt im DialogProvider, gelesen über useDialogs().
 * Steht in einer eigenen Datei, weil Komponenten-Dateien für Fast Refresh nur Komponenten exportieren sollen.
 */
export const DialogContext = createContext<DialogContextValue | null>(null);
