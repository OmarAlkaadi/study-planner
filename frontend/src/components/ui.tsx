// Kleine, wiederverwendbare UI-Bausteine, die auf allen Seiten benutzt werden.

import type { ReactNode } from 'react';

/** Seitenkopf mit Titel, optionalem Untertitel und Aktions-Buttons rechts. */
export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: string; actions?: ReactNode }) {
  return (
    <header className="page-header">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {actions && <div className="page-actions">{actions}</div>}
    </header>
  );
}

/** Fehlermeldung als roter Kasten – zeigt nichts an, wenn message null ist. */
export function ErrorMessage({ message }: { message: string | null }) {
  if (!message) return null;
  return (
    <div className="alert" role="alert">
      {message}
    </div>
  );
}

/** Platzhalter für leere Listen (z. B. "Noch keine Kurse"). */
export function EmptyState({ title, text, action }: { title: string; text?: string; action?: ReactNode }) {
  return (
    <div className="empty-state">
      <p className="empty-title">{title}</p>
      {text && <p className="muted">{text}</p>}
      {action}
    </div>
  );
}

/** Ladeanzeige. */
export function Loading() {
  return <p className="muted loading">Lädt …</p>;
}

/** Farbiger Punkt für die Kursfarbe (grau, wenn keine Farbe gesetzt ist). */
export function ColorDot({ color }: { color: string | null }) {
  return <span className="color-dot" style={{ background: color ?? 'var(--muted)' }} aria-hidden="true" />;
}

/**
 * Formularfeld: Beschriftung, das Eingabeelement (children) und darunter die Fehlermeldung vom Backend.
 */
export function Field({ label, error, children }: { label: string; error?: string; children: ReactNode }) {
  return (
    <label className="field">
      <span className="field-label">{label}</span>
      {children}
      {error && <span className="field-error">{error}</span>}
    </label>
  );
}

/** Fortschrittsbalken (value von max in Prozent), barrierefrei über role="progressbar". */
export function ProgressBar({ value, max, color }: { value: number; max: number; color?: string | null }) {
  const percent = max > 0 ? Math.round((value / max) * 100) : 0;
  return (
    <div
      className="progress"
      role="progressbar"
      aria-valuemin={0}
      aria-valuemax={100}
      aria-valuenow={percent}
    >
      <div className="progress-fill" style={{ width: `${percent}%`, background: color ?? 'var(--primary)' }} />
    </div>
  );
}
