import type { ReactNode } from 'react';

/**
 * Gemeinsamer Rahmen für alle Seiten ohne Login: Login, Registrierung, Passwort vergessen,
 * Passwort zurücksetzen und E-Mail bestätigen. Zeigt Logo, Überschrift, Untertitel und darunter den Inhalt.
 */
export function AuthCard({ title, subtitle, children }: { title: string; subtitle?: string; children: ReactNode }) {
  return (
    <div className="auth-screen">
      <div className="auth-card">
        <div className="brand auth-brand">
          <span className="brand-mark">SP</span>
          <span className="brand-name">Study Planner</span>
        </div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
        {children}
      </div>
    </div>
  );
}
